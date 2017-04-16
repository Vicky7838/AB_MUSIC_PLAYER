package com.music.player.bhandari.m.activity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.UIElemetHelper.ColorHelper;
import com.music.player.bhandari.m.adapter.CurrentTracklistAdapter;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.model.TrackItem;
import com.music.player.bhandari.m.qlyrics.Lyrics.offlineStorage.OfflineStorageLyrics;
import com.music.player.bhandari.m.utils.MusicLibrary;
import com.music.player.bhandari.m.utils.recyclerviewHelper.OnStartDragListener;
import com.music.player.bhandari.m.utils.recyclerviewHelper.SimpleItemTouchHelperCallback;
import com.music.player.bhandari.m.service.PlayerService;
import com.music.player.bhandari.m.utils.DBHelper.DbHelperUserMusicData;
import com.music.player.bhandari.m.utils.MyApp;
import com.music.player.bhandari.m.utils.PlaylistManager;
import com.music.player.bhandari.m.utils.UtilityFun;
import com.sackcentury.shinebuttonlib.ShineButton;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.blurry.Blurry;

/**
 * Created by amit on 11/12/16.
 */

public class NowPlayingActivity extends AppCompatActivity implements
        View.OnClickListener, SeekBar.OnSeekBarChangeListener, OnStartDragListener {

    private SeekBar seekBar_track_progress;
    private SeekBar seekBar_volume;
    private boolean isVolumeBarChangedManually = false;
    private ImageView shuffle;
    private ImageView repeat;
    private ImageView play_pause;
    private Toolbar toolbar;
    private TextView textInsideRepeat, startTime;
    private PowerManager.WakeLock mWakeLock;
    int screenWidth, screenHeight;
    ShineButton shineButton;

    //is artist thumb loaded in blurry background
    private boolean isArtistLoadedInBackground = false;

    private ViewPager viewPager;
    private NowPlayingActivity.ViewPagerAdapter viewPagerAdapter;

    AudioManager audioManager ;

    private boolean isInvokedFromFileExplorer=false;

    private  SharedPreferences pref;
    private static SlidingUpPanelLayout slidingUpPanelLayout;

    //bind player service
    private  PlayerService playerService;
    private BroadcastReceiver mUIUpdateReceiver;

    private RecyclerView mRecyclerView;
    private  CurrentTracklistAdapter mAdapter;
    NowPlayingActivity.WrapContentLinearLayoutManager mLayoutManager=
            new NowPlayingActivity.WrapContentLinearLayoutManager(this);
    private ItemTouchHelper mItemTouchHelper;


    //runnable stopper
    private  boolean stopRunnable = false;
    private  Handler mHandler = new Handler();
    /**
     * Background Runnable thread
     */
    private final Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            if (!stopRunnable) {
                startTime.setText(UtilityFun.secondsToString(playerService.getCurrentTrackProgress() / 1000));
                int totalDur = playerService.getCurrentTrackDuration();
                int curDur = playerService.getCurrentTrackProgress();
                seekBar_track_progress.setProgress(UtilityFun.getProgressPercentage(curDur / 1000, totalDur / 1000));
                //Running this thread after 100 milliseconds*/
                mHandler.postDelayed(this, 1000);
            }
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int themeSelector = MyApp.getPref().getInt(getString(R.string.pref_theme),Constants.THEME.LIGHT);
        switch (themeSelector){
            case Constants.THEME.DARK:
                setTheme(R.style.AppThemeDark);
                break;

            case Constants.THEME.GLOSSY:
                setTheme(R.style.AppThemeDark);
                break;

            case Constants.THEME.LIGHT:
                setTheme(R.style.AppThemeLight);
                break;
        }
        setContentView(R.layout.activity_now_playing);
        if(getIntent().getAction()!=null) {
            if (getIntent().getAction().equals(Constants.ACTION.OPEN_FROM_FILE_EXPLORER)) {
                isInvokedFromFileExplorer = true;
            }
        }else {
            isInvokedFromFileExplorer = false;
        }

        //prevent display from dimming
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        //startService(new Intent(this,PlayerService.class));
        //bind music service
        //Intent playerServiceIntent = new Intent(this, PlayerService.class);
        //bindService(playerServiceIntent, playerServiceConnection, Context.BIND_AUTO_CREATE);
        playerService = MyApp.getService();

        toolbar = (Toolbar) findViewById(R.id.toolbar_);

        if(playerService!=null && playerService.getCurrentTrack()!=null) {
            toolbar.setTitle(playerService.getCurrentTrack().getTitle());
            toolbar.setSubtitle(playerService.getCurrentTrack().getArtist());
        }
        setSupportActionBar(toolbar);


        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        audioManager =
                (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        //findViewById(R.id.scroll_lyric_view).setVisibility(View.GONE);
        slidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);

        //set gradient as background

        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.BR_TL,
                new int[] {ColorHelper.getAccentColor(),0xFF131313});
        gd.setCornerRadius(0f);
        slidingUpPanelLayout.setBackgroundDrawable(gd);

        slidingUpPanelLayout.setDragView(R.id.play_queue_title);
        slidingUpPanelLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {

            @Override
            public void onPanelSlide(View panel, float slideOffset) {

            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState
                    , SlidingUpPanelLayout.PanelState newState) {

                switch (newState){
                    case EXPANDED:
                        Log.v(Constants.TAG,"EXPANDED");
                        break;

                    case DRAGGING:
                        try {
                            int position = playerService.getCurrentTrackPosition();
                            mRecyclerView.scrollToPosition(position);
                        }catch (Exception ignored){}
                        //Log.v(Constants.TAG,"DRAGGING");
                        break;
                }
            }
        });

        final View view = findViewById(R.id.handle_current_queue);
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                ; //height is ready

                slidingUpPanelLayout.setPanelHeight(view.getHeight());
                slidingUpPanelLayout.setScrollableView(mRecyclerView);
            }
        });

        shineButton = (ShineButton) findViewById(R.id.shineButton);
        shineButton.init(this);
        shineButton.setOnCheckStateChangeListener(new ShineButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(View view, boolean checked) {
                Log.v(Constants.TAG,"checccc" + checked);
            }
        });

        startTime = ((TextView) findViewById(R.id.startTIme));

        ImageView next = (ImageView) findViewById(R.id.next);
        next.setOnClickListener(this);

        play_pause = (ImageView) findViewById(R.id.play_pause);
        play_pause.setOnClickListener(this);

        ImageView previous = (ImageView) findViewById(R.id.previous);
        previous.setOnClickListener(this);

        shuffle = (ImageView) findViewById(R.id.shuffle);
        shuffle.setOnClickListener(this);

        repeat = (ImageView) findViewById(R.id.repeat);
        repeat.setOnClickListener(this);

        textInsideRepeat = (TextView) findViewById(R.id.text_in_repeat);

        seekBar_volume = (SeekBar) findViewById(R.id.seekBar_volume);
        seekBar_volume.getProgressDrawable().setColorFilter(
                ColorHelper.getNowPlayingControlsColor(), PorterDuff.Mode.SRC_IN);
        seekBar_volume.setThumb(ContextCompat.getDrawable(this, R.drawable.ic_brightness_1_black_24dp));
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        Log.v(Constants.TAG,audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)+"VOLUME");
        seekBar_volume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        seekBar_volume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        seekBar_volume.setOnSeekBarChangeListener(this);


        seekBar_track_progress = (SeekBar) findViewById(R.id.seekBar_track_progress);
        seekBar_track_progress.getProgressDrawable().setColorFilter(
                ColorHelper.getNowPlayingControlsColor(), PorterDuff.Mode.SRC_IN);
        seekBar_track_progress.setThumb(ContextCompat.getDrawable(this, R.drawable.ic_brightness_1_black_24dp));
        seekBar_track_progress.setOnSeekBarChangeListener(this);
        seekBar_track_progress.setMax(100);

        mUIUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v(Constants.TAG, "update UI please Jarvis");
                UpdateUI();
            }
        };

        pref = PreferenceManager.getDefaultSharedPreferences(this);

        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");

        //current tracklist
        InitializeCurrentTracklistAdapter();

        viewPager = (ViewPager) findViewById(R.id.view_pager_now_playing);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                Log.v(Constants.L_TAG+"wow","selected "+position );
                //2 lyrics fragment
                if(position==2 && playerService.getStatus() == PlayerService.PLAYING){
                    acquireWindowPowerLock(true);
                }else {
                    acquireWindowPowerLock(false);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                Log.v(Constants.L_TAG+"wow","state changed to "+state );
            }
        });
        viewPager.setOffscreenPageLimit(2);
        setupViewPager(viewPager);
        //set cuurent item to disc
        viewPager.setCurrentItem(MyApp.getPref()
                .getInt(getString(R.string.pref_exit_now_playing_at),Constants.EXIT_NOW_PLAYING_AT.DISC_FRAG), true);

        //display current play queue header
        if(!MyApp.getService().getTrackList().isEmpty()){
            //String title = "PLAY QUEUE (" + MyApp.getService().getTrackList().size() + " tracks)";
            String title = "PLAY QUEUE";
            ((TextView)findViewById(R.id.current_track_list_header)).setText(title);
        }

        if(!MyApp.getPref().getBoolean(getString(R.string.pref_swipe_right_shown),false)) {
            showInfoDialog();
        }
    }

    private void acquireWindowPowerLock(boolean acquire){
        if(acquire) {
            if (mWakeLock != null && !mWakeLock.isHeld()) {
                this.mWakeLock.acquire();
            }
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }else {
            if(mWakeLock!=null && mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void showInfoDialog(){
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Swipe right to look up artist Bio!");

        alert.setNegativeButton("Got it!", null);

        alert.setPositiveButton("Never show again", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                MyApp.getPref().edit().putBoolean(getString(R.string.pref_swipe_right_shown),true).apply();
            }
        });

        alert.show();
    }

    private void setupViewPager(ViewPager viewPager) {

        viewPagerAdapter = new NowPlayingActivity.ViewPagerAdapter(getSupportFragmentManager());

        FragmentArtistInfo artistInfo = new FragmentArtistInfo();
        viewPagerAdapter.addFragment(artistInfo,"Artist Bio");

        FragmentDisc fragmentDisc=new FragmentDisc();
        viewPagerAdapter.addFragment(fragmentDisc, "Disc");

        FragmentLyrics fragmentLyric=new FragmentLyrics();
        viewPagerAdapter.addFragment(fragmentLyric, "Lyrics");

        viewPager.setAdapter(viewPagerAdapter);
    }

    private void InitializeCurrentTracklistAdapter(){
        mRecyclerView=(RecyclerView)findViewById(R.id.recyclerViewForCurrentTracklist);
        mAdapter= new CurrentTracklistAdapter(this,this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(mLayoutManager);
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
    }

    @Override
    protected void onStop() {
        mAdapter.clear();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.v(Constants.TAG, "DESTORY NOW PLAYING");
        //this removes any memory leak caused by handler
        mHandler.removeCallbacksAndMessages(null);

        //save exit status so than we can open corresponding frag next time
        switch (viewPager.getCurrentItem()){
            case 2:
                MyApp.getPref().edit()
                        .putInt(getString(R.string.pref_exit_now_playing_at),Constants.EXIT_NOW_PLAYING_AT.LYRICS_FRAG).apply();
                break;

            case 0:
                MyApp.getPref().edit()
                        .putInt(getString(R.string.pref_exit_now_playing_at),Constants.EXIT_NOW_PLAYING_AT.ARTIST_FRAG).apply();
                break;

            case 1:
            default:
                MyApp.getPref().edit()
                        .putInt(getString(R.string.pref_exit_now_playing_at),Constants.EXIT_NOW_PLAYING_AT.DISC_FRAG).apply();
                break;
        }

        if(mWakeLock!=null && mWakeLock.isHeld()){
            mWakeLock.release();
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onDestroy();
    }

    private void UpdateUI() {
        if(playerService!=null) {
            TrackItem item = playerService.getCurrentTrack();
            mAdapter.notifyDataSetChanged();
            invalidateOptionsMenu();

            if (item != null) {
                //update lyrics and info
                Intent intent = new Intent().setAction(Constants.ACTION.UPDATE_LYRIC_AND_INFO);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

                //update disc
                updateDisc();

                if(playerService.getStatus()==PlayerService.PLAYING){
                    acquireWindowPowerLock(true);
                }else {
                    acquireWindowPowerLock(false);
                }

                Bitmap b=null ;// = playerService.getAlbumArt();
                try {
                    //look in cache for artist image
                    String CACHE_ART_THUMBS = this.getCacheDir()+"/art_thumbs/";
                    String actual_file_path = CACHE_ART_THUMBS+playerService.getCurrentTrack().getArtist();
                    b= BitmapFactory.decodeFile(actual_file_path);
                    if(b==null) {
                        isArtistLoadedInBackground=false;
                        b = UtilityFun.decodeUri(this, MusicLibrary.getInstance()
                                .getAlbumArtUri(playerService.getCurrentTrack().getAlbumId()), 500);
                    }else {
                        isArtistLoadedInBackground=true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(b!=null) {
                    int width = b.getWidth();
                    int height = b.getHeight();
                    int maxWidth = screenWidth;
                    int maxHeight = screenHeight;
                    if (width > height) {
                        // landscape
                        float ratio = (float) width / maxWidth;
                        width = maxWidth;
                        height = (int)(height / ratio);
                    } else if (height > width) {
                        // portrait
                        float ratio = (float) height / maxHeight;
                        height = maxHeight;
                        width = (int)(width / ratio);
                    } else {
                        // square
                        if(maxHeight<height) {
                            height = maxHeight;
                            width = maxWidth;
                        }
                    }

                    b = Bitmap.createScaledBitmap(b, width, height, false);
                    setBlurryBackground(b);
                }else {
                    b = BitmapFactory.decodeResource(getResources(),R.drawable.ic_first);
                    setBlurryBackground(b);
                }

                toolbar.setTitle(playerService.getCurrentTrack().getTitle());
                toolbar.setSubtitle(playerService.getCurrentTrack().getArtist());

                ((TextView) findViewById(R.id.endTime)).setText(UtilityFun.secondsToString(item.getDurInt() / 1000));
                int totalDur = playerService.getCurrentTrackDuration();
                int curDur = playerService.getCurrentTrackProgress();

                //setting this 0 freees seekbar on launch, so make sure minimum 1 is set
                seekBar_track_progress
                        .setProgress(Math.max(1,UtilityFun.getProgressPercentage(curDur / 1000, totalDur / 1000)));
                Log.v(Constants.TAG, "updated UI " + System.currentTimeMillis());

                if ((playerService.getStatus() == PlayerService.PLAYING)) {
                    play_pause.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_pause_black_24dp));
                } else {
                    play_pause.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_play_arrow_black_24dp));
                }

                if (pref.getBoolean(Constants.PREFERENCES.SHUFFLE, false)) {
                    shuffle.setColorFilter(ColorHelper.getNowPlayingControlsColor());
                } else {
                    shuffle.setColorFilter(ColorHelper.getColor(R.color.colorwhite));
                }

                if (pref.getInt(Constants.PREFERENCES.REPEAT, 0) == Constants.PREFERENCE_VALUES.REPEAT_ALL) {
                    //repeat.setColorFilter(UtilityFun.GetDominatColor(playerService.getAlbumArt()));
                    //textInsideRepeat.setTextColor(UtilityFun.GetDominatColor(playerService.getAlbumArt()));
                    textInsideRepeat.setTextColor(ColorHelper.getNowPlayingControlsColor());
                    repeat.setColorFilter(ColorHelper.getNowPlayingControlsColor());
                    textInsideRepeat.setText("A");
                } else if (pref.getInt(Constants.PREFERENCES.REPEAT, 0) == Constants.PREFERENCE_VALUES.REPEAT_ONE) {
                    //repeat.setColorFilter(UtilityFun.GetDominatColor(playerService.getAlbumArt()));
                    //textInsideRepeat.setTextColor(UtilityFun.GetDominatColor(playerService.getAlbumArt()));
                    repeat.setColorFilter(ColorHelper.getNowPlayingControlsColor());
                    textInsideRepeat.setTextColor(ColorHelper.getNowPlayingControlsColor());
                    textInsideRepeat.setText("1");
                } else if (pref.getInt(Constants.PREFERENCES.REPEAT, 0) == Constants.PREFERENCE_VALUES.NO_REPEAT) {
                    repeat.setColorFilter(ColorHelper.getColor(R.color.colorwhite));
                    textInsideRepeat.setTextColor(ColorHelper.getColor(R.color.colorwhite));
                    textInsideRepeat.setText("");
                }

//                play_pause.setColorFilter(ColorHelper.getAccentColor());


                //mHandler.post(getDominantColorRunnable());
            }
        }
        else {
                //this should not happen
                //restart app
                Intent mStartActivity = new Intent(this, MainActivity.class);
                int mPendingIntentId = 123456;
                PendingIntent mPendingIntent = PendingIntent.getActivity(this, mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                System.exit(0);
        }

    }

    public void setBlurryBackground(Bitmap b){
        Animation fadeIn = AnimationUtils.loadAnimation(NowPlayingActivity.this, R.anim.fade_in);
        fadeIn.setDuration(2000);
        findViewById(R.id.full_screen_iv).startAnimation(fadeIn);

        Blurry.with(this).radius(1).color(Color.argb(90
                , 0, 0, 0)).from(b).into(((ImageView) findViewById(R.id.full_screen_iv)));
    }

    @Override
    protected void onPause() {
        MyApp.isAppVisible = false;
        Log.v(Constants.TAG,"PAUSE NOW PLAYING");
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mUIUpdateReceiver);
        stopRunnable=true;
       /* if(mpv.isRotating()) {
            mpv.stop();
        }*/
        super.onPause();
    }

    @Override
    protected void onResume() {
        MyApp.isAppVisible = true;
        super.onResume();
        if(playerService!=null)
            UpdateUI();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mUIUpdateReceiver
                ,new IntentFilter(Constants.ACTION.UI_UPDATE));
        stopRunnable=false;
        /*if(!mpv.isRotating()){
            if(playerService!=null && playerService.getStatus()==PlayerService.PLAYING){
                mpv.start();
            }
        }*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_now_plying, menu);
        for(int i = 0; i < menu.size(); i++){
            if(menu.getItem(i).getItemId()==R.id.action_fav) {
                Drawable drawable = menu.getItem(i).getIcon();
                if (drawable != null) {
                    TrackItem item=playerService.getCurrentTrack();
                    if(item!=null && PlaylistManager.getInstance(getApplicationContext()).isFav(item.getTitle())) {
                        drawable.mutate();
                        drawable.setColorFilter(ColorHelper.getNowPlayingControlsColor(), PorterDuff.Mode.SRC_ATOP);
                    }else {
                        drawable.mutate();
                        drawable.setColorFilter(ColorHelper.getColor(R.color.colorwhite), PorterDuff.Mode.SRC_ATOP);
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        //
        if(slidingUpPanelLayout.getPanelState()== SlidingUpPanelLayout.PanelState.EXPANDED){
            slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            return;
        }

        /*if(mpv!=null){
            mpv.removeHandlerCallback();
        }*/

        if(isInvokedFromFileExplorer){
            finish();
            return;
        }

        if(isTaskRoot()){
            startActivity(new Intent(this,MainActivity.class));
            overridePendingTransition(android.R.anim.slide_in_left,android.R.anim.slide_out_right);
            //overridePendingTransition( R.anim.slide_down,R.anim.no_change);
        }
        super.onBackPressed();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        boolean b = intent.getBooleanExtra("refresh",false);
        if(b){
            int position = intent.getIntExtra("position", -1);
            String title = intent.getStringExtra("title");
            String artist = intent.getStringExtra("artist");
            String album = intent.getStringExtra("album");

            if(playerService!=null) {
                playerService.updateTrackItem(position,title, artist, album);
                playerService.PostNotification(false);

                //no need to update here as onResume will update UI automatically
                //UpdateUI();

                //update currenttracklistadapteritem
                mAdapter.updateItem(position, title, artist, album);
            }
        }



    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        TrackItem trackItem = playerService.getCurrentTrack();
        switch (item.getItemId()){
            case R.id.action_fav:
                if(playerService.getCurrentTrack()==null) {
                    Toast.makeText(this,"Nothing to set fav!",Toast.LENGTH_LONG).show();
                    return true;
                }
                if(PlaylistManager.getInstance(getApplicationContext()).isFav(playerService.getCurrentTrack().getTitle())){
                    PlaylistManager.getInstance(getApplicationContext()).RemoveFromFav(playerService.getCurrentTrack().getTitle());
                }else {
                    String []titles = new String[]{playerService.getCurrentTrack().getTitle()};
                    PlaylistManager.getInstance(getApplicationContext())
                            .AddSongToPlaylist(DbHelperUserMusicData.KEY_FAV,titles);
                    shineButton.setVisibility(View.VISIBLE);
                    shineButton.showAnim();
                    shineButton.clearAnimation();
                 //   shineButton.setVisibility(View.INVISIBLE);
                }
                invalidateOptionsMenu();
                break;

            case R.id.action_equ:
                Intent intent = new Intent(AudioEffect
                        .ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);

                if((intent.resolveActivity(getPackageManager()) != null)) {
                    try {
                        startActivityForResult(intent, 0);
                    }catch (Exception ignored){

                    }
                } else {
                    // No equalizer found :(
                    Toast.makeText(this,"No equalizer application found in device. In built equalizer is coming soon!", Toast.LENGTH_LONG).show();
                }
                break;

            case android.R.id.home:
                startActivity(new Intent(this,MainActivity.class));
                overridePendingTransition(android.R.anim.slide_in_left,android.R.anim.slide_out_right);
                finish();
                break;

            case R.id.action_settings:
                finish();
                startActivity(new Intent(this,SettingsActivity.class)
                        .putExtra("launchedFrom",Constants.PREF_LAUNCHED_FROM.NOW_PLAYING));
                break;

            case R.id.action_go_to_artist:
                if(trackItem!=null) {
                    Intent art_intent = new Intent(this, SecondaryLibraryActivity.class);
                    art_intent.putExtra("status", Constants.FRAGMENT_STATUS.ARTIST_FRAGMENT);
                    art_intent.putExtra("key",trackItem.getArtist_id());
                    art_intent.putExtra("title", trackItem.getArtist());
                    startActivity(art_intent);
                }
                break;

            case R.id.action_go_to_album:
                if(trackItem!=null) {
                    Intent alb_intent = new Intent(this, SecondaryLibraryActivity.class);
                    alb_intent.putExtra("status", Constants.FRAGMENT_STATUS.ALBUM_FRAGMENT);
                    alb_intent.putExtra("key", trackItem.getAlbumId());
                    alb_intent.putExtra("title", trackItem.getAlbum());
                    startActivity(alb_intent);
                }
                break;

            case R.id.action_share:
                if(trackItem!=null) {
                    File fileToBeShared = new File(trackItem.getFilePath());
                    ArrayList<Uri> files = new ArrayList<>();
                    files.add(Uri.fromFile(fileToBeShared));
                    UtilityFun.Share(this, files, trackItem.getTitle() );
                }else {
                    Toast.makeText(this,"Nothing to share!",Toast.LENGTH_LONG).show();
                }
                break;

            case R.id.action_add_to_playlist:
                //Toast.makeText(context,"Playlists coming soon" ,Toast.LENGTH_SHORT).show();
                if(trackItem!=null) {
                    AddToPlaylist();
                }
                break;

            case R.id.action_sleep_timer:
                    setSleepTimerDialog(this);
                break;

            case R.id.action_edit_track_info:
                if(trackItem!=null) {
                    startActivity(new Intent(this, ActivityTagEditor.class)
                            .putExtra("from", Constants.TAG_EDITOR_LAUNCHED_FROM.NOW_PLAYING)
                            .putExtra("file_path", trackItem.getFilePath())
                            .putExtra("track_title", trackItem.getTitle())
                            .putExtra("position", MyApp.getService().getCurrentTrackPosition()));
                }
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;


            case R.id.action_clear_lyrics_offline:
                if(trackItem!=null){
                    if(OfflineStorageLyrics.clearLyrics(trackItem)){
                        ((FragmentLyrics)viewPagerAdapter.getItem(2)).clearLyrics();
                    }else {
                        Toast.makeText(this, "Unable to delete lyrics!", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void AddToPlaylist(){
        String[] song_titles;
        TrackItem trackItem = playerService.getCurrentTrack();
        song_titles=new String[]{trackItem.getTitle()};
        UtilityFun.AddToPlaylist(this, song_titles);
        invalidateOptionsMenu();
    }

    private long mLastClickTime = 0;

    @Override
    public void onClick(View view) {

        switch (view.getId()){
            case R.id.play_pause:
                ColorSwitchRunnableForImageView colorSwitchPlayPause = new ColorSwitchRunnableForImageView((ImageView) view);
                mHandler.post(colorSwitchPlayPause);

                if(playerService.getCurrentTrack()==null) {
                    Toast.makeText(this,"Nothing to play!",Toast.LENGTH_LONG).show();
                    return;
                }
                playerService.play();
                togglePlayPauseButton();
                updateDisc();

                if(playerService.getStatus()==PlayerService.PLAYING){
                    acquireWindowPowerLock(true);
                }else {
                    acquireWindowPowerLock(false);
                }
                break;

            case R.id.next:
                ColorSwitchRunnableForImageView colorSwitchRunnableNext = new ColorSwitchRunnableForImageView((ImageView) view);
                mHandler.post(colorSwitchRunnableNext);
                if(playerService.getCurrentTrack()==null) {
                    Toast.makeText(this,"Nothing to play!",Toast.LENGTH_LONG).show();
                    return;
                }
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                playerService.nextTrack();
                UpdateUI();
                break;

            case R.id.previous:
                ColorSwitchRunnableForImageView colorSwitchRunnablePrevious = new ColorSwitchRunnableForImageView((ImageView) view);
                mHandler.post(colorSwitchRunnablePrevious);
                if(playerService.getCurrentTrack()==null) {
                    Toast.makeText(this,"Nothing to play!",Toast.LENGTH_LONG).show();
                    return;
                }
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                playerService.prevTrack();
                UpdateUI();
                break;


            case R.id.shuffle:
                if(playerService.getCurrentTrack()==null) {
                    Toast.makeText(this,"Nothing to play!",Toast.LENGTH_LONG).show();
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                if(pref.getBoolean(Constants.PREFERENCES.SHUFFLE,false)){
                    //shuffle is on, turn it off
                    //Toast.makeText(this, "shuffle off", Toast.LENGTH_SHORT).show();
                    pref.edit().putBoolean(Constants.PREFERENCES.SHUFFLE,false).apply();
                    playerService.shuffle(false);
                    InitializeCurrentTracklistAdapter();
                    shuffle.setColorFilter(ColorHelper.getColor(R.color.colorwhite));
                }else {
                    //shuffle is off, turn it on
                    pref.edit().putBoolean(Constants.PREFERENCES.SHUFFLE,true).apply();
                    playerService.shuffle(true);
                    InitializeCurrentTracklistAdapter();
                    shuffle.setColorFilter(ColorHelper.getNowPlayingControlsColor());
                }
                mRecyclerView.getAdapter().notifyDataSetChanged();
                break;

            case R.id.repeat:
                if(pref.getInt(Constants.PREFERENCES.REPEAT,0)==Constants.PREFERENCE_VALUES.NO_REPEAT){
                    pref.edit().putInt(Constants.PREFERENCES.REPEAT,Constants.PREFERENCE_VALUES.REPEAT_ALL).apply();
                    //repeat.setColorFilter(UtilityFun.GetDominatColor(playerService.getAlbumArt()));
                    textInsideRepeat.setTextColor(ColorHelper.getNowPlayingControlsColor());
                    repeat.setColorFilter(ColorHelper.getNowPlayingControlsColor());
                    textInsideRepeat.setText("A");
                }else if(pref.getInt(Constants.PREFERENCES.REPEAT,0)==Constants.PREFERENCE_VALUES.REPEAT_ALL){
                    pref.edit().putInt(Constants.PREFERENCES.REPEAT,Constants.PREFERENCE_VALUES.REPEAT_ONE).apply();
                    textInsideRepeat.setTextColor(ColorHelper.getNowPlayingControlsColor());
                    repeat.setColorFilter(ColorHelper.getNowPlayingControlsColor());
                    textInsideRepeat.setText("1");
                }else if(pref.getInt(Constants.PREFERENCES.REPEAT,0)==Constants.PREFERENCE_VALUES.REPEAT_ONE){
                    pref.edit().putInt(Constants.PREFERENCES.REPEAT,Constants.PREFERENCE_VALUES.NO_REPEAT).apply();
                    repeat.setColorFilter(ColorHelper.getColor(R.color.colorwhite));
                    textInsideRepeat.setTextColor(ColorHelper.getColor(R.color.colorwhite));
                    textInsideRepeat.setText("");
                }
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        switch (seekBar.getId()){
            case R.id.seekBar_track_progress:
                mHandler.removeCallbacks(mUpdateTimeTask);
                if(b){
                    ((TextView)findViewById(R.id.startTIme)).setText(UtilityFun.secondsToString((int) (((double)i/100)*playerService.getCurrentTrackDuration()/1000)));
                }
                mHandler.postDelayed(mUpdateTimeTask, 100);
                break;

            case R.id.seekBar_volume:
                Log.v(Constants.TAG,seekBar.getProgress()+"");
                //this flag indicated if vl=olume bar is changed manually
                //set volume only when touched manually
                //Aavoid changing volume when volume is getting changed from hardware buttons
                if(isVolumeBarChangedManually) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, seekBar.getProgress(), 0);
                }
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        switch (seekBar.getId()){
            case R.id.seekBar_track_progress:
                mHandler.removeCallbacks(mUpdateTimeTask);
                stopRunnable=true;
                break;

            case R.id.seekBar_volume:
                isVolumeBarChangedManually = true;
                break;
        }

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        switch (seekBar.getId()){
            case R.id.seekBar_track_progress:
                mHandler.removeCallbacks(mUpdateTimeTask);
                int totalDuration = playerService.getCurrentTrackDuration();
                int currentDuration = UtilityFun.progressToTimer(seekBar.getProgress(), totalDuration);

                // forward or backward to certain seconds
                playerService.seekTrack(currentDuration);
                ((FragmentLyrics)viewPagerAdapter.getItem(2)).seekLyrics(currentDuration);
                mHandler.postDelayed(mUpdateTimeTask, 100);
                stopRunnable=false;
                break;

            case R.id.seekBar_volume:
                isVolumeBarChangedManually = false;
                break;
        }

    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //super.onKeyDown(keyCode,event);
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                playerService.play();
                updateDisc();
                togglePlayPauseButton();
                break;

            case KeyEvent.KEYCODE_MEDIA_NEXT:
                playerService.nextTrack();
                UpdateUI();
                break;

            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                playerService.prevTrack();
                UpdateUI();
                break;

            case KeyEvent.KEYCODE_MEDIA_STOP:
                playerService.stop();
                UpdateUI();
                break;

            case KeyEvent.KEYCODE_BACK:
                onBackPressed();
                break;

            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                super.onKeyDown(keyCode,event);
                Log.v(Constants.TAG,keyCode + " v " + audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)  );
                seekBar_volume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                break;
        }

        return false;
    }

    private void updateDisc(){
        LocalBroadcastManager.getInstance(getApplicationContext())
                .sendBroadcast(new Intent(Constants.ACTION.DISC_UPDATE));
        ((FragmentLyrics)viewPagerAdapter.getItem(2)).runLyricThread();
    }

    public void togglePlayPauseButton(){
        if ((playerService.getStatus() == PlayerService.PLAYING)) {
            play_pause.setImageDrawable(ContextCompat.getDrawable(NowPlayingActivity.this, R.drawable.ic_pause_black_24dp));
        } else {
            play_pause.setImageDrawable(ContextCompat.getDrawable(NowPlayingActivity.this, R.drawable.ic_play_arrow_black_24dp));
        }
    }

    public boolean isArtistLoadedInBack(){
        return isArtistLoadedInBackground;
    }

    public static void setSleepTimerDialog(final Context context){
        final AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(context.getString(R.string.title_sleep_timer));
        LinearLayout linear = new LinearLayout(context);
        linear.setOrientation(LinearLayout.VERTICAL);
        final TextView text = new TextView(context);
        int timer = MyApp.getPref().getInt(context.getString(R.string.pref_sleep_timer),0);
        if(timer==0) {
            text.setText("0 minutes");
        }else {
            text.setText("Timer set for "+timer+" minutes already");
            alert.setNeutralButton("Discard timer",new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    MyApp.getPref().edit().putInt(context.getString(R.string.pref_sleep_timer),0).apply();
                    MyApp.getService().setSleepTimer(0, false);
                    Toast.makeText(context, "Sleep timer discarded", Toast.LENGTH_LONG).show();
                }
            });
        }
        text.setPadding(0, 10,0,0);
        text.setGravity(Gravity.CENTER);

        final SeekBar seek = new SeekBar(context);
        seek.setPadding(40,10,40,10);
        seek.setMax(100);
        seek.setProgress(0);

        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                text.setText(progress+" minutes");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
            }
        });

        linear.addView(seek);
        linear.addView(text);
        alert.setView(linear);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //RescanLibrary();
                if(seek.getProgress()!=0) {
                    MyApp.getPref().edit().putInt(context.getString(R.string.pref_sleep_timer),seek.getProgress()).apply();
                    MyApp.getService().setSleepTimer(seek.getProgress(), true);
                    String temp = "Music will be stopped in " + seek.getProgress() + " minutes";
                    Toast.makeText(context, temp, Toast.LENGTH_LONG).show();
                }
            }
        });


        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });

        alert.show();
    }

    //for catching exception generated by recycler view which was causing abend, no other way to handle this
    private class WrapContentLinearLayoutManager extends LinearLayoutManager {
        public WrapContentLinearLayoutManager(Context context) {
            super(context);
        }

        //... constructor
        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                super.onLayoutChildren(recycler, state);
            } catch (IndexOutOfBoundsException e) {
                Log.e("probe", "meet a IOOBE in RecyclerView");
            }
        }
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    private class ColorSwitchRunnableForImageView implements Runnable {

        ImageView v;
        boolean colorChanged = false;

        public ColorSwitchRunnableForImageView(ImageView v){
            this.v = v;
        }
        @Override
        public void run() {
            if(!colorChanged) {
                v.setColorFilter(ColorHelper.getNowPlayingControlsColor());
                colorChanged=true;
                mHandler.postDelayed(this,200);
            }else {
                v.setColorFilter(ColorHelper.getColor(R.color.colorwhite));
                colorChanged = false;
            }
        }
    }
}
