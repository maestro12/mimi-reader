package com.emogoth.android.phone.mimi.util;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import androidx.annotation.Nullable;

import java.io.File;

public class SingleMediaScanner implements MediaScannerConnection.MediaScannerConnectionClient {

    private final MediaScannerConnection.OnScanCompletedListener scanCompleteListener;
    private MediaScannerConnection mediaScannerConnection;
    private File file;

    public SingleMediaScanner(final Context context, File f, @Nullable MediaScannerConnection.OnScanCompletedListener listener) {
        file = f;
        scanCompleteListener = listener;
        mediaScannerConnection = new MediaScannerConnection(context, this);
        mediaScannerConnection.connect();
    }

    @Override
    public void onMediaScannerConnected() {
        mediaScannerConnection.scanFile(file.getAbsolutePath(), null);
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        mediaScannerConnection.disconnect();

        if (scanCompleteListener != null) {
            scanCompleteListener.onScanCompleted(path, uri);
        }
    }

}
