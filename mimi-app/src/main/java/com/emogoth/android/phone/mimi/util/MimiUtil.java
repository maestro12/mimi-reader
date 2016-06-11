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
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.StartupActivity;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.db.HistoryTableConnection;
import com.google.android.exoplayer.util.Util;
import com.mimireader.chanlib.models.ChanThread;

import java.io.BufferedOutputStream;
import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * General purpose util class to handle the generic methods used throughout the app
 */
public class MimiUtil {
    private static final String LOG_TAG = MimiUtil.class.getSimpleName();
    private static MimiUtil ourInstance = new MimiUtil();

    private static final int THREAD_POOL_CORE_SIZE = 2;
    private static final int THREAD_POOL_MAX_SIZE = 10;
    private static final int THREAD_POOL_KEEP_ALIVE = 10;

    private static final long AD_TIMEOUT = 30 * 60 * 1000;

    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;

    public static final int THEME_COLOR_DEFAULT = 0;
    public static final int THEME_COLOR_RED = 1;
    public static final int THEME_COLOR_GREEN = 2;
    public static final int THEME_COLOR_BLUE = 3;
    public static final int THEME_COLOR_INDIGO = 4;
    public static final int THEME_COLOR_PINK = 5;
    public static final int THEME_COLOR_PURPLE = 6;
    public static final int THEME_COLOR_ORANGE = 7;
    public static final int THEME_COLOR_DEEP_ORANGE = 8;
    public static final int THEME_COLOR_BROWN = 9;
    public static final int THEME_COLOR_BLUE_GREY = 10;
    public static final int THEME_COLOR_LIGHT_GREY = 11;
    public static final int THEME_COLOR_GREY = 12;
    public static final int THEME_COLOR_DARK_GREY = 13;
    public static final int THEME_COLOR_BLACK = 14;

    private int toolbarColor;
    private int theme;
    private int themeResource;

    private int nextLoaderId;
    private HashMap<Object, Integer> loaderIdMap;

    private Context context;

    private File cacheDir;
    private File bookmarkDir;
    private String cacheDirPrefString;
    private boolean ready = false;

    private int quoteColor;
    private int replyColor;
    private int highlightColor;
    private int linkColor;

    private static Integer[][] themeArray = new Integer[2][15];

    static {
        themeArray[THEME_LIGHT][THEME_COLOR_DEFAULT] = R.style.Theme_Mimi_Light_Toolbar_Default;
        themeArray[THEME_LIGHT][THEME_COLOR_RED] = R.style.Theme_Mimi_Light_Toolbar_Red;
        themeArray[THEME_LIGHT][THEME_COLOR_GREEN] = R.style.Theme_Mimi_Light_Toolbar_Green;
        themeArray[THEME_LIGHT][THEME_COLOR_BLUE] = R.style.Theme_Mimi_Light_Toolbar_Blue;
        themeArray[THEME_LIGHT][THEME_COLOR_INDIGO] = R.style.Theme_Mimi_Light_Toolbar_Indigo;
        themeArray[THEME_LIGHT][THEME_COLOR_PINK] = R.style.Theme_Mimi_Light_Toolbar_Pink;
        themeArray[THEME_LIGHT][THEME_COLOR_PURPLE] = R.style.Theme_Mimi_Light_Toolbar_Purple;
        themeArray[THEME_LIGHT][THEME_COLOR_ORANGE] = R.style.Theme_Mimi_Light_Toolbar_Orange;
        themeArray[THEME_LIGHT][THEME_COLOR_DEEP_ORANGE] = R.style.Theme_Mimi_Light_Toolbar_DeepOrange;
        themeArray[THEME_LIGHT][THEME_COLOR_BROWN] = R.style.Theme_Mimi_Light_Toolbar_Brown;
        themeArray[THEME_LIGHT][THEME_COLOR_BLUE_GREY] = R.style.Theme_Mimi_Light_Toolbar_BlueGrey;
        themeArray[THEME_LIGHT][THEME_COLOR_LIGHT_GREY] = R.style.Theme_Mimi_Light_Toolbar_LightGrey;
        themeArray[THEME_LIGHT][THEME_COLOR_GREY] = R.style.Theme_Mimi_Light_Toolbar_Grey;
        themeArray[THEME_LIGHT][THEME_COLOR_DARK_GREY] = R.style.Theme_Mimi_Light_Toolbar_DarkGrey;
        themeArray[THEME_LIGHT][THEME_COLOR_BLACK] = R.style.Theme_Mimi_Light_Toolbar_Black;

        themeArray[THEME_DARK][THEME_COLOR_DEFAULT] = R.style.Theme_Mimi_Dark_Toolbar_Default;
        themeArray[THEME_DARK][THEME_COLOR_RED] = R.style.Theme_Mimi_Dark_Toolbar_Red;
        themeArray[THEME_DARK][THEME_COLOR_GREEN] = R.style.Theme_Mimi_Dark_Toolbar_Green;
        themeArray[THEME_DARK][THEME_COLOR_BLUE] = R.style.Theme_Mimi_Dark_Toolbar_Blue;
        themeArray[THEME_DARK][THEME_COLOR_INDIGO] = R.style.Theme_Mimi_Dark_Toolbar_Indigo;
        themeArray[THEME_DARK][THEME_COLOR_PINK] = R.style.Theme_Mimi_Dark_Toolbar_Pink;
        themeArray[THEME_DARK][THEME_COLOR_PURPLE] = R.style.Theme_Mimi_Dark_Toolbar_Purple;
        themeArray[THEME_DARK][THEME_COLOR_ORANGE] = R.style.Theme_Mimi_Dark_Toolbar_Orange;
        themeArray[THEME_DARK][THEME_COLOR_DEEP_ORANGE] = R.style.Theme_Mimi_Dark_Toolbar_DeepOrange;
        themeArray[THEME_DARK][THEME_COLOR_BROWN] = R.style.Theme_Mimi_Dark_Toolbar_Brown;
        themeArray[THEME_DARK][THEME_COLOR_BLUE_GREY] = R.style.Theme_Mimi_Dark_Toolbar_BlueGrey;
        themeArray[THEME_DARK][THEME_COLOR_LIGHT_GREY] = R.style.Theme_Mimi_Dark_Toolbar_LightGrey;
        themeArray[THEME_DARK][THEME_COLOR_GREY] = R.style.Theme_Mimi_Dark_Toolbar_Grey;
        themeArray[THEME_DARK][THEME_COLOR_DARK_GREY] = R.style.Theme_Mimi_Dark_Toolbar_DarkGrey;
        themeArray[THEME_DARK][THEME_COLOR_BLACK] = R.style.Theme_Mimi_Dark_Toolbar_Black;
    }

    private static Map<String, Integer> fontSizeMap = new HashMap<>();

    static {
        fontSizeMap.put("0", R.style.FontStyle_Small);
        fontSizeMap.put("1", R.style.FontStyle_Medium);
        fontSizeMap.put("2", R.style.FontStyle_Large);
    }

    public static MimiUtil getInstance() {
        return ourInstance;
    }

    private MimiUtil() {
    }

    public void init(final Context context) {

        if (ready) {
            return;
        }

        this.context = context;

        final SharedPreferences sh = PreferenceManager.getDefaultSharedPreferences(context);

        final String themeShadePrefString = context.getString(R.string.theme_pref);
        final String themeColorPrefString = context.getString(R.string.theme_color_pref);
        int themeId = Integer.valueOf(sh.getString(themeShadePrefString, "0"));
        int colorId = Integer.valueOf(sh.getString(themeColorPrefString, "0"));

        setCurrentTheme(themeId, colorId);
        Log.i(LOG_TAG, "Set theme: id=" + themeId + ", resource=" + getThemeResourceId());

//        if (boardOrderPrefString == null) {
//            boardOrderPrefString = context.getString(R.string.board_order_pref);
//        }

        cacheDirPrefString = context.getString(R.string.cache_external_pref);
        if (sh.getBoolean(cacheDirPrefString, false)) {
            Log.i(LOG_TAG, "Setting cache dir to sd card storage");
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                cacheDir = new File(context.getExternalFilesDir(state), "cache");
                if (!cacheDir.exists()) {
                    if (!cacheDir.mkdirs()) {
                        cacheDir = context.getCacheDir();
                    }
                }
            } else {
                cacheDir = context.getCacheDir();
            }
        } else {
            Log.i(LOG_TAG, "Setting cache dir to internal storage");
            cacheDir = context.getCacheDir();
        }

        nextLoaderId = 0;
        loaderIdMap = new HashMap<>();

        bookmarkDir = new File(context.getFilesDir(), "bookmarks/");

        quoteColor = sh.getInt(context.getString(R.string.quote_color_pref), context.getResources().getColor(R.color.quote));
        replyColor = sh.getInt(context.getString(R.string.reply_color_pref), context.getResources().getColor(R.color.reply));
        highlightColor = sh.getInt(context.getString(R.string.highlight_color_pref), context.getResources().getColor(R.color.reply_highlight));
        linkColor = sh.getInt(context.getString(R.string.link_color_pref), context.getResources().getColor(R.color.link));

        ready = true;
    }

    public int getQuoteColor() {
        return quoteColor;
    }

    public int getReplyColor() {
        return replyColor;
    }

    public int getHighlightColor() {
        return highlightColor;
    }

    public int getLinkColor() {
        return linkColor;
    }

    public void setQuoteColor(int quoteColor) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(context.getString(R.string.quote_color_pref), quoteColor)
                .apply();
        this.quoteColor = quoteColor;
    }

    public void setReplyColor(int replyColor) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(context.getString(R.string.reply_color_pref), replyColor)
                .apply();
        this.replyColor = replyColor;
    }

    public void setHighlightColor(int highlightColor) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(context.getString(R.string.highlight_color_pref), highlightColor)
                .apply();
        this.highlightColor = highlightColor;
    }

    public void setLinkColor(int linkColor) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(context.getString(R.string.link_color_pref), linkColor)
                .apply();
        this.linkColor = linkColor;
    }

    public static LayoutType getLayoutType(final Context context) {
        final String layoutName = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.start_activity_pref), StartupActivity.getDefaultStartupActivity());

        try {
            LayoutType type = LayoutType.valueOf(layoutName.toUpperCase());
            return type;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error getting layout type", e);
        }

        return StartupActivity.DEFAULT_LAYOUT_TYPE;
    }

    public static boolean historyEnabled(final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.save_history_pref), true);
    }

    public int getLoaderId(final Class clazz) {
        Integer loaderId = loaderIdMap.get(clazz);
        if (loaderId == null) {
            loaderId = nextLoaderId;
            nextLoaderId++;
            loaderIdMap.put(clazz, loaderId);
        }

        return loaderId;
    }

    public static File getSaveDir(final Context context) {
        final String dir = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.image_file_location_pref), null);

        if (!TextUtils.isEmpty(dir)) {
            return new File(dir);
        }

        return new File(Environment.getExternalStorageDirectory(), "/Mimi");
    }

    public static String getTempPath(final Context context, final String extension) {
        return getSaveDir(context).getAbsolutePath() + "/tmp/temp." + extension;
    }

    public void setSaveDir(final Context context, final String path) {
        final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(context.getString(R.string.image_file_location_pref), path).apply();
    }

    public static int getFontStyle(final Context context) {
        final String style = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.font_style_pref), "0");
        return fontSizeMap.get(style);
    }

    private void clearBookmarkFiles() {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {

            }
        };

        final File dir = getBookmarkDir();
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                final File entry = new File(children[i]);
                if (!entry.isDirectory()) {
                    entry.delete();
                }

            }
        }
    }

    public int getTheme() {
        return this.theme;
    }

    public int getThemeResourceId() {
        return themeResource;
    }

    public void setCurrentTheme(int theme, int colorId) {
        this.theme = theme;
        this.toolbarColor = colorId;
        themeResource = themeArray[theme][colorId];
    }

    public void removeBookmark(String boardName, int threadId) {
        final File bookmarkFile = getBookmarkFile(getBookmarkDir(), boardName, threadId);

        try {
            if (bookmarkFile != null) {
                if (bookmarkFile.exists()) {
                    Log.d(LOG_TAG, "Deleting file: " + bookmarkFile.getAbsolutePath());
                    bookmarkFile.delete();
                }
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "Could not delete bookmark: board=" + boardName + ", thread=" + threadId, e);
        }
    }

    public void saveBookmark(final ChanThread data, final OperationCompleteListener listener) {
        if (data == null) {
            return;
        }

        HistoryTableConnection.putHistory(data.getBoardName(), data.getPosts().get(0), data.getPosts().size(), true)
                .flatMap(new Func1<Boolean, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(Boolean aBoolean) {
                        final File bookmarkFile = getBookmarkFile(getBookmarkDir(), data.getBoardName(), data.getThreadId());

                        if (bookmarkFile != null) {
                            if (bookmarkFile.exists()) {
                                Log.d(LOG_TAG, "Deleting file: " + bookmarkFile.getAbsolutePath());
                                bookmarkFile.delete();
                            }

                            return Observable.just(MimiUtil.writeObjectToFile(bookmarkFile, data));
                        }
                        return Observable.just(null);
                    }
                })
                .compose(DatabaseUtils.<Boolean>applySchedulers())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean success) {
                        if (success) {
                            if(listener != null) {
                                listener.onOperationComplete();
                            }
                        } else {
                            if (listener != null) {
                                listener.onOperationFailed();
                            }
                        }
                    }
                });
    }

    public File getBookmarkDir() {
        return this.bookmarkDir;
    }

    public static File getBookmarkFile(final File bookmarkDir, final String boardName, final int threadId) {
        final String fileName = boardName + "_" + String.valueOf(threadId);

        if (!bookmarkDir.exists()) {
            if (!bookmarkDir.mkdirs()) {
                return null;
            }
        }

        return new File(bookmarkDir, fileName);

    }

    public File getCacheDir() {
        return cacheDir;
    }

    public static int getMaxCacheSize(final Context context) {
        final int memClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
        final int cacheSize = 1024 * 1024 * memClass / 8;

        return cacheSize;
    }

    public static int getBoardOrder(final Context context) {
        try {
            return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.board_order_pref), 0);
        } catch (final ClassCastException e) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(context.getString(R.string.board_order_pref), 0).apply();
        }

        return 0;
    }

    public static void setBoardOrder(final Context context, final int order) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(context.getString(R.string.board_order_pref), order).apply();
    }

    public static String httpOrHttps(final Context context) {
        final String securePref = context.getString(R.string.use_ssl_pref);
        final String httpString;
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(securePref, false)) {
            httpString = "https://";
        } else {
            httpString = "http://";
        }

        return httpString;
    }

    public static boolean isSecureConnection(final Context context) {
        final String securePref = context.getString(R.string.use_ssl_pref);
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(securePref, false);
    }

    public static void setScreenOrientation(final Activity activity) {
        final String orientationPref = activity.getString(R.string.prevent_screen_rotation_pref);

        if (PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(orientationPref, false)) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        }
    }

    public View createActionBarNotification(final LayoutInflater inflater, final ViewGroup container, final int count) {
        final View v = inflater.inflate(R.layout.badge_layout, container, false);

        if (v != null) {
            final TextView bookmarkCountButton = (TextView) v.findViewById(R.id.bookmark_count);
            if (count > 0) {
                bookmarkCountButton.setBackgroundResource(R.drawable.new_unread_count);
            } else {
                bookmarkCountButton.setBackgroundResource(R.drawable.no_unread_count);
            }
            bookmarkCountButton.setText(String.valueOf(count));

            bookmarkCountButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(LOG_TAG, "Clicked badge");
                }
            });

            return v;
        }

        return null;
    }

    public ChanThread getBookmarkedThread(final String boardName, final int threadId) {
        if (!bookmarkDir.exists()) {
            return null;
        }

        final File bookmarkFile = getBookmarkFile(bookmarkDir, boardName, threadId);
        if (bookmarkFile == null || !bookmarkFile.exists()) {
            return null;
        }

        ObjectInputStream input;

        try {
            input = new ObjectInputStream(new FileInputStream(bookmarkFile));
            ChanThread thread = new ChanThread();
            thread.readExternal(input);
            input.close();

            return thread;
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(LOG_TAG, "Returning null bookmark");
        return null;
    }

    public static boolean writeObjectToFile(final File saveFile, final Externalizable data) {
        ObjectOutput out;
        try {
            out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(saveFile, true)));
            data.writeExternal(out);
//            out.writeObject(data);
            out.close();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error writing bookmark to file", e);
            return false;
        }

        return true;
    }

    public boolean isLoggedIn() {
        final CookieStore cookieStore = HttpClientFactory.getInstance().getCookieStore();
        boolean found = false;

        for (final HttpCookie cookie : cookieStore.getCookies()) {
            if ("pass_enabled".equals(cookie.getName()) && "1".equals(cookie.getValue())) {
                found = true;
            }
        }

        return found;
    }

    public static boolean handleYouTubeLinks(Context contex) {
        return PreferenceManager.getDefaultSharedPreferences(contex)
                .getBoolean(contex.getString(R.string.handle_youtube_links_pref), true);
    }

    public void logout() {
        HttpClientFactory.getInstance().getCookieStore().removeAll();
    }

    public void openMarketLink(final Context context) {
        final String applicationName = context.getPackageName();
        if (applicationName != null) {
            if (applicationName.toLowerCase().contains("amazon")) {
                try {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.amazon.com/EmoGoth-Mimi-Reader/dp/B00EGIPHC8/")));
                } catch (ActivityNotFoundException e) {
                    Log.e(LOG_TAG, "Could not open market link to Amazon", e);
                }
            } else {
                try {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + applicationName)));
                } catch (ActivityNotFoundException e) {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + applicationName)));
                }
            }
        }

        Log.i(LOG_TAG, "Using package: name=" + applicationName);
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (width > reqWidth) {
            int remainder = width % reqWidth;
            inSampleSize = Math.round((float) width / (float) reqWidth);
            if (remainder >= 3 || inSampleSize == 0) {
                inSampleSize++;
            }
        }

        final String samsungString = Build.MANUFACTURER.toLowerCase();

        if (samsungString != null && samsungString.contains("samsung")) {
            inSampleSize *= 2;
        }

        return inSampleSize;
    }

    public static String removeExtention(String filePath) {
        // These first few lines the same as Justin's
        File f = new File(filePath);

        // if it's a directory, don't remove the extention
        if (f.isDirectory()) return filePath;

        String name = f.getName();

        // Now we know it's a file - don't need to do any special hidden
        // checking or contains() checking because of:
        final int lastPeriodPos = name.lastIndexOf('.');
        if (lastPeriodPos <= 0) {
            // No period after first character - return name as it was passed in
            return filePath;
        } else {
            // Remove the last period and everything after it
            File renamed = new File(f.getParent(), name.substring(0, lastPeriodPos));
            return renamed.getPath();
        }
    }

    public static boolean rememberThreadScrollPosition(final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.remember_thread_position_pref), true);
    }

    public static String parseKeywordsFromBoardTitle(final String boardTitle) {
        if (boardTitle != null) {
            String[] keywordArray;
            if (boardTitle.contains("&")) {
                keywordArray = boardTitle.split("&");

                if (keywordArray.length > 1) {
                    String keywords = null;
                    int i = 0;
                    while (i < keywordArray.length) {
                        keywords = keywordArray[i];
                        i++;

                        if (i < keywordArray.length) {
                            keywords = keywords + ",";
                        }
                    }

                    return keywords;
                }
            }

            if (boardTitle.contains("/")) {
                keywordArray = boardTitle.split("&");

                if (keywordArray.length > 1) {
                    String keywords = null;
                    int i = 0;
                    while (i < keywordArray.length) {
                        keywords = keywordArray[i];
                        i++;

                        if (i < keywordArray.length) {
                            keywords = keywords + ",";
                        }
                    }

                    return keywords;
                }
            }
        }

        return null;
    }

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static boolean isCrappySamsung() {
        if (Util.SDK_INT <= 19 && "samsung".equals(Util.MANUFACTURER)) {
            return true;
        }

        return false;
    }

    public interface OperationCompleteListener {
        void onOperationComplete();

        void onOperationFailed();
    }

}
