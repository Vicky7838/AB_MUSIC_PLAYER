package com.music.player.bhandari.m.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.UIElemetHelper.ColorHelper;
import com.music.player.bhandari.m.customViews.nowplaying.MusicPlayerView;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.service.PlayerService;
import com.music.player.bhandari.m.utils.MusicLibrary;
import com.music.player.bhandari.m.utils.MyApp;
import com.music.player.bhandari.m.utils.UtilityFun;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by Amit Bhandari on 3/10/2017.
 */

public class FragmentDisc extends Fragment implements View.OnClickListener {

    private static MusicPlayerView mpv;
    private PlayerService playerService;
    private BroadcastReceiver mDiscUpdate;


    public FragmentDisc(){}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.fragment_disc, container, false);
        playerService = MyApp.getService();
        mpv = (MusicPlayerView) layout.findViewById(R.id.mpv);
        mpv.setOnClickListener(this);
        UpdateDisc();
        mDiscUpdate = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v(Constants.TAG, "update UI please Jarvis");
                UpdateDisc();
            }
        };

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mDiscUpdate
                ,new IntentFilter(Constants.ACTION.DISC_UPDATE));

        return layout;
    }

    @Override
    public void onDestroyView() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mDiscUpdate);
        super.onDestroyView();
    }

    @Override
    public void onPause() {
        if(mpv.isRotating()) {
            mpv.stop();
        }
        Log.v(Constants.TAG,"Disc paused........");
        super.onPause();
    }

    @Override
    public void onResume() {

        if(!mpv.isRotating()){
            if(playerService!=null && playerService.getStatus()==PlayerService.PLAYING){
                mpv.start();
            }
        }
        Log.v(Constants.TAG,"Disc resumed........");
        super.onResume();
    }

    private void UpdateDisc(){

        if(getActivity()==null || !isAdded()){
            return;
        }

        Bitmap b=null ;// = playerService.getAlbumArt();
        try {
          //  b = MediaStore.Images.Media.getBitmap(getContext().getContentResolver()
            //        , MusicLibrary.getInstance().getAlbumArtUri(playerService.getCurrentTrack().getAlbumId()));
           b= UtilityFun.decodeUri(getContext()
                   ,MusicLibrary.getInstance().getAlbumArtUri(playerService.getCurrentTrack().getAlbumId()),500);
            Log.v(Constants.TAG,"bittt"+b.getWidth()+"  "+b.getHeight());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
        if(b!=null) {
            mpv.setCoverBitmap(b);
        }else {
            mpv.setCoverDrawable(getResources().getDrawable(R.drawable.ic_first));
        }
        if ((playerService.getStatus() == PlayerService.PLAYING)) {
            if (!mpv.isRotating()) {
                mpv.start();
            }
        } else {
            if (mpv.isRotating()) {
                mpv.stop();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.mpv:
                if(playerService.getCurrentTrack()==null) {
                    Toast.makeText(getActivity(),"Nothing to play!",Toast.LENGTH_LONG).show();
                    return;
                }

                playerService.play();
                if ((playerService.getStatus() == PlayerService.PLAYING)) {
                    if (!mpv.isRotating()) {
                        mpv.start();
                    }
                } else {
                   if (mpv.isRotating()) {
                        mpv.stop();
                    }
                }
                ((NowPlayingActivity)getActivity()).togglePlayPauseButton();
                break;
        }
    }
}
