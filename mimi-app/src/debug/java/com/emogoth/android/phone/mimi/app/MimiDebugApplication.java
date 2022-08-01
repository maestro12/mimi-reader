package com.emogoth.android.phone.mimi.app;


import com.facebook.stetho.Stetho;

public class MimiDebugApplication extends MimiApplication {
    @Override
    public void onCreate() {
        super.onCreate();

        Stetho.initializeWithDefaults(this);
    }
}
