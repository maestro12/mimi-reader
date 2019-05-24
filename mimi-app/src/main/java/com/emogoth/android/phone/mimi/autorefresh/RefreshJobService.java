package com.emogoth.android.phone.mimi.autorefresh;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.util.Pair;

import com.emogoth.android.phone.mimi.BuildConfig;
import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.StartupActivity;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.async.ProcessThreadTask;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.db.HistoryTableConnection;
import com.emogoth.android.phone.mimi.db.PostTableConnection;
import com.emogoth.android.phone.mimi.db.UserPostTableConnection;
import com.emogoth.android.phone.mimi.db.model.UserPost;
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
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.emogoth.android.phone.mimi.util.ThreadRegistry;
import com.mimireader.chanlib.ChanConnector;
import com.mimireader.chanlib.models.ChanPost;
import com.mimireader.chanlib.models.ChanThread;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import retrofit2.HttpException;

public class RefreshJobService extends JobService {
    private static final String LOG_TAG = RefreshJobService.class.getSimpleName();
    private static final boolean LOG_DEBUG = BuildConfig.DEBUG;

    public static final String NOTIFICATION_GROUP = "mimi_autorefresh";
    public static final int NOTIFICATION_ID = 1013;
    public static final String REFRESH_CHANNEL_ID = "mimi_autorefresh_channel";

    public static final int NOTIFICATIONS_NONE = 1;
    public static final int NOTIFICATIONS_ONLY_ME = 2;
    public static final int NOTIFICATIONS_ALL = 3;

    public static final String NOTIFICATIONS_KEY_THREAD_SIZE = "mimi_thread_size";

    private Disposable fetchThreadSubscription;
    private int userPosts;
    private Disposable fetchPostDisposable;

    @Override
    public boolean onStartJob(final JobParameters params) {
        final PersistableBundle extras = params.getExtras();

        if (LOG_DEBUG) {
            Log.d(LOG_TAG, "Running onStartJob");
        }

        final boolean backgrounded = extras.getInt(RefreshScheduler.BACKGROUNDED_KEY, 0) == 1;
        final boolean watched = extras.getInt(RefreshScheduler.WATCHED_EXTRA, 0) == 1;
        final long threadId = extras.getLong(RefreshScheduler.THREAD_ID_EXTRA, 0);
        final String boardName = extras.getString(RefreshScheduler.BOARD_NAME_EXTRA, "");
        final String boardTitle = extras.getString(RefreshScheduler.BOARD_TITLE_EXTRA, "");
        final long lastRefreshTime = extras.getLong(RefreshScheduler.LAST_REFRESH_TIME_EXTRA, 0);
        final ThreadInfo threadInfo = new ThreadInfo(threadId, boardName, lastRefreshTime, watched);
        final String protocol = MimiUtil.https();
        final String server = getString(R.string.api_link);
        final String url = protocol + server + getString(R.string.api_thread_path, threadInfo.boardName, threadInfo.threadId);

        ChanConnector chanConnector = new FourChanConnector.Builder()
                .setEndpoint(FourChanConnector.getDefaultEndpoint())
                .setPostEndpoint(FourChanConnector.getDefaultPostEndpoint())
                .setCacheDirectory(MimiUtil.getInstance().getCacheDir())
                .setClient(HttpClientFactory.getInstance().getClient())
                .build();

        RxUtil.safeUnsubscribe(fetchThreadSubscription);
        fetchThreadSubscription = Flowable.zip(chanConnector.fetchThread(threadInfo.boardName, threadInfo.threadId, ChanConnector.CACHE_FORCE_NETWORK),
                HistoryTableConnection.fetchPost(threadInfo.boardName, threadInfo.threadId), (chanThread, history) -> {
                    if (chanThread == null || chanThread.getPosts() == null || chanThread.getPosts().size() == 0) {
                        return new ThreadMetadata();
                    }

                    threadInfo.watched = history.watched == 1;
                    return new ThreadMetadata(chanThread, history.threadSize, history.lastReadPosition, new ThreadInfo(threadId, boardName, lastRefreshTime, history.watched == 1));
                })
                .doOnNext(threadMetadata -> {
                    ChanThread chanThread = threadMetadata.thread;
                    if (chanThread != null && chanThread.getPosts().size() > threadMetadata.size) {
                        ChanPost firstPost = chanThread.getPosts().get(0);
                        int size = chanThread.getPosts().size();
                        if (size > threadMetadata.size) {
                            HistoryTableConnection.putHistory(chanThread.getBoardName(), firstPost, size, threadMetadata.lastReadPosition, threadInfo.watched)
                                    .subscribe();

                            new Handler(
                                    Looper.getMainLooper()).post(
                                    () -> BusProvider.getInstance().post(new UpdateHistoryEvent(chanThread.getThreadId(), boardName, size, firstPost.isClosed())));
                        }

                    }
                })
                .doOnNext(threadMetadata -> {
                    final ChanThread response = threadMetadata.thread;
                    final ThreadInfo ti = threadMetadata.threadInfo;
                    final long id = ti.threadId;
                    final int threadSize = threadMetadata.size;
                    PostTableConnection.putThread(threadMetadata.getThread())
                            .single(false)
                            .subscribe(new SingleObserver<Boolean>() {
                                @Override
                                public void onSubscribe(Disposable d) {

                                }

                                @Override
                                public void onSuccess(Boolean success) {
                                    if (success) {
                                        if (LOG_DEBUG) {
                                            Log.d(LOG_TAG, "[refresh] Saved thread to database from auto refresh service");
                                        }

                                        if (backgrounded && threadSize < response.getPosts().size()) {
                                            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(RefreshJobService.this);
                                            final String notificationPref = preferences.getString(getString(R.string.background_notification_pref), String.valueOf(NOTIFICATIONS_ALL));
                                            final int notificationLevel = Integer.valueOf(notificationPref);

                                            createNotification(threadMetadata.thread.getBoardName(), threadMetadata.thread.getThreadId(), notificationLevel);
                                        }
                                    }

                                    jobFinished(params, false);
                                }

                                @Override
                                public void onError(Throwable throwable) {
                                    Log.e(LOG_TAG, "[refresh] Caught error while fetching posts from the auto refresh service", throwable);
                                    jobFinished(params, false);
                                }
                            });

                    if (LOG_DEBUG) {
                        Log.i(LOG_TAG, "[refresh] Setting thread size to: size=" + response.getPosts().size() + ", old size=" + threadSize);
                    }

                    if (ThreadRegistry.getInstance().getThreadSize(id) <= 0) {
                        ThreadRegistry.getInstance().add(response.getBoardName(), id, 0, response.getPosts().size(), threadInfo.watched);
                    } else {
                        ThreadRegistry.getInstance().update(response.getBoardName(), id, response.getPosts().size(), threadInfo.watched);
                    }
                })
                .compose(DatabaseUtils.applySchedulers())
                .onErrorReturn(refreshError(new ThreadInfo(threadId, boardName, lastRefreshTime, watched)))
                .subscribe(fetchComplete(params, backgrounded), fetchFailed(threadInfo, params));

        if (LOG_DEBUG) {
            Log.d(LOG_TAG, "[refresh] Starting request for " + url + ": backgrounded=" + backgrounded + ", timestamp=" + threadInfo.refreshTimestamp);
        }

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    private void startNextJob(ThreadInfo threadInfo, int code) {
        RefreshScheduler.getInstance().setWaitingForService(false);

        if (code == 404) {
            RefreshScheduler.getInstance().removeThread(threadInfo.boardName, threadInfo.threadId);

            if (LOG_DEBUG) {
                Log.e(LOG_TAG, "Error while auto refreshing thread: id=/" + threadInfo.boardName + "/" + threadInfo.threadId + ", code=" + code);
            }
        } else {

            if (LOG_DEBUG) {
                if (threadInfo != null) {
                    Log.d(LOG_TAG, "Returned from refresh service successfully: id=/" + threadInfo.boardName + "/" + threadInfo.threadId + ", size=" + ThreadRegistry.getInstance().getThreadSize(threadInfo.threadId) + ", unread=" + ThreadRegistry.getInstance().getUnreadCount(threadInfo.threadId));
                } else {
                    Log.d(LOG_TAG, "Returned from refresh service successfully: id=UNKNOWN");
                }
            }
        }

        if (threadInfo != null) {
            RefreshScheduler.getInstance().scheduleNextRun();
        }
    }

    private Consumer<ThreadMetadata> fetchComplete(final JobParameters params, final boolean inBackground) {
        return metadata -> {
            if (ChanThread.isEmpty(metadata.thread)) {
                return;
            }

            startNextJob(metadata.threadInfo, 0);
        };
    }

    private Consumer<Throwable> fetchFailed(@NonNull final ThreadInfo info, final JobParameters params) {
        return error -> {
            doFailure(info, error);
            jobFinished(params, false);
        };
    }

    private Function<Throwable, ThreadMetadata> refreshError(@NonNull final ThreadInfo info) {
        return throwable -> {
            doFailure(info, throwable);
            return new ThreadMetadata();
        };
    }

    private void doFailure(@NonNull ThreadInfo info, @NonNull Throwable error) {

        int code = 1;
        if (error instanceof HttpException) {
            HttpException exception = (HttpException) error;
            code = exception.code();
        } else {
            Log.e(LOG_TAG, "Caught error", error);
        }

        startNextJob(info, code);
        BusProvider.getInstance().post(new HttpErrorEvent(error, info));
    }

    private void createNotification(final String boardName, final long threadId, final int notificationLevel) {

        if (notificationLevel <= 1) {
            return;
        }

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

        final List<ThreadRegistryModel> updatedThreads = ThreadRegistry.getInstance().getUpdatedThreads();
        if (updatedThreads.size() > 0) {
            final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(MimiApplication.getInstance(), REFRESH_CHANNEL_ID);
            final NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();

            userPosts = 0;
            if (fetchPostDisposable != null && !fetchPostDisposable.isDisposed()) {
                fetchPostDisposable.dispose();
            }

            fetchPostDisposable = UserPostTableConnection.fetchPosts()
                    .flatMap(posts -> {
                        for (UserPost userPostDbModel : posts) {
                            ThreadRegistry.getInstance().addUserPost(userPostDbModel.boardName, userPostDbModel.threadId, userPostDbModel.postId);
                        }

                        final List<ThreadRegistryModel> models = ThreadRegistry.getInstance().getUpdatedThreads();
                        return Flowable.just(models)
                                .flatMapIterable(threadRegistryModels -> {
                                    Log.d(LOG_TAG, "Processing posts");
                                    return threadRegistryModels;
                                })
                                .flatMap(model -> Flowable.zip(
                                        PostTableConnection.fetchThread(model.getThreadId())
                                                .map(PostTableConnection.mapDbPostsToChanThread(model.getBoardName(), model.getThreadId()))
                                                .toFlowable(),
                                        Flowable.just(model),
                                        (chanThread, model1) -> {
                                            Log.d(LOG_TAG, "Processing post: " + chanThread);
                                            return processPosts(model1, chanThread, style);
                                        }
                                ))
                                .toList()
                                .toFlowable();
                    })
                    .doOnNext(threadList -> {
                        if (threadList == null) {
                            return;
                        }

                        int threadCount = 0;
                        if (notificationLevel == NOTIFICATIONS_ALL) {
                            threadCount = threadList.size();
                        } else {
                            for (Pair<ChanThread, Integer> chanThreadIntegerPair : threadList) {
                                if (chanThreadIntegerPair.second != null && chanThreadIntegerPair.second > 0) {
                                    threadCount++;
                                }
                            }

                            if (threadCount <= 0) {
                                return;
                            }
                        }

                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                        int oldThreadCount = prefs.getInt(NOTIFICATIONS_KEY_THREAD_SIZE + "." + threadId, -1);

                        if (threadCount == 0 || threadCount == oldThreadCount) {
                            return;
                        }

                        final String threadsUpdated = getResources().getQuantityString(R.plurals.threads_updated_plural, threadCount, threadCount);
                        final String repliesToYou = getResources().getQuantityString(R.plurals.replies_to_you_plurals, userPosts, userPosts);
                        style.setBigContentTitle(threadsUpdated);
                        style.setSummaryText(repliesToYou);

                        notificationBuilder.setStyle(style);
                        notificationBuilder.setContentIntent(bookmarksPendingIntent);
                        notificationBuilder.setSmallIcon(R.drawable.ic_stat_leaf_icon);

                        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                        if (notificationManager != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                String channelName = getString(R.string.mimi_thread_watcher);

                                NotificationChannel saveFileChannel = new NotificationChannel(REFRESH_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT);

                                notificationBuilder.setChannelId(REFRESH_CHANNEL_ID);
                                notificationManager.createNotificationChannel(saveFileChannel);
                            }

                            final Notification notification = notificationBuilder.build();
                            notificationManager.notify(NOTIFICATION_ID, notification);

                            if (LOG_DEBUG) {
                                Log.d(LOG_TAG, "Creating notification for /" + boardName + "/" + threadId + ": id=" + NOTIFICATION_ID);
                            }
                        }
                    })
                    .onErrorReturn(throwable -> {
                        Log.e(LOG_TAG, "Error creating notification", throwable);
                        return new ArrayList<>();
                    })
                    .compose(DatabaseUtils.applySchedulers())
                    .subscribe(success -> {

                    }, throwable -> {
                        Log.e(LOG_TAG, "Exception while creating notification", throwable);
                    });
        }
    }

    private Pair<ChanThread, Integer> processPosts(ThreadRegistryModel model, ChanThread thread, NotificationCompat.InboxStyle style) {
        if (model != null && model.getUnreadCount() > 0) {
            final String hasUnread = getResources().getQuantityString(R.plurals.has_unread_plural, model.getUnreadCount(), model.getUnreadCount());
            final String notificationTitle = "/" + model.getBoardName() + "/" + model.getThreadId() + " " + hasUnread;
            style.addLine(notificationTitle);

            int repliesToYou = -1;

            if (model.getUserPosts() != null && model.getUserPosts().size() > 0) {

                if (thread == null) {
                    return Pair.create(ChanThread.empty(), -1);
                }

                final ChanThread currentThread = ProcessThreadTask.processThread(
                        thread.getPosts(),
                        ThreadRegistry.getInstance().getUserPosts(model.getBoardName(), model.getThreadId()),
                        model.getBoardName(),
                        model.getThreadId()
                );

                final int pos = ThreadRegistry.getInstance().getLastReadPosition(model.getThreadId()) < ThreadRegistry.getInstance().getThreadSize(model.getThreadId()) ?
                        ThreadRegistry.getInstance().getLastReadPosition(model.getThreadId()) :
                        ThreadRegistry.getInstance().getThreadSize(model.getThreadId());

                if (currentThread != null && currentThread.getPosts() != null) {
                    repliesToYou = 0;
                    for (int i = pos; i < currentThread.getPosts().size(); i++) {
                        final ChanPost post = currentThread.getPosts().get(i);
                        if (LOG_DEBUG) {
                            Log.d(LOG_TAG, "post id=" + post.getNo());
                        }
                        if (post.getRepliesTo() != null && post.getRepliesTo().size() > 0) {
                            for (Long l : model.getUserPosts()) {
                                final String s = String.valueOf(l);

                                if (LOG_DEBUG) {
                                    Log.d(LOG_TAG, "Checking post id " + s);
                                }

                                if (post.getRepliesTo().indexOf(s) >= 0) {
                                    if (LOG_DEBUG) {
                                        Log.d(LOG_TAG, "Found reply to " + s);
                                    }
                                    repliesToYou++;
                                    userPosts++;
                                }
                            }
                        }
                    }
                }

                return new Pair<>(currentThread, repliesToYou);
            }
        }

        return Pair.create(ChanThread.empty(), -1);
    }

    private class ThreadMetadata {
        private final ChanThread thread;
        private final int size;
        private final int lastReadPosition;
        private final ThreadInfo threadInfo;


        ThreadMetadata(ChanThread thread, int size, int lastReadPosition, ThreadInfo threadInfo) {
            this.thread = thread;
            this.size = size;
            this.lastReadPosition = lastReadPosition;
            this.threadInfo = threadInfo;
        }

        ThreadMetadata() {
            this.thread = new ChanThread();
            this.size = -1;
            this.lastReadPosition = 0;
            this.threadInfo = new ThreadInfo();
        }

        public ChanThread getThread() {
            return thread;
        }

        public int getSize() {
            return size;
        }

        public ThreadInfo getThreadInfo() {
            return threadInfo;
        }
    }
}
