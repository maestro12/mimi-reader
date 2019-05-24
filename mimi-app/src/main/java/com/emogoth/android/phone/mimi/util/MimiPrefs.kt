package com.emogoth.android.phone.mimi.util

import android.content.Context
import android.net.ConnectivityManager
import android.preference.PreferenceManager
import com.emogoth.android.phone.mimi.R

class MimiPrefs {
    companion object {
        @JvmStatic
        fun navDrawerBookmarkCount(context: Context): Int {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val countString = prefs.getString(context.getString(R.string.nav_drawer_bookmark_count_pref), "5")
            return Integer.parseInt(countString!!)
        }

        @JvmStatic
        fun wifiConnected(app: Context): Boolean {
            val connManager = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

            return wifi.isConnected
        }

        @JvmStatic
        fun preloadEnabled(context: Context): Boolean {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val pref = prefs.getString(context.getString(R.string.gallery_preload_pref), "2")
            val value = pref?.toInt() ?: 2

            return when (value) {
                2 -> {
                    wifiConnected(context)
                }
                1 -> true
                0 -> false
                else -> false
            }
        }

        @JvmStatic
        fun scrollThreadWithGallery(context: Context): Boolean {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            return prefs.getBoolean(context.getString(R.string.scroll_thread_with_gallery_pref), true)
        }

        @JvmStatic
        fun closeGalleryOnClick(context: Context): Boolean {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            return prefs.getBoolean(context.getString(R.string.close_gallery_on_click_pref), true)
        }

        @JvmStatic
        fun refreshInterval(context: Context): Int {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            return prefs.getString(context.getString(R.string.app_auto_refresh_time), "10")?.toInt() ?: 10
        }

        @JvmStatic
        fun userOriginalFilename(context: Context): Boolean {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            return prefs.getBoolean(context.getString(R.string.use_original_filename_pref), false)
        }
    }
}