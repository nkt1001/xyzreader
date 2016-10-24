package com.example.xyzreader;

import android.app.Application;

import com.facebook.stetho.BuildConfig;
import com.facebook.stetho.Stetho;

/**
 * Created by Nkt1001 on 24.10.2016.
 */

public class XYZApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) Stetho.initializeWithDefaults(this);
    }
}
