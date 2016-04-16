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

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;

import java.io.File;


public class SimpleMediaScannerConnction implements MediaScannerConnection.MediaScannerConnectionClient {
    private final File fileToScan;
    private final MediaScannerConnection mediaScannerConnection;

    public SimpleMediaScannerConnction(final Context context, final File f) {
        fileToScan = f;
        mediaScannerConnection = new MediaScannerConnection(context, this);
        try {
            mediaScannerConnection.connect();
        }
        catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMediaScannerConnected() {
        mediaScannerConnection.scanFile(fileToScan.getAbsolutePath(), null);
    }

    @Override
    public void onScanCompleted(final String path, final Uri uri) {
        if(path != null) {
            if(path.equals(fileToScan.getAbsolutePath())) {
                mediaScannerConnection.disconnect();
            }
        }
    }
}