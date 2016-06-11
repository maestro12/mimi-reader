package com.emogoth.android.phone.mimi.util;


import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import com.emogoth.android.phone.mimi.R;

public class FourChanUtil {
    private static final String LOG_TAG = FourChanUtil.class.getSimpleName();

    public static Spannable getUserName(final Context context, final String userName, final String capcode) {
        String name = userName;

        if (name == null) {
            name = "";
        }

        int color = 0;
        if (!TextUtils.isEmpty(capcode)) {
            if (capcode.toLowerCase().equals("mod")) {
                name = name + " ## Mod";
                color = context.getResources().getColor(R.color.mod_username_color);
            } else if (capcode.toLowerCase().equals("admin")) {
                name = name + " ## Admin";
                color = context.getResources().getColor(R.color.admin_username_color);
            }
        }

        final Spannable nameSpan = new SpannableString(name);

        if (color != 0) {
            final ForegroundColorSpan colorSpan = new ForegroundColorSpan(color);
            nameSpan.setSpan(colorSpan, 0, name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return nameSpan;

    }

}
