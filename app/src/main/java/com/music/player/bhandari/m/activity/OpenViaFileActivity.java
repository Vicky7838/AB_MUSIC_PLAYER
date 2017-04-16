package com.music.player.bhandari.m.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.model.TrackItem;
import com.music.player.bhandari.m.service.PlayerService;
import com.music.player.bhandari.m.utils.MusicLibrary;
import com.music.player.bhandari.m.utils.MyApp;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * Created by Amit Bhandari on 2/10/2017.
 */

public class OpenViaFileActivity extends AppCompatActivity {
    File fileFromFileExplorer;
    boolean mBound;

    private ServiceConnection playerServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            Log.v(Constants.TAG,"Service  bound");
            PlayerService.PlayerBinder playerBinder = (PlayerService.PlayerBinder) service;
            PlayerService playerService = playerBinder.getService();
            MyApp.setService(playerService);
            playSong();
            //playerService.notifyUI();
            mBound=true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound=false;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Intent.ACTION_VIEW.equals(getIntent().getAction())){
            Log.v(Constants.TAG,"Received open intent");
            Uri fileURI;
            fileURI = getIntent().getData();

            fileFromFileExplorer = null;
            String path = fileURI.toString() ;// "/mnt/sdcard/FileName.mp3"
            try {
                fileFromFileExplorer = new File(new URI(path));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

            if(MyApp.getService()==null){
                Log.v(Constants.TAG,"Service is null");
                MusicLibrary.getInstance();
                startService(new Intent(this,PlayerService.class));
                try {
                    Intent playerServiceIntent = new Intent(this, PlayerService.class);
                    bindService(playerServiceIntent, playerServiceConnection, Context.BIND_AUTO_CREATE);
                }catch (Exception e){
                    Toast.makeText(this,"Error playing file!",Toast.LENGTH_LONG).show();
                }
            }else {
                playSong();
            }

        } else {
            Log.d(Constants.TAG, "intent was something else: "+getIntent().getAction());
        }
    }

    private void playSong(){
        MyApp.getService().playTrackFromFile(fileFromFileExplorer);

        startActivity(new Intent(OpenViaFileActivity.this, NowPlayingActivity.class)
                .setAction(Constants.ACTION.OPEN_FROM_FILE_EXPLORER));
        finish();
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
}
