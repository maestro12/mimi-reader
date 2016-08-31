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

package com.emogoth.android.phone.mimi.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.StartupActivity;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.async.ProcessThreadTask;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.db.HistoryTableConnection;
import com.emogoth.android.phone.mimi.db.model.History;
import com.emogoth.android.phone.mimi.event.HttpErrorEvent;
import com.emogoth.android.phone.mimi.event.UpdateHistoryEvent;
import com.emogoth.android.phone.mimi.fourchan.FourChanConnector;
import com.emogoth.android.phone.mimi.model.ThreadInfo;
import com.emogoth.android.phone.mimi.model.ThreadRegistryModel;
import com.emogoth.android.phone.mimi.util.BusProvider;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.HttpClientFactory;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.Pages;
import com.emogoth.android.phone.mimi.util.RefreshScheduler;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.emogoth.android.phone.mimi.util.ThreadRegistry;
import com.mimireader.chanlib.ChanConnector;
import com.mimireader.chanlib.models.ChanPost;
import com.mimireader.chanlib.models.ChanThread;

import java.io.File;
import java.util.List;

import retrofit2.adapter.rxjava.HttpException;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

public class AutoRefreshService extends IntentService {
    private static final String LOG_TAG = AutoRefreshService.class.getSimpleName();
    private static final boolean LOG_DEBUG = false;

    private static final String NOTIFICATION_GROUP = "mimi_autorefresh";
    private static final int NOTIFICATION_ID = 1013;

    private Subscription fetchThreadSubscription;

    public AutoRefreshService() {
        super("AutoRefreshService");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        if (intent != null && intent.getExtras() != null) {
            final Bundle extras = intent.getExtras();
            final Bundle hackBundle = extras.getBundle(RefreshScheduler.HACK_BUNDLE_KEY);
            final ThreadInfo threadInfo = hackBundle.getParcelable(RefreshScheduler.THREAD_INFO_KEY);
            final boolean backgrounded = extras.getBoolean(RefreshScheduler.BACKGROUDNED_KEY);
            final String protocol = MimiUtil.httpOrHttps(this);
            final String server = getString(R.string.api_link);
            final String url = protocol + server + getString(R.string.api_thread_path, threadInfo.boardName, threadInfo.threadId);

            ChanConnector chanConnector = new FourChanConnector.Builder()
                    .setEndpoint(FourChanConnector.getDefaultEndpoint(MimiUtil.isSecureConnection(this)))
                    .setPostEndpoint(FourChanConnector.getDefaultPostEndpoint())
                    .setCacheDirectory(MimiUtil.getInstance().getCacheDir())
                    .setClient(HttpClientFactory.getInstance().getOkHttpClient())
                    .build();

            RxUtil.safeUnsubscribe(fetchThreadSubscription);
            fetchThreadSubscription = chanConnector.fetchThread(this, threadInfo.boardName, threadInfo.threadId, ChanConnector.CACHE_FORCE_NETWORK)
                    .zipWith(HistoryTableConnection.fetchPost(threadInfo.boardName, threadInfo.threadId), new Func2<ChanThread, History, ChanThread>() {
                        @Override
                        public ChanThread call(ChanThread chanThread, History history) {
                            if (chanThread == null || chanThread.getPosts() == null || chanThread.getPosts().size() == 0) {
                                return null;
                            }

                            threadInfo.watched = history.watched;
                            return chanThread;
                        }
                    })
                    .compose(DatabaseUtils.<ChanThread>applySchedulers())
                    .onErrorReturn(refreshError(threadInfo))
                    .subscribe(fetchComplete(threadInfo, intent, backgrounded), fetchFailed(threadInfo, intent));

            if (LOG_DEBUG) {
                Log.d(LOG_TAG, "Starting request for " + url + ": backgrounded=" + backgrounded + ", timestamp=" + threadInfo.refreshTimestamp);
            }
        }

    }

    private Action1<ChanThread> fetchComplete(@NonNull final ThreadInfo threadInfo, @Nullable final Intent intent, final boolean inBackground) {
        return new Action1<ChanThread>() {
            @Override
            public void call(ChanThread response) {
                if (response == null) {
                    return;
                }

                if (LOG_DEBUG) {
                    Log.d(LOG_TAG, "Got response from request");
                }

                final String name = threadInfo.boardName;
                final int id = threadInfo.threadId;
                final int threadSize = ThreadRegistry.getInstance().getThreadSize(id);

                response.setBoardName(name);
                response.setThreadId(id);

                final boolean bookmarked = threadInfo.watched;

                try {
                    if (bookmarked && threadSize < response.getPosts().size()) {
                        final File bookmarkFile = MimiUtil.getBookmarkFile(MimiUtil.getInstance().getBookmarkDir(), name, id);

                        if (bookmarkFile != null) {
                            MimiUtil.getInstance().saveBookmark(response, new MimiUtil.OperationCompleteListener() {
                                @Override
                                public void onOperationComplete() {
                                    if (LOG_DEBUG) {
                                        Log.d(LOG_TAG, "Saved bookmark successfully: thread=" + id);
                                    }

                                    if (inBackground) {
                                        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AutoRefreshService.this);
                                        final String notificationPref = preferences.getString(getString(R.string.background_notification_pref), "3");
                                        final int notificationLevel = Integer.valueOf(notificationPref);

                                        createNotification(name, id, notificationLevel);
                                    }
                                }

                                @Override
                                public void onOperationFailed() {
                                    if (LOG_DEBUG) {
                                        Log.e(LOG_TAG, "Error saving bookmark: thread=" + id);
                                    }
                                }
                            });

                            if (LOG_DEBUG) {
                                Log.i(LOG_TAG, "Saving bookmark for thread: thread=" + id);
                            }
                        }

                        if (LOG_DEBUG) {
                            Log.i(LOG_TAG, "Setting thread size to: size=" + response.getPosts().size() + ", old size=" + threadSize);
                        }

                        if (ThreadRegistry.getInstance().getThreadSize(id) <= 0) {
                            ThreadRegistry.getInstance().setThreadSize(id, response.getPosts().size());
                        }

                        ThreadRegistry.getInstance().update(id, response.getPosts().size(), true);
                    }
                } catch (final Exception e) {
                    Log.e(LOG_TAG, "Exception while saving bookmark", e);
                }

                final Bundle dataBundle = new Bundle();
                final Bundle hackBundle = new Bundle();
                dataBundle.putInt(RefreshScheduler.RESULT_KEY, RefreshScheduler.RESULT_SUCCESS);

                hackBundle.putParcelable(RefreshScheduler.THREAD_INFO_KEY, threadInfo);
                dataBundle.putBundle(RefreshScheduler.HACK_BUNDLE_KEY, hackBundle);

                final Intent result = new Intent(RefreshScheduler.INTENT_FILTER);
                result.putExtras(dataBundle);
                sendBroadcast(result);

                final UpdateHistoryEvent refreshEvent = new UpdateHistoryEvent();
                refreshEvent.setBoardName(name);
                refreshEvent.setThread(response);

                if (Looper.myLooper() == Looper.getMainLooper()) {
                    BusProvider.getInstance().post(refreshEvent);
                } else {
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            BusProvider.getInstance().post(refreshEvent);
                        }
                    });
                }
//
//                if (intent != null) {
//                    RefreshBroadcastReceiver.completeWakefulIntent(intent);
//                }
            }
        };
    }

    private Action1<Throwable> fetchFailed(@NonNull final ThreadInfo info, @Nullable final Intent intent) {
        return new Action1<Throwable>() {
            @Override
            public void call(Throwable error) {
                doFailure(info, error);

//                if (intent != null) {
//                    RefreshBroadcastReceiver.completeWakefulIntent(intent);
//                }
            }
        };
    }

    private Func1<Throwable, ChanThread> refreshError(@NonNull final ThreadInfo info) {
        return new Func1<Throwable, ChanThread>() {
            @Override
            public ChanThread call(Throwable throwable) {
                doFailure(info, throwable);
                return null;
            }
        };
    }

    private void doFailure(@NonNull ThreadInfo info, @NonNull Throwable error) {
        final Bundle dataBundle = new Bundle();
        final Bundle hackBundle = new Bundle();
        dataBundle.putInt(RefreshScheduler.RESULT_KEY, RefreshScheduler.RESULT_ERROR);

        hackBundle.putParcelable(RefreshScheduler.THREAD_INFO_KEY, info);
        dataBundle.putBundle(RefreshScheduler.HACK_BUNDLE_KEY, hackBundle);

        if (error instanceof HttpException) {
            HttpException exception = (HttpException) error;

            dataBundle.putInt(RefreshScheduler.ERROR_CODE, exception.code());
            if (exception.code() == 404) {
                if (LOG_DEBUG) {
                    Log.e(LOG_TAG, "Caught 404 error while refreshing " + info.boardName + "/" + info.threadId);
                }
                HistoryTableConnection.setHistoryRemovedStatus(info.boardName, info.threadId, true).subscribe();
            }
        }

        ThreadRegistry.getInstance().deactivate(info.threadId);

        final Intent result = new Intent(RefreshScheduler.INTENT_FILTER);
        result.putExtras(dataBundle);
        sendBroadcast(result);

        BusProvider.getInstance().post(new HttpErrorEvent(error, info));
    }

    private void createNotification(final String boardName, final int threadId, final int notificationLevel) {

        if (notificationLevel <= 1) {
            return;
        }

        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(MimiApplication.getInstance());

        final Intent openActivityIntent = new Intent(this, StartupActivity.class);
        final Bundle args = new Bundle();

        args.putString(Extras.OPEN_PAGE, Pages.BOOKMARKS.name());
        openActivityIntent.putExtras(args);

        final PendingIntent bookmarksPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        openActivityIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        List<ThreadRegistryModel> models = ThreadRegistry.getInstance().getUpdatedThreads();
        final int unreadCount = ThreadRegistry.getInstance().getUnreadCount();
        if (models != null && models.size() > 0) {

            final String unreadPosts = getResources().getQuantityString(R.plurals.unread_plural, unreadCount, unreadCount);

            notificationBuilder
                    .setGroup(NOTIFICATION_GROUP)
                    .setSmallIcon(R.drawable.ic_stat_leaf_icon)
                    .setAutoCancel(true)
                    .setContentTitle(getString(R.string.you_have_unread_posts_exclamation))
                    .setContentText(unreadPosts)
                    .setContentIntent(bookmarksPendingIntent);

            final NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
            int userPosts = 0;
            for (ThreadRegistryModel model : models) {
                if (model != null && model.getUnreadCount() > 0) {
                    final String hasUnread = getResources().getQuantityString(R.plurals.has_unread_plural, model.getUnreadCount(), model.getUnreadCount());
                    final String notificationTitle = "/" + model.getBoardName() + "/" + model.getThreadId() + " " + hasUnread;
                    style.addLine(notificationTitle);

                    if (model.getUserPosts() != null && model.getUserPosts().size() > 0) {
                        final ChanThread thread = MimiUtil.getInstance().getBookmarkedThread(model.getBoardName(), model.getThreadId());
                        final ProcessThreadTask task = new ProcessThreadTask(this, model.getBoardName(), thread);
                        final ChanThread t = task.loadInBackground();
                        final int pos = ThreadRegistry.getInstance().getLastReadPosition(model.getThreadId()) < ThreadRegistry.getInstance().getThreadSize(model.getThreadId()) ?
                                ThreadRegistry.getInstance().getLastReadPosition(model.getThreadId()) :
                                ThreadRegistry.getInstance().getThreadSize(model.getThreadId());

                        if (t != null && t.getPosts() != null) {
                            for (int i = pos; i < t.getPosts().size(); i++) {
                                final ChanPost post = t.getPosts().get(i);
                                if (LOG_DEBUG) {
                                    Log.d(LOG_TAG, "post id=" + post.getNo());
                                }
                                if (post.getRepliesTo() != null && post.getRepliesTo().size() > 0) {
                                    for (Integer integer : model.getUserPosts()) {
                                        final String s = String.valueOf(integer);

                                        if (LOG_DEBUG) {
                                            Log.d(LOG_TAG, "Checking post id " + s);
                                        }

                                        if (post.getRepliesTo().indexOf(s) >= 0) {
                                            if (LOG_DEBUG) {
                                                Log.d(LOG_TAG, "Found reply to " + s);
                                            }
                                            userPosts++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            final String threadsUpdated = getResources().getQuantityString(R.plurals.threads_updated_plural, models.size(), models.size());
            final String repliesToYou = getResources().getQuantityString(R.plurals.replies_to_you_plurals, userPosts, userPosts);
            style.setBigContentTitle(threadsUpdated);
            style.setSummaryText(repliesToYou);

            notificationBuilder.setStyle(style);

//            final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MimiApplication.getInstance());
            final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                final Notification notification = notificationBuilder.build();
                notificationManager.notify(NOTIFICATION_ID, notification);

                if (LOG_DEBUG) {
                    Log.d(LOG_TAG, "Creating notification for /" + boardName + "/" + threadId + ": id=" + NOTIFICATION_ID);
                }
            }
        }

    }
}
