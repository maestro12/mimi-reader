package com.mimireader.chanlib.interfaces;

import androidx.annotation.NonNull;

import com.mimireader.chanlib.models.ArchivedChanThread;

public interface ArchiveConverter {
    ArchivedChanThread toArchivedThread(@NonNull String board, long threadId, @NonNull String name, @NonNull String domain);
}
