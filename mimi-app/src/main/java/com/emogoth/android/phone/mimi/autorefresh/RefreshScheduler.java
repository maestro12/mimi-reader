/*
 * Copyright (c) 2016. Eli Connelly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.emogoth.android.phone.mimi.autorefresh;


import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import androidx.preference.PreferenceManager;
import androidx.core.app.NotificationManagerCompat;
import android.util.Log;

import com.emogoth.android.phone.mimi.BuildConfig;
import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.db.HistoryTableConnection;
import com.emogoth.android.phone.mimi.db.model.History;
import com.emogoth.android.phone.mimi.model.ThreadInfo;
import com.emogoth.android.phone.mimi.model.ThreadRegistryModel;
import com.emogoth.android.phone.mimi.util.AppRater;
import com.emogoth.android.phone.mimi.util.MimiPrefs;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.emogoth.android.phone.mimi.util.ThreadRegistry;
import com.squareup.tape2.ObjectQueue;
import com.squareup.tape2.QueueFile;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.reactivex.disposables.Disposable;

public class RefreshScheduler {

    public static final String LOG_TAG = RefreshScheduler.class.getSimpleName();
    public static final boolean LOG_DEBUG = BuildConfig.DEBUG;

    private static final int JOB_ID = 8382;

    public static final String INTENT_FILTER = "com.emogoth.android.phone.mimi.AutoRefresh";

    public static final String REFRESH_QUEUE_FILE_NAME = "refresh.queue";
    public static final String REFRESH_QUEUE_PREF_FILE = "refresh_scheduler";
    public static final String REMOVED_THREADS_PREF = "removed_threads_pref";
    public static final String ADDED_THREADS_PREF = "added_threads_pref";

    public static final String POST_KEY = "post";
    public static final String DATA_KEY = "data";
    public static final String RESULT_KEY = "result";
    public static final String THREAD_INFO_KEY = "threadinfo";
    public static final String REFRESH_TIME_KEY = "timestamp";
    public static final String HACK_BUNDLE_KEY = "hack_bundle";
    public static final String BACKGROUNDED_KEY = "backgrounded";
    public static final String ERROR_CODE = "error_code";

    public static final String THREAD_ID_EXTRA = "thread_id";
    public static final String BOARD_NAME_EXTRA = "board_name";
    public static final String BOARD_TITLE_EXTRA = "board_title";
    public static final String WATCHED_EXTRA = "watched";
    public static final String LAST_REFRESH_TIME_EXTRA = "last_refresh_time";

    public static final Long MIN_TIME_BETWEEN_REFRESH = 0L;
    public static final Long MAX_TIME_BETWEEN_REGISTER = 5000L;

    public static final int RESULT_SUCCESS = 0;
    public static final int RESULT_ERROR = 1;
    public static final int RESULT_SCHEDULED = 2;

    public static final int REFRESH_TIMEOUT = 10000;

    private Sequencer sequencer;

    private ObjectQueue<ThreadInfo> refreshQueue;
    private int refreshInterval = 0;
    private int backgroundRefreshInterval = 0;
    private Context context;
    private JobScheduler refreshScheduler;
    private AutoRefreshListener callback;

    private boolean refreshActive = false;
    private boolean waitingForService = false;
    private boolean backgrounded = true;

    private final Object lock = new Object();

    private Handler registrationHandler = new Handler();
    private Runnable stopScheduler = () -> shutdown();

    private Runnable backgroundScheduler = () -> {
        backgrounded = true;
        refreshInterval = backgroundRefreshInterval;
//            scheduleNextRun();
    };

    private static RefreshScheduler instance;

    private Disposable fetchHistorySubscription;


    private RefreshScheduler() {
        if (LOG_DEBUG) {
            Log.d(LOG_TAG, "Creating refresh scheduler");
        }

        // hack to hopefully fix oom crashes
        SharedPreferences prefs = AppRater.getAppRaterPrefs(MimiApplication.getInstance());
        int lastVersion = prefs.getInt("currentversion", 0);
        if (lastVersion < 138) {
            try {
                Log.d(LOG_TAG, "Removing refresh queue file");
                File queueFile = new File(MimiApplication.getInstance().getFilesDir(), REFRESH_QUEUE_FILE_NAME);
                if (queueFile.exists()) {
                    Log.d(LOG_TAG, "Refresh queue file exists; deleting");
                    queueFile.delete();
                } else {
                    Log.d(LOG_TAG, "Refresh queue file does not exist");
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error while purging old queue file", e);
            }
        } else {
            Log.d(LOG_TAG, "Not removing refresh queue file");
        }

        try {
            File queueFileLocation = new File(MimiApplication.getInstance().getFilesDir(), REFRESH_QUEUE_FILE_NAME);
            QueueFile queueFile = new QueueFile.Builder(queueFileLocation).build();
            refreshQueue = ObjectQueue.create(queueFile, new RefreshQueueConverter());

            sequencer = new Sequencer();

            init(MimiApplication.getInstance());
        } catch (IOException e) {
            Log.w(LOG_TAG, "Could not create an instance of RefreshScheduler; retrying", e);

            try {
                File queueFileLocation = new File(MimiApplication.getInstance().getFilesDir(), REFRESH_QUEUE_FILE_NAME);
                if (queueFileLocation.exists()) {
                    queueFileLocation.delete();
                }

                QueueFile queueFile = new QueueFile.Builder(queueFileLocation).build();
                refreshQueue = ObjectQueue.create(queueFile, new RefreshQueueConverter());

                sequencer = new Sequencer();
                sequencer.setOnSequencerStartedCallback(this::loadBookmarkFiles);
            } catch (IOException error) {
                Log.e(LOG_TAG, "Could not create an instance of RefreshScheduler; exiting", e);
            }
        }
    }

    public static RefreshScheduler getInstance() {
        if (instance == null) {
            instance = new RefreshScheduler();
        }
        return instance;
    }

    public void init(final Context context) {
        if (LOG_DEBUG) {
            Log.d(LOG_TAG, "Initializing refresh scheduler");
        }

        this.context = context;

        cleanupPrefs();
        refreshInterval = MimiPrefs.refreshInterval(context);
        sequencer.start();
        refreshScheduler = (JobScheduler) this.context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    }

    private void cleanupPrefs() {
        try {
            SharedPreferences prefs = getSchedulerPrefs();
            Set<String> addedThreads = prefs.getStringSet(ADDED_THREADS_PREF, new HashSet<>());
            Set<String> removedThreads = prefs.getStringSet(REMOVED_THREADS_PREF, new HashSet<>());

            if (refreshQueue != null && refreshQueue.size() == 0) {
                boolean addedEmpty = true;
                boolean removedEmpty = true;
                if (addedThreads.size() > 0) {
                    addedEmpty = false;
                }

                if (removedThreads.size() > 0) {
                    removedEmpty = false;
                }

                if (!addedEmpty || !removedEmpty) {
                    SharedPreferences.Editor edits = prefs.edit();

                    if (!addedEmpty) {
                        edits.remove(ADDED_THREADS_PREF);
                    }

                    if (!removedEmpty) {
                        edits.remove(REMOVED_THREADS_PREF);
                    }

                    edits.apply();
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error cleaning up refreshed threads prefs", e);
        }
    }

    private void loadBookmarkFiles() {
        RxUtil.safeUnsubscribe(fetchHistorySubscription);
        fetchHistorySubscription = HistoryTableConnection.fetchHistory(true)
                .compose(DatabaseUtils.applySingleSchedulers())
                .onErrorReturn(throwable -> {
                    Log.w(LOG_TAG, "Error fetching history", throwable);
                    return Collections.emptyList();
                })
                .subscribe(historyList -> {
                    if (historyList.size() == 0) {
                        return;
                    }

                    resetQueuePrefs();

                    for (History bookmark : historyList) {
                        final ThreadInfo threadInfo = new ThreadInfo(bookmark.threadId, bookmark.boardName, 0, bookmark.watched == 1);
                        addThread(threadInfo.boardName, threadInfo.threadId, threadInfo.watched);
                    }

                    if (!refreshActive) {
                        scheduleNextRun();
                    }

                    if (LOG_DEBUG) {
                        Log.d(LOG_TAG, "Initializing refresh scheduler: thread size=" + historyList.size() + ", refresh interval=" + refreshInterval + ", backgrounded=" + backgrounded);
                    }
                }, throwable -> {
                    if (LOG_DEBUG) {
                        Log.w(LOG_TAG, "Error while processing history", throwable);
                    }
                });
    }

    public void addThread(String boardName, long threadId, boolean watched) {
        if (refreshQueue == null) {
            File queueFileLocation = new File(MimiApplication.getInstance().getFilesDir(), REFRESH_QUEUE_FILE_NAME);

            try {
                QueueFile queueFile = new QueueFile.Builder(queueFileLocation).build();
                refreshQueue = ObjectQueue.create(queueFile, new RefreshQueueConverter());
            } catch (IOException e) {
                Log.w(LOG_TAG, "Could not create refresh queue file");
                refreshQueue = null;
                return;
            }
        }

        if (sequencer.started) {
            sequencer.addThread(boardName, threadId, watched);
        } else {
            ThreadInfo threadInfo = new ThreadInfo(threadId, boardName, null, watched);
            addThreadSynchronized(threadInfo);
        }
    }

    private void addThreadSynchronized(final ThreadInfo threadInfo) {

        if (threadInfo == null) {
            return;
        }

        if (LOG_DEBUG) {
            Log.w(LOG_TAG, "Adding thread: id=/" + threadInfo.boardName + "/" + threadInfo.threadId);
        }

        SharedPreferences prefs = getSchedulerPrefs();
        Set<String> addedThreads = prefs.getStringSet(ADDED_THREADS_PREF, new HashSet<>());

        String id = String.valueOf(threadInfo.threadId);
        if (!addedThreads.contains(id)) {

            addedThreads.add(String.valueOf(threadInfo.threadId));
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(ADDED_THREADS_PREF).commit();
            editor.putStringSet(ADDED_THREADS_PREF, addedThreads).commit();

            try {
                refreshQueue.add(threadInfo);

                if (LOG_DEBUG) {
                    Log.d(LOG_TAG, "Adding thread to queue file: id=" + threadInfo.threadId);
                    for (String addedThread : addedThreads) {
                        Log.d(LOG_TAG, "Added thread: id=" + addedThread);
                    }
                }

                if (!refreshActive) {
                    if (LOG_DEBUG) {
                        Log.d(LOG_TAG, "[addThreadSynchronized] Setting refresh active to true");
                    }
                    refreshActive = true;
                    scheduleNextRun();
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Failed to add thread to refresh scheduler", e);
            }
        }

    }

    private SharedPreferences getSchedulerPrefs() {
        return MimiApplication.getInstance().getSharedPreferences(REFRESH_QUEUE_PREF_FILE, Context.MODE_PRIVATE);
    }

    public void removeThread(final String boardName, final long threadId) {
        if (LOG_DEBUG) {
            Log.e(LOG_TAG, "Removing thread: id=/" + boardName + "/" + threadId);
        }

        if (sequencer.started) {
            sequencer.removeThread(boardName, threadId);
        } else {
            removeThreadSynchronized(boardName, threadId);
        }
    }

    private void resetQueuePrefs() {
        SharedPreferences prefs = getSchedulerPrefs();
        prefs.edit().remove(ADDED_THREADS_PREF).remove(REMOVED_THREADS_PREF).apply();
    }

    protected void removeThreadSynchronized(final String boardName, final long threadId) {
        SharedPreferences prefs = getSchedulerPrefs();
        Set<String> removedThreads = prefs.getStringSet(REMOVED_THREADS_PREF, new HashSet<String>());
        removedThreads.add(String.valueOf(threadId));

        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(REMOVED_THREADS_PREF).commit();
        editor.putStringSet(REMOVED_THREADS_PREF, removedThreads).commit();

        if (LOG_DEBUG) {
            for (String removedThread : removedThreads) {
                Log.d(LOG_TAG, "Removed thread: id=" + removedThread);
            }
        }
    }

    public void setWaitingForService(boolean waiting) {
        waitingForService = waiting;
    }

    public void onRefreshStart(Bundle extras) {
        if (callback != null) {
            callback.onRefreshStart(extras);
        }
    }

    public void setInterval(final int timeout) {
        sequencer.updateInterval(timeout);
    }

    public void setIntervalSynchronized(final int interval) {
        refreshInterval = interval;
    }

    public void setAutoRefreshListener(final AutoRefreshListener listener) {
        callback = listener;
    }

    public void stop() {
        try {

            refreshScheduler.cancel(JOB_ID);

            refreshQueue.close();
            refreshQueue = null;

            if (LOG_DEBUG) {
                Log.d(LOG_TAG, "[stop] Setting refresh active to false");
            }
            refreshActive = false;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error stopping refresh scheduler", e);
        }
    }

    public void scheduleNextRun() {
        if (LOG_DEBUG) {
            Log.d(LOG_TAG, "[scheduleNextRun] Setting refresh active to true");
        }
        refreshActive = true;

        if (sequencer.started) {
            sequencer.scheduleNextRun();
        } else {
            scheduleNextRunSynchronized();
        }
    }

    private void scheduleNextRunSynchronized() {
        if (refreshQueue == null) {
            if (LOG_DEBUG) {
                Log.d(LOG_TAG, "refreshQueue is null; not scheduling next");
            }
            return;
        }

        final ThreadInfo nextPostToRefresh;
        try {
            nextPostToRefresh = refreshQueue.peek();
        } catch (Exception e) {
            Log.w(LOG_TAG, "Error peeking into queue", e);
            refreshActive = false;
            purgeQueueAndReload(true);
            return;
        }

        if (nextPostToRefresh == null) {
            Log.w(LOG_TAG, "Next post to refresh is null; exiting");

            try {
                if (refreshQueue == null || refreshQueue.size() == 0) {
                    stop();
                } else {
                    refreshQueue.remove();
                    scheduleNextRun();
                }
            } catch (Exception e) {
                Log.d(LOG_TAG, "Exception while checking refresh queue size", e);
                stop();
            }

            return;
        } else {
            ThreadRegistryModel t = ThreadRegistry.getInstance().getThread(nextPostToRefresh.threadId);
            if (t != null) {
                nextPostToRefresh.watched = t.isBookmarked();
            }
        }

        SharedPreferences prefs = getSchedulerPrefs();

        if (refreshQueue.size() > 0) {
            try {
                refreshQueue.remove();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Caught exception while removing queued item; exiting", e);
                return;
            }

            Set<String> removedThreads = prefs.getStringSet(REMOVED_THREADS_PREF, new HashSet<>());

            if (removedThreads.contains(String.valueOf(nextPostToRefresh.threadId)) || (backgrounded && !nextPostToRefresh.watched)) {
                try {
                    Set<String> addedThreads = prefs.getStringSet(ADDED_THREADS_PREF, new HashSet<>());
                    addedThreads.remove(String.valueOf(nextPostToRefresh.threadId));
                    removedThreads.remove(String.valueOf(nextPostToRefresh.threadId));

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove(REMOVED_THREADS_PREF).remove(ADDED_THREADS_PREF).commit();
                    editor.putStringSet(REMOVED_THREADS_PREF, removedThreads).putStringSet(ADDED_THREADS_PREF, addedThreads).commit();
                } catch (Exception e) {
                    Log.w(LOG_TAG, "Could not remove thread from removedThreads list", e);
                }
                scheduleNextRun();
                return;
            }

            long lastRefreshTime = nextPostToRefresh.refreshTimestamp;
            long nextRefreshTime = lastRefreshTime + (refreshInterval * 1000);

            final Long delta = nextRefreshTime - System.currentTimeMillis();
            if (delta < MIN_TIME_BETWEEN_REFRESH) {
                if (delta > 0) {
                    nextRefreshTime = nextRefreshTime + MIN_TIME_BETWEEN_REFRESH;
                } else {
                    nextRefreshTime = System.currentTimeMillis() + MIN_TIME_BETWEEN_REFRESH;
                }
            }

            nextPostToRefresh.setTimestamp(nextRefreshTime);

            if (LOG_DEBUG) {
                Log.d(LOG_TAG, "Scheduling next refresh cycle: active=" + refreshActive + ", thread=/" + nextPostToRefresh.boardName + "/" + nextPostToRefresh.threadId + ", time=" + (nextRefreshTime - System.currentTimeMillis()) + " ms" + ", backgrounded=" + backgrounded);
            }

            scheduleJob(nextPostToRefresh, nextRefreshTime);

            try {
                refreshQueue.add(nextPostToRefresh);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Caught exception while adding nenxt post to refresh; exiting", e);
                stop();
            }

            if (LOG_DEBUG) {
                Log.d(LOG_TAG, "Scheduled next refresh cycle: active=" + refreshActive + ", thread=/" + nextPostToRefresh.boardName + "/" + nextPostToRefresh.threadId + ", time=" + (nextRefreshTime - System.currentTimeMillis()) + " ms" + ", backgrounded=" + backgrounded);
            }

        } else {

            if (LOG_DEBUG) {
                Log.d(LOG_TAG, "Shutting down refresh service: no more threads to refresh");
            }
            stop();
        }
    }

    private void scheduleJob(ThreadInfo threadInfo, long nextRunTime) {
        long delay = nextRunTime - System.currentTimeMillis();
        PersistableBundle bundle = new PersistableBundle();
        bundle.putLong(THREAD_ID_EXTRA, threadInfo.threadId);
        bundle.putString(BOARD_NAME_EXTRA, threadInfo.boardName);
        bundle.putString(BOARD_TITLE_EXTRA, threadInfo.boardTitle);
        bundle.putInt(WATCHED_EXTRA, threadInfo.watched ? 1 : 0);
        bundle.putLong(LAST_REFRESH_TIME_EXTRA, threadInfo.refreshTimestamp);
        bundle.putInt(RESULT_KEY, RESULT_SCHEDULED);
        bundle.putInt(BACKGROUNDED_KEY, backgrounded ? 1 : 0);

        ComponentName serviceComponent = new ComponentName(context, RefreshJobService.class);
        JobInfo jobInfo = new JobInfo.Builder(JOB_ID, serviceComponent)
                .setMinimumLatency(delay)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setRequiresCharging(false)
                .setExtras(bundle)
                .build();
        refreshScheduler.schedule(jobInfo);
    }

    public void register(final Activity activity) {
        synchronized (lock) {
//            lock = activity;

            if (LOG_DEBUG) {
                Log.d(LOG_TAG, "Registering activity: name=" + activity.getClass().getSimpleName());
            }

            refreshInterval = MimiPrefs.refreshInterval(activity);
            registrationHandler.removeCallbacks(stopScheduler);
            registrationHandler.removeCallbacks(backgroundScheduler);

            if (backgrounded) {
                backgrounded = false;
                hideNotification(activity);

                try {
                    if (refreshInterval > 0) {
                        purgeQueueAndReload(false);
                    } else {
                        Log.d(LOG_TAG, "Not starting refresh; interval is 0)");
                    }
                } catch (Exception e) {
                    Log.w(LOG_TAG, "Error creating refresh queue file", e);
                }
            }
        }
    }

    private void hideNotification(Context context) {
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.cancel(RefreshJobService.NOTIFICATION_ID);
    }

    private void purgeQueueAndReload(boolean forceNewFile) {
        try {
            if (refreshInterval > 0) {
                if (refreshQueue == null || forceNewFile) {
                    if (refreshQueue != null) {
                        try {
                            refreshQueue.close();
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "Caught exception while closing refresh queue", e);
                        }
                    }
                    File queueFileLocation = new File(MimiApplication.getInstance().getFilesDir(), REFRESH_QUEUE_FILE_NAME);
                    if (queueFileLocation.exists()) {
                        queueFileLocation.delete();
                    }

                    QueueFile queueFile = new QueueFile.Builder(queueFileLocation).build();

                    refreshQueue = ObjectQueue.create(queueFile, new RefreshQueueConverter());
                    loadBookmarkFiles();
                }
            } else {
                Log.d(LOG_TAG, "Not starting refresh; interval is 0)");
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Could not create new queue file; exiting", e);
        }
    }

    public void unregister(final Activity activity) {
        synchronized (lock){
            if (LOG_DEBUG) {
                Log.d(LOG_TAG, "Unregistering activity: name=" + activity.getClass().getSimpleName());
            }

            backgroundRefreshInterval = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(activity).getString(activity.getString(R.string.background_auto_refresh_time), "120"));

            if (refreshInterval == 0) {
                registrationHandler.postDelayed(stopScheduler, MAX_TIME_BETWEEN_REGISTER);
            } else {
                registrationHandler.postDelayed(backgroundScheduler, MAX_TIME_BETWEEN_REGISTER);
            }
        }
    }

    private void shutdown() {
        if (LOG_DEBUG) {
            Log.d(LOG_TAG, "[shutdown] Setting refresh active to false");
        }
        refreshActive = false;
        backgrounded = true;

        if (refreshQueue != null) {
            if (LOG_DEBUG) {
                Log.d(LOG_TAG, "Clearing thread queue", new Exception());
            }
            try {
                refreshQueue.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Caught exception while trying to close queue", e);
            }
        }

        if (refreshInterval == 0) {
            refreshScheduler.cancel(JOB_ID);

            if (LOG_DEBUG) {
                Log.d(LOG_TAG, "Stopping scheduler");
            }
        }

        if (sequencer != null) {
            sequencer.shutdown();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        Log.d(LOG_TAG, "Refresh scheduler is being garbage collected");
        super.finalize();
    }

    public interface AutoRefreshListener {
        void onRefreshStart(Bundle b);

        void onRefreshComplete(Bundle b);
    }

    private static class RefreshMessageHandler extends Handler {
        final RefreshScheduler scheduler;
        public RefreshMessageHandler(RefreshScheduler scheduler) {
            this.scheduler = scheduler;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.arg1 == Sequencer.MSG_ADD_RAW) {
                if (msg.obj != null) {
                    scheduler.addThreadSynchronized((ThreadInfo) msg.obj);
                }
            } else if (msg.arg1 == Sequencer.MSG_ADD_OBJ) {
                if (msg.obj != null) {
                    scheduler.addThreadSynchronized((ThreadInfo) msg.obj);
                }
            } else if (msg.arg1 == Sequencer.MSG_REMOVE) {
                if (msg.obj != null) {
                    ThreadInfo ti = (ThreadInfo) msg.obj;
                    scheduler.removeThreadSynchronized(ti.boardName, ti.threadId);
                }
            } else if (msg.arg1 == Sequencer.MSG_UPDATE_INTERVAL) {
                scheduler.setIntervalSynchronized(msg.arg2);
            } else if (msg.arg1 == Sequencer.MSG_SCHEDULE_RUN) {
                scheduler.scheduleNextRunSynchronized();
            }
//            else if (msg.arg1 == Sequencer.MSG_QUIT) {
//                getLooper().quit();
//            }
        }
    }

    private class Sequencer extends Thread {
        static final int MSG_ADD_RAW = 0;
        static final int MSG_ADD_OBJ = 4;
        static final int MSG_REMOVE = 1;
        static final int MSG_UPDATE_INTERVAL = 2;
        static final int MSG_SCHEDULE_RUN = 3;
        static final int MSG_QUIT = 4;

        private Handler handler;
        private OnSequencerStartedCallback callback;

        boolean started = false;


        @Override
        public void run() {
            super.run();

            Looper.prepare();

            synchronized (this) {

                handler = new RefreshMessageHandler(RefreshScheduler.this);
                started = true;

                if (callback != null) {
                    callback.onStart();
                    callback = null;
                }
            }

            Looper.loop();

        }

        @Override
        public synchronized void start() {
            final long startTime = System.currentTimeMillis();

            if (LOG_DEBUG) {
                Log.i(LOG_TAG, "Starting sequencer thread: time=" + startTime);
            }

            super.start();

            if (LOG_DEBUG) {
                final long endTime = System.currentTimeMillis();
                Log.i(LOG_TAG, "Sequencer started: time=" + endTime + ", elapsed=" + (endTime - startTime));
            }
        }

        public void addThread(String boardName, long threadId, boolean watched) {
            ThreadInfo threadInfo = new ThreadInfo(threadId, boardName, null, watched);
            addThread(threadInfo);
        }

        public void addThread(final ThreadInfo threadInfo) {
            final Message msg = new Message();
            msg.arg1 = MSG_ADD_OBJ;
            msg.obj = threadInfo;

            handler.sendMessage(msg);
        }

        public void removeThread(final String boardName, final long threadId) {
            ThreadInfo threadInfo = new ThreadInfo(threadId, boardName, "", false);
            final Message msg = new Message();
            msg.arg1 = MSG_REMOVE;
            msg.obj = threadInfo;

            handler.sendMessage(msg);
        }

        public void updateInterval(final int interval) {
            final Message msg = new Message();
            msg.arg1 = MSG_REMOVE;
            msg.arg2 = interval;

            handler.sendMessage(msg);
        }

        public void scheduleNextRun() {
            final Message msg = new Message();
            msg.arg1 = MSG_SCHEDULE_RUN;

            handler.sendMessage(msg);
        }

        public void setOnSequencerStartedCallback(final OnSequencerStartedCallback callback) {
            this.callback = callback;
        }

        public void shutdown() {
            final Message msg = new Message();
            msg.arg1 = MSG_QUIT;

            handler.sendMessage(msg);
        }

    }

    protected interface OnSequencerStartedCallback {
        void onStart();
    }
}
