package com.hirbod.instadownloader;

import android.app.Application;

import ir.tapsell.sdk.Tapsell;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Tapsell.initialize(this, "ncqbftmddjkqmomkirbobtmefolofacgsjchcsoeigrpghenfkptpnitfcfboamembmrgh");
    }
}
