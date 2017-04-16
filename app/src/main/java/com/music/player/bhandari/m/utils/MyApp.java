package com.music.player.bhandari.m.utils;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;

import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.service.PlayerService;

/**
 * Created by Amit Bhandari on 1/26/2017.
 */

public class MyApp extends Application {
    private static MyApp instance;
    private static SharedPreferences pref;
    private static PlayerService service;
    private static boolean isLocked = false;
    public static boolean isAppVisible;

    @Override
    public void onCreate() {
        Log.v(Constants.TAG,"App created");
        instance = this;
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        super.onCreate();
    }

    public static MyApp getInstance() {
        return instance;
    }

    public static Context getContext(){
        return instance;
        // or return instance.getApplicationContext();
    }

    public static SharedPreferences getPref(){
        return pref;
        // or return instance.getApplicationContext();
    }

    public  static void setService(PlayerService s){
        service = s;
    }

    public static  PlayerService getService(){
        return  service;
    }

    public static boolean isLocked(){return isLocked;}

    public static void setLocked(boolean lock){isLocked = lock;}

}