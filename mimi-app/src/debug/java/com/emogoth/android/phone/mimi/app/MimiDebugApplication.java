package com.emogoth.android.phone.mimi.app;


import com.facebook.stetho.Stetho;
import com.squareup.leakcanary.LeakCanary;

public class MimiDebugApplication extends MimiApplication {
    @Override
    public void onCreate() {
        super.onCreate();

//        if (!LeakCanary.isInAnalyzerProcess(this)) {
//            setRefWatcher(LeakCanary.install(this));
//        }
        Stetho.initializeWithDefaults(this);
    }
}
