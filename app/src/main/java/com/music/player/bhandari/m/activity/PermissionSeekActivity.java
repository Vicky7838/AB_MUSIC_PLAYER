package com.music.player.bhandari.m.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.UIElemetHelper.ColorHelper;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.service.PlayerService;
import com.music.player.bhandari.m.utils.MusicLibrary;
import com.music.player.bhandari.m.utils.MyApp;

/**
 * Created by Amit Bhandari on 1/29/2017.
 */

public class PermissionSeekActivity extends AppCompatActivity {


    final int MY_PERMISSIONS_REQUEST = 0;
    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private boolean mBound=false;
    private ServiceConnection playerServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            PlayerService.PlayerBinder playerBinder = (PlayerService.PlayerBinder) service;
            PlayerService playerService = playerBinder.getService();
            MyApp.setService(playerService);
            mBound=true;
            Log.v(Constants.TAG,"LAUNCH MAIN ACTIVITY");
            startActivity(new Intent(PermissionSeekActivity.this, MainActivity.class));
            finish();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound=false;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //check version and make changes if any
        getResources().getDrawable(R.drawable.ic_batman_1).setColorFilter(ColorHelper.getPrimaryColor(), PorterDuff.Mode.OVERLAY);
        changeSettingsForVersion();
        if(!hasPermissions(this, PERMISSIONS)) {
            RequestPermission();
        }else {
            bindService();
        }
    }

    private void RequestPermission(){
        // Here, thisActivity is the current activity

            ActivityCompat.requestPermissions(this,
                    PERMISSIONS,
                    MY_PERMISSIONS_REQUEST);

    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void changeSettingsForVersion() {
        int verCode = 0;
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            verCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        /*
        if (MyApp.getPref().getInt(getString(R.string.pref_version_code),0) < verCode && (verCode == 8)) {
            //change base theme to glossy and color to something nice
            MyApp.getPref().edit().putInt(getString(R.string.pref_theme),Constants.THEME.GLOSSY).apply();
            MyApp.getPref().edit().putInt(getString(R.string.pref_theme_color),Constants.THEME.PINK_CERISE).apply();
            MyApp.getPref().edit().putInt(getString(R.string.pref_version_code),verCode).apply();
        }*/

        //if first install
        if(MyApp.getPref().getBoolean(getString(R.string.pref_first_install),true)) {
            MyApp.getPref().edit().putBoolean(getString(R.string.pref_first_install),false).apply();
            MyApp.getPref().edit().putInt(getString(R.string.pref_theme),Constants.THEME.GLOSSY).apply();
            MyApp.getPref().edit().putInt(getString(R.string.pref_theme_color),Constants.THEME.ANTIQUE_RUBY).apply();
            MyApp.getPref().edit().putInt(getString(R.string.pref_version_code),verCode).apply();
        }
    }

    private void bindService(){
        MusicLibrary.getInstance();
        startService(new Intent(this,PlayerService.class));
        try {
            Intent playerServiceIntent = new Intent(this, PlayerService.class);
            bindService(playerServiceIntent, playerServiceConnection, Context.BIND_AUTO_CREATE);
        }catch (Exception e){

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if(mBound) {
                unbindService(playerServiceConnection);
                mBound=false;
            }
        }catch (Exception ignored){

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    bindService();
                } else {

                    if(grantResults[0] == PackageManager.PERMISSION_DENIED){
                        //READ PHONE STATE DENIED
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_HOME);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        Toast.makeText(this, "Reading phone state is necessary to control playback during calls!", Toast.LENGTH_LONG).show();
                        finish();
                    }else {
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_HOME);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        Toast.makeText(this, "Please provide storage permission from Settings for app to work properly", Toast.LENGTH_LONG).show();
                        finish();
                    }
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }
            break;



            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
