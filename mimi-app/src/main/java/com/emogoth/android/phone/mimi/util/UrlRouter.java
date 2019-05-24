package com.emogoth.android.phone.mimi.util;

import android.content.Intent;
import android.content.UriMatcher;
import android.net.Uri;

public class UrlRouter {
    public static final int UNSET = -1;
    public static final int SINGLE_BOARD = 1;
    public static final int CATALOG = 2;
    public static final int THREAD = 3;

    private static String FOURCHAN_BASE_AUTHORITY = "4chan.org";
    private static String FOURCHAN_BOARDS_AUTHORITY = "boards." + FOURCHAN_BASE_AUTHORITY;

    private static UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(FOURCHAN_BOARDS_AUTHORITY, "*/catalog", CATALOG);
        uriMatcher.addURI(FOURCHAN_BOARDS_AUTHORITY, "*/thread/#/*", THREAD);
        uriMatcher.addURI(FOURCHAN_BOARDS_AUTHORITY, "*/thread/#", THREAD);
        uriMatcher.addURI(FOURCHAN_BOARDS_AUTHORITY, "*", SINGLE_BOARD);
    }

    public static int matchUri(Uri uri) {
        return uriMatcher.match(uri);
    }

//    public static Intent getIntent(Uri uri) {
//        int val = uriMatcher.match(uri);
//
//        switch (val) {
//            case SINGLE_BOARD:
//
//        }
//    }
//
//    private static Intent singleBoardIntent(Uri uri) {
//
//    }
}
