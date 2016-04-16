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

package com.emogoth.android.phone.mimi.util;


import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.db.HistoryTableConnection;
import com.emogoth.android.phone.mimi.db.model.History;
import com.emogoth.android.phone.mimi.model.ThreadInfo;
import com.emogoth.android.phone.mimi.service.AutoRefreshService;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import rx.Subscription;
import rx.functions.Action1;

public class RefreshScheduler extends BroadcastReceiver {

    public static final String LOG_TAG = RefreshScheduler.class.getSimpleName();
    public static final boolean LOG_DEBUG = false;

    public static final String INTENT_FILTER = "com.emogoth.android.phone.mimi.AutoRefresh";

    public static final String POST_KEY = "post";
    public static final String DATA_KEY = "data";
    public static final String RESULT_KEY = "result";
    public static final String THREAD_INFO_KEY = "threadinfo";
    public static final String REFRESH_TIME_KEY = "timestamp";
    public static final String HACK_BUNDLE_KEY = "hack_bundle";
    public static final String BACKGROUDNED_KEY = "backgrounded";
    public static final String ERROR_CODE = "error_code";

    public static final Long MIN_TIME_BETWEEN_REFRESH = 1500L;
    public static final Long MAX_TIME_BETWEEN_REGISTER = 5000L;

    public static final int RESULT_SUCCESS = 0;
    public static final int RESULT_ERROR = 1;
    public static final int RESULT_SCHEDULED = 2;

    public static final int REFRESH_TIMEOUT = 10000;

    private Sequencer sequencer;

    private LinkedList<ThreadInfo> threadInfoQueue;
    //    private Hashtable<ThreadInfo, Long> mThreads;
    private int refreshInterval = 0;
    private int backgroundRefreshInterval = 0;
    private Context context;
    private AlarmManager refreshScheduler;
    private AutoRefreshListener callback;

    private PendingIntent lastPendingIntent;

    private boolean refreshActive = false;
    private boolean waitingForService = false;
    private boolean backgrounded = false;

    private Object lock;

    private List<Integer> timeoutList = new ArrayList<>();

    private Handler handler = new Handler();
    private Runnable stopScheduler = new Runnable() {
        @Override
        public void run() {
            shutdown();
        }
    };

    private Runnable backgroundScheduler = new Runnable() {
        @Override
        public void run() {
            backgrounded = true;
            refreshInterval = backgroundRefreshInterval;
//            scheduleNextRun();
        }
    };

    private Handler timeoutHandler = new Handler();
    private Runnable timeoutRunnable;

    private static RefreshScheduler instance = new RefreshScheduler();

    private Subscription fetchHistorySubscription;


    public RefreshScheduler() {
        threadInfoQueue = new LinkedList<>();

        sequencer = new Sequencer();
        sequencer.setOnSequencerStartedCallback(new OnSequencerStartedCallback() {
            @Override
            public void onStart() {
                loadBookmarkFiles();
            }
        });
    }

    public static RefreshScheduler getInstance() {
        return instance;
    }

    public static void start(final Context context) {
        final Intent serviceIntent = new Intent(context, AutoRefreshService.class);
        context.startService(serviceIntent);
    }

    public void init(final Context context) {
        if (this.context == null) {
            this.context = context;
            backgrounded = false;

            refreshInterval = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.app_auto_refresh_time), "10"));

            sequencer.start();

            refreshScheduler = (AlarmManager) this.context.getSystemService(Context.ALARM_SERVICE);
            context.registerReceiver(this, new IntentFilter(INTENT_FILTER));
        }
    }

    private void loadBookmarkFiles() {
//        if (lock == null) {
//            backgrounded = true;
//        } else {
//            backgrounded = false;
//        }

        RxUtil.safeUnsubscribe(fetchHistorySubscription);
        fetchHistorySubscription = HistoryTableConnection.fetchHistory(true)
                .compose(DatabaseUtils.<List<History>>applySchedulers())
                .subscribe(new Action1<List<History>>() {
                    @Override
                    public void call(List<History> historyList) {
                        for (History bookmark : historyList) {
                            final ThreadInfo threadInfo = new ThreadInfo(bookmark.threadId, bookmark.boardName, 0, bookmark.watched);
                            sequencer.addThread(threadInfo);
                        }

                        if (LOG_DEBUG) {
                            Log.d(LOG_TAG, "Initializing refresh scheduler: thread size=" + historyList.size() + ", refresh interval=" + refreshInterval + ", backgrounded=" + backgrounded);
                        }
                    }
                });
    }

    public void addThread(String boardName, int threadId, boolean watched) {
        sequencer.addThread(boardName, threadId, watched);
    }

    protected void addThreadSynchronized(final ThreadInfo threadInfo) {
        if (!threadInfoQueue.contains(threadInfo)) {
            if (LOG_DEBUG) {
                Log.d(LOG_TAG, "Adding thread: id=/" + threadInfo.boardName + "/" + threadInfo.threadId);
            }

            threadInfoQueue.add(threadInfo);

            if (!refreshActive) {
                refreshActive = true;
                scheduleNextRun();
            }
        } else if (LOG_DEBUG) {
            Log.d(LOG_TAG, "Not adding thread: id=/" + threadInfo.boardName + "/" + threadInfo.threadId);
        }
    }


    public void removeThread(final String boardName, final int threadId) {
        if (LOG_DEBUG) {
            Log.e(LOG_TAG, "Removing thread: id=/" + boardName + "/" + threadId);
        }

        sequencer.removeThread(boardName, threadId);
    }

    protected void removeThreadSynchronized(final String boardName, final int threadId) {

        final ThreadInfo threadInfo = new ThreadInfo(threadId, boardName, null, false);
        boolean removed = true;
        threadInfoQueue.remove(threadInfo);

        for (ThreadInfo info : threadInfoQueue) {
            if (info.threadId == threadId) {
                removed = false;
            }
        }

        if (removed) {
            if (LOG_DEBUG) {
                Log.d(LOG_TAG, "Removed thread: id=/" + boardName + "/" + threadId);
            } else {
                Log.d(LOG_TAG, "Could not remove thread: id=/" + boardName + "/" + threadId);
            }
        }

        if (threadInfoQueue.size() == 0) {
            refreshActive = false;
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
        refreshInterval = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString("refreshTimeout", "10"));

        if (refreshInterval > 0) {
            context.registerReceiver(this, new IntentFilter(INTENT_FILTER));
            //scheduleNextRun();
        }
    }

    public void stop() {
        try {
            refreshInterval = 0;
            context.unregisterReceiver(this);

            if (lastPendingIntent != null) {
                refreshScheduler.cancel(lastPendingIntent);
                final Intent threadData = new Intent(this.context, AutoRefreshService.class);
                this.context.stopService(threadData);
            }

            refreshActive = false;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error: " + e.getLocalizedMessage());
        }
    }

    public void forceRefresh() {

    }

    public void scheduleNextRun() {
        refreshActive = true;
        sequencer.scheduleNextRun();
    }

    public void scheduleNextRunSynchronized() {
        if (threadInfoQueue == null) {
            return;
        }

        if (threadInfoQueue.size() > 0) {

            ThreadInfo nextPostToRefresh = threadInfoQueue.remove();
            Integer id = nextPostToRefresh.threadId;
            threadInfoQueue.addLast(nextPostToRefresh);
            timeoutList.remove(id);
            Long lastRefreshTime = nextPostToRefresh.refreshTimestamp;
            Long nextRefreshTime;

            nextRefreshTime = lastRefreshTime + (refreshInterval * 1000);

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

            final Intent broadcastIntent = new Intent(INTENT_FILTER);
            broadcastIntent.putExtra(RESULT_KEY, RESULT_SCHEDULED);
            broadcastIntent.putExtra(BACKGROUDNED_KEY, backgrounded);
            broadcastIntent.setExtrasClassLoader(ThreadInfo.class.getClassLoader());

            final Bundle hackBundle = new Bundle();
            hackBundle.putParcelable(THREAD_INFO_KEY, nextPostToRefresh);
            broadcastIntent.putExtra(HACK_BUNDLE_KEY, hackBundle);

            final PendingIntent intent = PendingIntent.getBroadcast(context, 0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                refreshScheduler.setExact(AlarmManager.RTC_WAKEUP, nextRefreshTime, intent);
            } else {
                refreshScheduler.set(AlarmManager.RTC_WAKEUP, nextRefreshTime, intent);
            }

            lastPendingIntent = intent;

            if (LOG_DEBUG) {
                Log.d(LOG_TAG, "Scheduled next refresh cycle: active=" + refreshActive + ", thread=/" + nextPostToRefresh.boardName + "/" + nextPostToRefresh.threadId + ", time=" + (nextRefreshTime - System.currentTimeMillis()) + " ms" + ", backgrounded=" + backgrounded);
            }

        } else {

            if (LOG_DEBUG) {
                Log.d(LOG_TAG, "Shutting down refresh service: no more threads to refresh");
            }

            stop();

            loadBookmarkFiles();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LOG_TAG, "AlarmManager fired!");
        int result = -1;
        final Bundle extras = intent.getExtras();
        if (extras != null) {

            if (extras.containsKey(RESULT_KEY)) {
                result = extras.getInt(RESULT_KEY);
            }

            ThreadInfo threadInfo = null;
            if (extras.containsKey(HACK_BUNDLE_KEY)) {
                Bundle hackBundle = extras.getBundle(HACK_BUNDLE_KEY);

                if (hackBundle != null && hackBundle.containsKey(THREAD_INFO_KEY)) {
                    threadInfo = hackBundle.getParcelable(THREAD_INFO_KEY);
                }
            }

            switch (result) {
                case RESULT_SCHEDULED:
                    if (callback != null) {
                        callback.onRefreshStart(extras);
                    }

                    final Intent threadData = new Intent(this.context, AutoRefreshService.class);
                    threadData.putExtras(extras);

                    waitingForService = true;
                    this.context.startService(threadData);
                    break;
                case RESULT_SUCCESS:

                    waitingForService = false;

                    if (LOG_DEBUG) {
                        if (threadInfo != null) {
                            Log.d(LOG_TAG, "Returned from refresh service successfully: id=/" + threadInfo.boardName + "/" + threadInfo.threadId + ", size=" + ThreadRegistry.getInstance().getThreadSize(threadInfo.threadId) + ", unread=" + ThreadRegistry.getInstance().getUnreadCount(threadInfo.threadId));
                        } else {
                            Log.d(LOG_TAG, "Returned from refresh service successfully: id=UNKNOWN");
                        }
                    }

                    if (threadInfo != null) {
                        Integer id = threadInfo.threadId;
                        if (!timeoutList.remove(id)) {
                            timeoutHandler.removeCallbacks(timeoutRunnable);
                            scheduleNextRun();
                        }
                    }

                    break;
                case RESULT_ERROR:
                    waitingForService = false;

                    if (extras.containsKey(ERROR_CODE)) {
                        final int code = extras.getInt(ERROR_CODE);

                        if (code == 404 && threadInfo != null) {
                            removeThread(threadInfo.boardName, threadInfo.threadId);
                        }
                    }

                    if (LOG_DEBUG) {
                        if (threadInfo != null) {
                            Log.e(LOG_TAG, "Error while auto refreshing thread: id=/" + threadInfo.boardName + "/" + threadInfo.threadId);
                        } else {
                            Log.e(LOG_TAG, "Error while auto refreshing thread: id=UNKNOWN");
                        }
                    }

                    if (threadInfo != null) {
                        Integer id = threadInfo.threadId;
                        if (!timeoutList.remove(id)) {
                            timeoutHandler.removeCallbacks(timeoutRunnable);
                            scheduleNextRun();
                        }
                    }

                    break;

                default:
                    waitingForService = false;
                    Log.w(LOG_TAG, "Did not recognize result: " + result);
                    scheduleNextRun();
                    break;

            }

        } else {
            Log.e(LOG_TAG, "Extras bundle from refresh scheduler is null: aborting");
        }

    }

    public void register(final Activity activity) {
        if (activity != lock) {
            lock = activity;

            if (LOG_DEBUG) {
                Log.d(LOG_TAG, "Registering activity: name=" + activity.getClass().getSimpleName());
            }

            final int oldRefreshInterval = refreshInterval;
            refreshInterval = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(activity).getString(activity.getString(R.string.app_auto_refresh_time), "10"));
            handler.removeCallbacks(stopScheduler);
            handler.removeCallbacks(backgroundScheduler);

            if (backgrounded) {
                backgrounded = false;

//                if (lastPendingIntent != null) {
//                    refreshScheduler.cancel(lastPendingIntent);
//                    final Intent threadData = new Intent(context, AutoRefreshService.class);
//                    context.stopService(threadData);
//                }

                if (refreshInterval > 0) {
                    if (threadInfoQueue == null || threadInfoQueue.size() == 0) {

                        if (LOG_DEBUG) {
                            Log.d(LOG_TAG, "Thread queue is empty, loading bookmarks from disk");
                        }

                        loadBookmarkFiles();
                    } else {
                        for (int i = 0; i < threadInfoQueue.size(); i++) {
                            threadInfoQueue.get(i).setTimestamp(System.currentTimeMillis());
                        }

                        if (LOG_DEBUG) {
                            Log.d(LOG_TAG, "Scheduling next run");
                        }

                        scheduleNextRun();
                    }
                } else {
                    Log.d(LOG_TAG, "Not starting refresh; interval is 0)");
                }
            }
        }
    }

    public void unregister(final Activity activity) {
        if (activity == lock) {
            lock = null;

            if (LOG_DEBUG) {
                Log.d(LOG_TAG, "Unregistering activity: name=" + activity.getClass().getSimpleName());
            }

            backgroundRefreshInterval = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(activity).getString(activity.getString(R.string.background_auto_refresh_time), "120"));

            if (refreshInterval == 0) {
                handler.postDelayed(stopScheduler, MAX_TIME_BETWEEN_REGISTER);
            } else {
                handler.postDelayed(backgroundScheduler, MAX_TIME_BETWEEN_REGISTER);
            }
        }
    }

    public void shutdown() {
        refreshActive = false;
        backgrounded = true;

        if (threadInfoQueue != null) {
            if (LOG_DEBUG) {
                Log.d(LOG_TAG, "Clearing thread queue");
            }
            threadInfoQueue.clear();
        }

        if (lastPendingIntent != null && refreshInterval == 0) {
            refreshScheduler.cancel(lastPendingIntent);
            final Intent threadData = new Intent(context, AutoRefreshService.class);
            context.stopService(threadData);

            if (LOG_DEBUG) {
                Log.d(LOG_TAG, "Stopping scheduler");
            }
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

    private class Sequencer extends Thread {
        protected int MSG_ADD_RAW = 0;
        protected int MSG_ADD_OBJ = 4;
        protected int MSG_REMOVE = 1;
        protected int MSG_UPDATE_INTERVAL = 2;
        protected int MSG_SCHEDULE_RUN = 3;

        private Handler handler;
        private OnSequencerStartedCallback callback;


        @Override
        public void run() {
            super.run();

            Looper.prepare();

            synchronized (this) {

                handler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);

                        if (msg.arg1 == MSG_ADD_RAW) {
                            addThreadSynchronized((ThreadInfo) msg.obj);
                        } else if (msg.arg1 == MSG_ADD_OBJ) {
                            addThreadSynchronized((ThreadInfo) msg.obj);
                        } else if (msg.arg1 == MSG_REMOVE) {
                            removeThreadSynchronized((String) msg.obj, msg.arg2);
                        } else if (msg.arg1 == MSG_UPDATE_INTERVAL) {
                            setIntervalSynchronized(msg.arg2);
                        } else if (msg.arg1 == MSG_SCHEDULE_RUN) {
                            scheduleNextRunSynchronized();
                        }
                    }
                };

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

        public void addThread(String boardName, int threadId, boolean watched) {
            ThreadInfo threadInfo = new ThreadInfo(threadId, boardName, null, watched);
            addThread(threadInfo);
        }

        public void addThread(final ThreadInfo threadInfo) {
            final Message msg = new Message();
            msg.arg1 = MSG_ADD_OBJ;
            msg.obj = threadInfo;

            handler.sendMessage(msg);
        }

        public void removeThread(final String boardName, final int threadId) {
            final Message msg = new Message();
            msg.arg1 = MSG_REMOVE;
            msg.arg2 = threadId;
            msg.obj = boardName;

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

    }

    protected interface OnSequencerStartedCallback {
        void onStart();
    }
}
