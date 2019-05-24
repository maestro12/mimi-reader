package com.emogoth.android.phone.mimi.autorefresh;

import com.emogoth.android.phone.mimi.model.ThreadInfo;
import com.squareup.tape2.ObjectQueue;

import java.io.IOException;
import java.io.OutputStream;

public class RefreshQueueConverter implements ObjectQueue.Converter<ThreadInfo> {

    @Override
    public ThreadInfo from(byte[] bytes) throws IOException {
        return ThreadInfo.from(bytes);
    }

    @Override
    public void toStream(ThreadInfo o, OutputStream bytes) throws IOException {
        o.toStream(bytes);
    }
}
