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

package com.mimireader.chanlib.util;


import android.graphics.Color;

public class ChanUtil {

    public static String getVariableOrEmptyString(String var) {
        return var == null ? "" : var;
    }

    // From: https://gist.github.com/odedhb/79d9ea471c10c040245e
    public static int calculateColorBase(String name) {
        String opacity = "#88"; //opacity between 00-ff
        String hexColor = String.format(
                opacity + "%06X", (0xffffff & name.hashCode()));

        return Color.parseColor(hexColor);
    }
}
