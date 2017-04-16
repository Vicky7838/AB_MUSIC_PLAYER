package com.music.player.bhandari.m.utils;

import android.util.Log;

import com.music.player.bhandari.m.model.Constants;

/**
 * Created by Amit Bhandari on 3/11/2017.
 */

public class T {
    long time;
    String m;
    public T(String m){
        this.m=m;
    }
    public void start(){
        time = System.currentTimeMillis();
    }

    public void stop(){
        Log.v(Constants.TAG,"Time elapsed for " + m + " " + (System.currentTimeMillis()-time));
    }
}
