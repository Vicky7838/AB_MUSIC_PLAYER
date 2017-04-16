package com.music.player.bhandari.m.service;

/**
 * Created by amit on 20/11/16.
 */

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.UIElemetHelper.ColorHelper;
import com.music.player.bhandari.m.activity.MainActivity;
import com.music.player.bhandari.m.activity.NowPlayingActivity;
import com.music.player.bhandari.m.activity.PermissionSeekActivity;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.utils.MusicLibrary;
import com.music.player.bhandari.m.model.TrackItem;
import com.music.player.bhandari.m.utils.MyApp;
import com.music.player.bhandari.m.utils.PlaylistManager;
import com.music.player.bhandari.m.utils.UtilityFun;
import com.music.player.bhandari.m.widget.WidgetReceiver;
import com.squareup.seismic.ShakeDetector;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.Executors;

import static android.content.ContentValues.TAG;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class PlayerService extends Service implements
        AudioManager.OnAudioFocusChangeListener, ShakeDetector.Listener {

    static public final int STOPPED = -1, PAUSED = 0, PLAYING = 1;
    private AudioManager mAudioManager;
    private MediaPlayer mediaPlayer;

    //playlist (now playing)
    private ArrayList<String> trackList =new ArrayList<>();
    private TrackItem currentTrack;
    private int currentVolume = 0 ;
    private boolean fVolumeIsBeingChanged = false;

    private IntentFilter headsetFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
    private HeadSetReceiver mReceiverHeadset=new HeadSetReceiver();

    private int status, currentTrackPosition;
    private IBinder playerBinder;

    private PendingIntent pendingIntent;
    private PendingIntent pSwipeToDismiss;
    private PendingIntent ppreviousIntent,pplayIntent,pnextIntent,pdismissIntent;
    private NotificationManager mNotificationManager;

    //media session and related objects
    private MediaSession mMediaSession;
    private PlaybackState.Builder stateBuilder;

    private PhoneStateListener phoneStateListener;
    private BroadcastReceiver mReceiver;
    private static ShakeDetector shakeDetector;
    private static SensorManager sensorManager;

    private boolean musicPausedBecauseOfCall, musicPuasedBecauseOfFocusLoss;

    private Handler mHandler;

    @Override
    public void onCreate() {
        Log.v(Constants.TAG,"SERVICE ON CREATE");
        super.onCreate();
        mHandler = new Handler();

        //for shake to play feature
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        shakeDetector = new ShakeDetector(this);

        if(MyApp.getPref().getBoolean(getString(R.string.pref_shake),false)){
            setShakeListener(true);
        }

        InitializeIntents();
        InitializeReceiver();

        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            //bluetooth button control and lock screen albumName art
            InitializeMediaSession();
        }

        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //initialize stuff  ///to broadcast to UI when track changes automatically
        mAudioManager=(AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer arg0) {
                if (currentTrackPosition == trackList.size()-1) {
                    if(MyApp.getPref().getInt(Constants.PREFERENCES.REPEAT,0)==Constants.PREFERENCE_VALUES.REPEAT_ALL){
                        playTrack(0);
                        currentTrackPosition=0;
                    } else if(MyApp.getPref().getInt(Constants.PREFERENCES.REPEAT,0)==Constants.PREFERENCE_VALUES.REPEAT_ONE) {
                        playTrack(currentTrackPosition);
                    } else {
                        stop();
                        PostNotification(false);
                    }
                }else if(MyApp.getPref().getInt(Constants.PREFERENCES.REPEAT,0)==Constants.PREFERENCE_VALUES.REPEAT_ONE) {
                    playTrack(currentTrackPosition);
                }else {
                    nextTrack();
                }
                notifyUI();
            }
        });

        currentTrackPosition = -1;
        setStatus(STOPPED);
        playerBinder = new PlayerBinder();

        restoreTracklist();
        this.registerReceiver(mReceiverHeadset,headsetFilter);

        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (state == TelephonyManager.CALL_STATE_RINGING) {
                    //Incoming call: Pause music
                    Log.v(Constants.TAG,"Ringing");
                    if(status==PLAYING) {
                        pause();
                        notifyUI();
                        musicPausedBecauseOfCall = true;
                    }
                } else if(state == TelephonyManager.CALL_STATE_IDLE) {
                    //Not in call: Play music
                    Log.v(Constants.TAG,"Idle");
                    if(musicPausedBecauseOfCall){
                        play();
                        notifyUI();
                        musicPausedBecauseOfCall=false;
                    }
                } else if(state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    //A call is dialing, active or on hold
                    Log.v(Constants.TAG,"Dialling");
                    if(status==PLAYING) {
                        pause();
                        notifyUI();
                        musicPausedBecauseOfCall=true;
                    }
                }
                super.onCallStateChanged(state, incomingNumber);
            }
        };
        TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if(mgr != null) {
            mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(MyApp.getService()==null){
            MyApp.setService(this);
        }
        if(intent!=null && intent.getAction()!=null) {
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
        return START_STICKY;
    }

    private void InitializeReceiver(){
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()){
                    case Constants.ACTION.PLAY_PAUSE_ACTION:
                        if (status == PLAYING) {
                            pause();
                        } else {
                            play();
                        }
                        //PostNotification(false);
                        notifyUI();
                        break;

                    case Constants.ACTION.PREV_ACTION:
                        prevTrack();
                        notifyUI();
                        //PostNotification(true);
                        break;

                    case Constants.ACTION.NEXT_ACTION:
                        nextTrack();
                        notifyUI();
                        //PostNotification(true);
                        break;

                    case Constants.ACTION.DISMISS_EVENT:
                        if(getStatus()==PLAYING){
                            mediaPlayer.pause();
                            setStatus(PAUSED);
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                PlaybackState state = new PlaybackState.Builder()
                                        .setActions(
                                                PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_PAUSE |
                                                        PlaybackState.ACTION_PLAY_FROM_MEDIA_ID | PlaybackState.ACTION_PAUSE |
                                                        PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                                        .setState(PlaybackState.STATE_PAUSED, 0, 1)
                                        .build();
                                mMediaSession.setPlaybackState(state);
                            }
                        }
                        stopForeground(true);
                        mNotificationManager.cancelAll();
                        notifyUI();
                        setShakeListener(false);
                        //stopSelf();
                        break;

                    case Constants.ACTION.SWIPE_TO_DISMISS:
                        setShakeListener(false);
                        break;

                    case Constants.ACTION.REFRESH_LIB:
                        if(currentTrack==null || trackList.isEmpty()){
                            trackList.clear();
                            trackList.addAll(MusicLibrary.getInstance().getDefaultTracklist());
                            if(!trackList.isEmpty()) {
                                int random = new Random().nextInt(trackList.size()-1);
                                currentTrack = MusicLibrary.getInstance().getTrackItemFromTitle(trackList.get(random));
                                currentTrackPosition = random;
                            }else {
                                currentTrackPosition = -1;
                                Toast.makeText(getApplicationContext(),"Empty library!",Toast.LENGTH_LONG).show();
                            }
                            notifyUI();
                        }
                        break;

                    case Constants.ACTION.WIDGET_UPDATE:
                        updateWidget(true);
                        break;

                    case Constants.ACTION.LAUNCH_PLAYER_FROM_WIDGET:
                        Log.v(Constants.TAG,"Luaanch now playing from service");
                        //permission seek activity is used here to show splash screen
                        startActivity(new Intent(getApplicationContext(),PermissionSeekActivity.class).addFlags(FLAG_ACTIVITY_NEW_TASK));
                        break;

                    case Constants.ACTION.SHUFFLE_WIDGET:
                        if(MyApp.getPref().getBoolean(Constants.PREFERENCES.SHUFFLE,false)){
                            //shuffle is on, turn it off
                            //Toast.makeText(this, "shuffle off", Toast.LENGTH_SHORT).show();
                            MyApp.getPref().edit().putBoolean(Constants.PREFERENCES.SHUFFLE,false).apply();
                            shuffle(false);
                        }else {
                            //shuffle is off, turn it on
                            MyApp.getPref().edit().putBoolean(Constants.PREFERENCES.SHUFFLE,true).apply();
                            shuffle(true);
                        }
                        updateWidget(false);
                        break;

                    case Constants.ACTION.REPEAT_WIDGET:
                        SharedPreferences pref = MyApp.getPref();
                        if(pref.getInt(Constants.PREFERENCES.REPEAT,0)==Constants.PREFERENCE_VALUES.NO_REPEAT){
                            pref.edit().putInt(Constants.PREFERENCES.REPEAT,Constants.PREFERENCE_VALUES.REPEAT_ALL).apply();
                        }else if(pref.getInt(Constants.PREFERENCES.REPEAT,0)==Constants.PREFERENCE_VALUES.REPEAT_ALL){
                            pref.edit().putInt(Constants.PREFERENCES.REPEAT,Constants.PREFERENCE_VALUES.REPEAT_ONE).apply();
                        }else if(pref.getInt(Constants.PREFERENCES.REPEAT,0)==Constants.PREFERENCE_VALUES.REPEAT_ONE){
                            pref.edit().putInt(Constants.PREFERENCES.REPEAT,Constants.PREFERENCE_VALUES.NO_REPEAT).apply();
                        }
                        updateWidget(false);
                        break;
                }
            }
        };
        IntentFilter intentFilter =  new IntentFilter();

        intentFilter.addAction(Constants.ACTION.PLAY_PAUSE_ACTION);
        intentFilter.addAction(Constants.ACTION.PREV_ACTION);
        intentFilter.addAction(Constants.ACTION.NEXT_ACTION);
        intentFilter.addAction(Constants.ACTION.DISMISS_EVENT);
        intentFilter.addAction(Constants.ACTION.SWIPE_TO_DISMISS);
        intentFilter.addAction(Constants.ACTION.REFRESH_LIB);
        intentFilter.addAction(Constants.ACTION.LAUNCH_PLAYER_FROM_WIDGET);
        intentFilter.addAction(Constants.ACTION.WIDGET_UPDATE);
        intentFilter.addAction(Constants.ACTION.SHUFFLE_WIDGET);
        intentFilter.addAction(Constants.ACTION.REPEAT_WIDGET);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver, intentFilter);
    }

    private void InitializeIntents(){
        //Notification intents
        Intent notificationIntent;
        if(MyApp.getPref().getInt(getString(R.string.pref_click_on_notif)
                , Constants.CLICK_ON_NOTIF.OPEN_LIBRARY_VIEW) == Constants.CLICK_ON_NOTIF.OPEN_LIBRARY_VIEW){
            notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);
        }else if(MyApp.getPref().getInt(getString(R.string.pref_click_on_notif)
                , Constants.CLICK_ON_NOTIF.OPEN_LIBRARY_VIEW) == Constants.CLICK_ON_NOTIF.OPEN_DISC_VIEW){
            notificationIntent = new Intent(this, NowPlayingActivity.class);
            notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);
        }

        Intent previousIntent = new Intent(this, PlayerService.class);
        previousIntent.setAction(Constants.ACTION.PREV_ACTION);
        ppreviousIntent = PendingIntent.getService(this, 0,
                previousIntent, 0);

        Intent playIntent = new Intent(this, PlayerService.class);
        playIntent.setAction(Constants.ACTION.PLAY_PAUSE_ACTION);
        pplayIntent = PendingIntent.getService(this, 0,
                playIntent, 0);

        Intent nextIntent = new Intent(this, PlayerService.class);
        nextIntent.setAction(Constants.ACTION.NEXT_ACTION);
        pnextIntent = PendingIntent.getService(this, 0,
                nextIntent, 0);

        Intent dismissIntent = new Intent(this, PlayerService.class);
        dismissIntent.setAction(Constants.ACTION.DISMISS_EVENT);
        pdismissIntent = PendingIntent.getService(this, 0,
                dismissIntent, 0);

        Intent swipeToDismissIntent = new Intent(this, PlayerService.class);
        swipeToDismissIntent.setAction(Constants.ACTION.SWIPE_TO_DISMISS);
        pSwipeToDismiss = PendingIntent.getService(this, 0, swipeToDismissIntent, 0);

    }

    @TargetApi(21)
    private void InitializeMediaSession(){
        mMediaSession = new MediaSession(getApplicationContext(), getPackageName() + "." + TAG);
        mMediaSession.setCallback(new MediaSession.Callback()   {
            public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
                Log.d(TAG, "onMediaButtonEvent called: " + mediaButtonIntent);
                return super.onMediaButtonEvent(mediaButtonIntent);
            }

            public void onPause() {
                Log.d(TAG, "onPause called (media button pressed)");
                if(!MyApp.isAppVisible) {
                    play();
                }
                super.onPause();
            }

            public void onSkipToPrevious(){
                Log.d(TAG, "onskiptoPrevious called (media button pressed)");
                if(!MyApp.isAppVisible) {
                    prevTrack();
                }
                super.onSkipToPrevious();
            }

            public void onSkipToNext() {
                Log.d(TAG, "onskiptonext called (media button pressed)");
                if(!MyApp.isAppVisible) {
                   nextTrack();
                }
                super.onSkipToNext();
            }

            public void onPlay() {
                Log.d(TAG, "onPlay called (media button pressed)");
                if(!MyApp.isAppVisible) {
                    play();
                }
                super.onPlay();
            }

            public void onStop() {
                if(!MyApp.isAppVisible) {
                    stop();
                }
                Log.d(TAG, "onStop called (media button pressed)");
                super.onStop();
            }
        });
        mMediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        stateBuilder = new PlaybackState.Builder()
                .setActions(
                        PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_PAUSE |
                                PlaybackState.ACTION_PLAY_FROM_MEDIA_ID | PlaybackState.ACTION_PAUSE |
                                PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS);
        PlaybackState state = stateBuilder
                .setState(PlaybackState.STATE_STOPPED, 0, 1)
                .build();

        mMediaSession.setPlaybackState(state);
        mMediaSession.setActive(true);
    }

    public static void setShakeListener(boolean status){
        if(status) {
            shakeDetector.start(sensorManager);
        }
        else {
            try {
                shakeDetector.stop();
            }catch (Exception ignored){

            }
        }

    }

    public void PostNotification(final boolean loadBitmap){
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {

                if(currentTrack==null){
                    return;
                }

                RemoteViews viewsSmall = new RemoteViews(getPackageName(), R.layout.status_notification_small);
                RemoteViews viewsExpanded = new RemoteViews(getPackageName(), R.layout.status_notification_expanded);
                viewsSmall.setOnClickPendingIntent(R.id.notificationPlay, pplayIntent);
                viewsSmall.setOnClickPendingIntent(R.id.notificationSkip_back, ppreviousIntent);
                viewsSmall.setOnClickPendingIntent(R.id.notificationSkip_forward, pnextIntent);
                //viewsSmall.setOnClickPendingIntent(R.id.notification_close, pdismissIntent);

                viewsExpanded.setOnClickPendingIntent(R.id.notificationPlay, pplayIntent);
                viewsExpanded.setOnClickPendingIntent(R.id.notificationSkip_back, ppreviousIntent);
                viewsExpanded.setOnClickPendingIntent(R.id.notificationSkip_forward, pnextIntent);
                viewsExpanded.setOnClickPendingIntent(R.id.close_player, pdismissIntent);

                if(sleepTimerMinutes!=0){
                    String temp = sleepTimerMinutes-sleepTimeAlreadyOver + " minutes to sleep";
                    viewsExpanded.setTextViewText(R.id.sleepTimer,temp);
                }else {
                    viewsExpanded.setTextViewText(R.id.sleepTimer,"");
                }

                viewsSmall.setTextViewText(R.id.notificationTrack_name,currentTrack.getTitle());
                viewsExpanded.setTextViewText(R.id.notificationTrack_name,currentTrack.getTitle());

                viewsSmall.setTextViewText(R.id.notificationArtist,currentTrack.getArtist());
                viewsExpanded.setTextViewText(R.id.notificationArtist,currentTrack.getArtist());

                if(status==PLAYING) {
                    viewsSmall.setImageViewResource(R.id.notificationPlay, R.drawable.ic_pause_black_24dp);
                    viewsExpanded.setImageViewResource(R.id.notificationPlay,R.drawable.ic_pause_black_24dp );
                }else {
                    viewsSmall.setImageViewResource(R.id.notificationPlay, R.drawable.ic_play_arrow_black_24dp);
                    viewsExpanded.setImageViewResource(R.id.notificationPlay,R.drawable.ic_play_arrow_black_24dp );
                }

                NotificationCompat.Builder builder = new NotificationCompat.Builder(PlayerService.this)
                        // Set Ticker Message
                        .setTicker("MusicPlayer")
                        // Dismiss Notification
                        .setAutoCancel(false)
                        // Set PendingIntent into Notification
                        .setContentIntent(pendingIntent)
                        // Set RemoteViews into Notification
                        .setContent(viewsSmall)
                        .setCustomBigContentView(viewsExpanded)
                        .setDeleteIntent(pSwipeToDismiss);

                builder.setSmallIcon(R.drawable.ic_batman_kitkat);

                final Notification notification = builder.build();

                Bitmap b = null;
                try {
                    b= UtilityFun.decodeUri(getApplication()
                            , MusicLibrary.getInstance().getAlbumArtUri(getCurrentTrack().getAlbumId()),200);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                ;
                if(b!=null) {
                    int width = b.getWidth();
                    int height = b.getHeight();
                    int maxWidth = 300;
                    int maxHeight =300;
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
                        height = maxHeight;
                        width = maxWidth;
                    }
                    b = Bitmap.createScaledBitmap(b, width,height, false);
                    viewsSmall.setImageViewBitmap(R.id.notificationAlbumart, b);
                    viewsExpanded.setImageViewBitmap(R.id.notificationAlbumart, b);
                }else {
                    viewsSmall.setImageViewResource(R.id.notificationAlbumart,R.drawable.ic_batman_1);
                    viewsExpanded.setImageViewResource(R.id.notificationAlbumart,R.drawable.ic_batman_1);
                }

                if(getStatus()==PLAYING) {
                    //builder.setOngoing(true);
                    startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,notification);
                }else {
                    stopForeground(false);
                    mNotificationManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, notification);
                }
               // mNotificationManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, builder.build());
            }
        });
    }

    //get played song position, shuffle the list and get the played song at first position
    //this will be called when song is played from music library
    private void shuffleTracklist(int position){
        if(!trackList.isEmpty()) {
            String originalSongPlayed = trackList.get(position);
            Collections.shuffle(trackList);
            trackList.remove(originalSongPlayed);
            trackList.add(position, originalSongPlayed);
        }
    }

    //this will be called when clicked on shuffle button on now playing
    public void shuffle(boolean shuffleStatus){
        if(!trackList.isEmpty()) {
            String currentSongPlaying = trackList.get(currentTrackPosition);
            if (shuffleStatus) {
                Collections.shuffle(trackList);
                trackList.remove(currentSongPlaying);
                trackList.add(0, currentSongPlaying);
                currentTrackPosition = 0;
            } else {
                Collections.sort(trackList);
                currentTrackPosition = trackList.indexOf(currentSongPlaying);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return playerBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }


    public void swapPosition(int from, int to){
        if(!trackList.isEmpty()) {
            String currentTrack = trackList.get(currentTrackPosition);
            Collections.swap(trackList, from, to);
            currentTrackPosition = trackList.indexOf(currentTrack);
        }
    }

    public void removeTrack(int position){
        if(currentTrackPosition>position) {
            currentTrackPosition--;
        }
        trackList.remove(position);
    }

    public void notifyUI() {
        Intent UIIntent = new Intent();
        UIIntent.setAction(Constants.ACTION.UI_UPDATE);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(UIIntent);
        try {
            updateWidget(true);
        }catch (Exception e ){

        }
        Intent intent = new Intent().setAction(Constants.ACTION.UPDATE_LYRIC_AND_INFO);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    public void updateWidget(boolean loadBitmap){
        Context context = this;
        if(getCurrentTrack()==null){
            return;
        }
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.wigdet);
        views.setTextViewText(R.id.song_name_widget, getCurrentTrack().getTitle());
        views.setTextViewText(R.id.artist_widget, getCurrentTrack().getArtist());

        if(loadBitmap) {
            Bitmap b= null;
            try {
                b = UtilityFun.decodeUri(this
                        , MusicLibrary.getInstance().getAlbumArtUri(getCurrentTrack().getAlbumId()),200);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            if(b!=null) {
                views.setImageViewBitmap(R.id.widget_album_art, b);
            }else {
                views.setImageViewResource(R.id.widget_album_art,R.drawable.ic_batman_1);
            }
        }

        if(status==PLAYING) {
            views.setImageViewResource(R.id.widget_Play, R.drawable.ic_pause_black_24dp);
        }else {
            views.setImageViewResource(R.id.widget_Play, R.drawable.ic_play_arrow_black_24dp);
        }

        if (MyApp.getPref().getBoolean(Constants.PREFERENCES.SHUFFLE, false)) {
            views.setInt(R.id.widget_shuffle,"setColorFilter", ColorHelper.getAccentColor());
        } else {
            views.setInt(R.id.widget_shuffle,"setColorFilter",ColorHelper.getColor(R.color.colorwhite));
        }

        views.setTextColor(R.id.text_in_repeat_widget,ColorHelper.getBrightPrimaryColor());

        if (MyApp.getPref().getInt(Constants.PREFERENCES.REPEAT, 0) == Constants.PREFERENCE_VALUES.REPEAT_ALL) {
            views.setTextViewText(R.id.text_in_repeat_widget,"A");
            views.setInt(R.id.widget_repeat,"setColorFilter", ColorHelper.getAccentColor());
        } else if (MyApp.getPref().getInt(Constants.PREFERENCES.REPEAT, 0) == Constants.PREFERENCE_VALUES.REPEAT_ONE) {
            views.setTextViewText(R.id.text_in_repeat_widget,"1");
            views.setInt(R.id.text_in_repeat_widget,"setTextColor", ColorHelper.getAccentColor());
            views.setInt(R.id.widget_repeat,"setColorFilter", ColorHelper.getAccentColor());
        } else if (MyApp.getPref().getInt(Constants.PREFERENCES.REPEAT, 0) == Constants.PREFERENCE_VALUES.NO_REPEAT) {
            views.setTextViewText(R.id.text_in_repeat_widget,"");
            views.setInt(R.id.widget_repeat,"setColorFilter", ColorHelper.getColor(R.color.colorwhite));
        }

        ComponentName thisWidget = new ComponentName(context, WidgetReceiver.class);
        appWidgetManager.updateAppWidget(thisWidget, views);
    }

    private void setStatus(int s) {
        status = s;
    }

    public int getStatus() {
        return status;
    }

    public TrackItem getCurrentTrack() {
        if (currentTrackPosition < 0) {
            return null;
        } else {
            return currentTrack;
        }
    }

    public int getCurrentTrackPosition() {
        return currentTrackPosition;
    }

    public ArrayList<String> getTrackList(){return trackList;}

    @TargetApi(21)
    public void setMediaSessionMetadata(final boolean enable) {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
                metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, currentTrack.getTitle());
                metadataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, currentTrack.getArtist());
                metadataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM, currentTrack.getAlbum());
                metadataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI
                        ,MusicLibrary.getInstance().getAlbumArtUri(currentTrack.getAlbumId()).toString());
                metadataBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION,currentTrack.getDurInt());
                metadataBuilder.putString(MediaMetadata.METADATA_KEY_GENRE, currentTrack.getGenre());
                if(MyApp.getPref().getBoolean(getString(R.string.pref_lock_screen_album_Art),true)) {
                    if (enable) {
                        Bitmap b= null;
                        try {
                            b = UtilityFun.decodeUri(getApplication()
                                    , MusicLibrary.getInstance().getAlbumArtUri(getCurrentTrack().getAlbumId()),200);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART,b);
                    } else {
                        metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, null);
                    }
                }
                mMediaSession.setMetadata(metadataBuilder.build());
            }
        });
    }

    public void playTrack(int pos) {

        try {
            trackList.get(pos);
        } catch (Exception ignored) {
            return;
        }

        TrackItem temp = MusicLibrary.getInstance().getTrackItemFromTitle(trackList.get(pos));
        if (temp == null) {
            Handler h = new Handler(getApplicationContext().getMainLooper());

            h.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext()
                            , "Something wrong! Track may be deleted or storage is not available at this time"
                            , Toast.LENGTH_LONG).show();
                }
            });
            return;
        }

        currentTrack = temp;

        PlaylistManager.getInstance(getApplicationContext()).AddToRecentlyPlayedAndUpdateCount(trackList.get(pos));

        int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return;
        }

        //currentTrack=trackInfoFetcher.get(trackList.get(pos)).iterator().next();

        gradualIncreaseVolume();

        if (status > STOPPED) {
            stop();
        }
        FileInputStream file = null;
        try {
            file = new FileInputStream(new File(currentTrack.getFilePath()));
            mediaPlayer.setDataSource(file.getFD());
            mediaPlayer.prepare();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.start();
        currentTrackPosition = pos;
        setStatus(PLAYING);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
            setSessionState();
            setMediaSessionMetadata(true);
        }

    }

    private void setSessionState(){
        //set state play
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

            if(status==PLAYING){
               stateBuilder.setState(PlaybackState.STATE_PLAYING, 0, 1);
            }else if(status==PAUSED) {
                stateBuilder.setState(PlaybackState.STATE_PAUSED, 0, 1);
            }else {
                stateBuilder.setState(PlaybackState.STATE_STOPPED, 0, 1);
            }
            mMediaSession.setPlaybackState(stateBuilder.build());
        }
    }

    public void playTrackFromFile(File fileToBePlayed){
        String song = MusicLibrary.getInstance().getTitleFromFilePath(fileToBePlayed.getAbsolutePath());
        TrackItem item=null;
        if(song!=null) {
            item=MusicLibrary.getInstance().getTrackItemFromTitle(song);
        }
        if(item==null){
            MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
            metaRetriever.setDataSource(fileToBePlayed.getAbsolutePath());

            String out = "";
            // get mp3 info

            // convert duration to minute:seconds
            String duration =
                    metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            item = new TrackItem();
            item.setTitle(fileToBePlayed.getName());
            item.setDuration(String.valueOf(duration));
            item.setArtist("unknown");
          //  item.setDuration();
        }
        ArrayList<String> temp = new ArrayList<>();
        temp.add(item.getTitle());
        setTrackList(temp);

        currentTrack = item;

        int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        if(result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        {
            return;
        }

        if (status > STOPPED) {
            stop();
        }

        gradualIncreaseVolume();
        FileInputStream file = null;
        try {
            file = new FileInputStream(fileToBePlayed);
            mediaPlayer.setDataSource(file.getFD());
            mediaPlayer.prepare();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mediaPlayer.start();
        currentTrackPosition = 0;
        setStatus(PLAYING);
        PostNotification(true);
        notifyUI();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            setSessionState();
            setMediaSessionMetadata(true);
        }
    }

    public void playAtPosition(int position){
        if(MyApp.getPref().getBoolean(Constants.PREFERENCES.SHUFFLE,false)){
            shuffleTracklist(position);
            playTrack(position);
        }else {
            playTrack(position);
        }
        PostNotification(true);
        notifyUI();
    }

    public void playAtPositionFromNowPlaying(int position){
        playTrack(position);
        PostNotification(true);
        notifyUI();
    }

    public void play() {

        if(status==PAUSED){
            gradualIncreaseVolume();
        }

        switch (status) {
            case STOPPED:
                if (!trackList.isEmpty()) {
                    playTrack(currentTrackPosition);
                    notifyUI();
                }
                break;

            case PLAYING:
                mediaPlayer.pause();
                setStatus(PAUSED);
                setSessionState();
                break;

            case PAUSED:
                mediaPlayer.start();
                setStatus(PLAYING);
                setSessionState();
                break;
        }
        PostNotification(true);
    }

    public void pause() {
        mediaPlayer.pause();
        setStatus(PAUSED);
        setSessionState();
        PostNotification(false);
    }

    public void stop() {
        mediaPlayer.stop();
        mediaPlayer.reset();
        setStatus(STOPPED);
    }

    public void shuffleAll(){
        ArrayList<String> tempList= MusicLibrary.getInstance().getDefaultTracklist();
        if(tempList!=null) {
            Collections.shuffle(tempList);
            setTrackList(tempList);
            playTrack(0);
            notifyUI();
            PostNotification(true);
        }
    }


    /*
    add songs to playlist for
    arguments:  clickedON = header string to process
                status = which fragment  (title,artist,albumName,genre)
                whereToAdd = position where to add (immediately, atLast)
     */
    public void addToQ(String clickedOn,  int whereToAdd){
        int addPosition = (whereToAdd==Constants.ADD_TO_Q.IMMEDIATE_NEXT ? currentTrackPosition : trackList.size()-1) ;
        try {
            trackList.add(addPosition + 1, clickedOn);
        }catch (ArrayIndexOutOfBoundsException e){
            Toast.makeText(getApplicationContext(), "Error adding song to queue", Toast.LENGTH_LONG);
        }
    }

    public void nextTrack() {
        if(MyApp.getPref().getInt(Constants.PREFERENCES.REPEAT,0)==Constants.PREFERENCE_VALUES.REPEAT_ONE){
            playTrack(currentTrackPosition);
        }else if (currentTrackPosition < trackList.size()-1) {
            playTrack(currentTrackPosition+1);
        } else if(currentTrackPosition == trackList.size()-1){
            //if repeat all on, play first song
            if(MyApp.getPref().getInt(Constants.PREFERENCES.REPEAT,0)==Constants.PREFERENCE_VALUES.REPEAT_ALL){
                playTrack(0);
                currentTrackPosition=0;
            }else {
                Toast.makeText(getApplicationContext(),"Empty queue!",Toast.LENGTH_LONG).show();
            }
        }
        PostNotification(true);
        //notifyUI();
    }

    public void prevTrack() {
        if(MyApp.getPref().getBoolean(Constants.PREFERENCES.PREV_ACTION,false)){
            if(((float)getCurrentTrackProgress()/(float)getCurrentTrackDuration())
                    > Constants.PREFERENCE_VALUES.PREV_ACT_TIME_CONSTANT){
                //start same song from start
                mediaPlayer.seekTo(0);
            }else if (currentTrackPosition > 0) {
                //currentTrack=MusicLibrary.getInstance().getTrackItemFromTitle(trackList.get(currentTrackPosition-1));
                playTrack(currentTrackPosition-1);
            }
        }else if (currentTrackPosition > 0) {
            //currentTrack=MusicLibrary.getInstance().getTrackItemFromTitle(trackList.get(currentTrackPosition-1));
            playTrack(currentTrackPosition-1);
        }
        PostNotification(true);
        //notifyUI();
    }

    //to update trackitem when tags are changed
    public void updateTrackItem(int position, String... param){
        if(position==getCurrentTrackPosition()) {
            try {
                currentTrack.setTitle(param[0]);
                currentTrack.setArtist(param[1]);
                currentTrack.setAlbum(param[2]);
            } catch (Exception ignored) {

            }
        }
        trackList.set(position,param[0]);
    }

    public int getCurrentTrackProgress() {
        if (status > STOPPED) {
            return mediaPlayer.getCurrentPosition();
        } else {
            return 0;
        }
    }

    //returns duration of current item in ms
    public int getCurrentTrackDuration() {

        if (currentTrackPosition!=-1 && currentTrack!=null) {
            return currentTrack.getDurInt();
        } else {
            return 0;
        }
    }

    public void seekTrack(int p) {
        if (status > STOPPED) {
            mediaPlayer.seekTo(p);
        }
    }

    public void setTrackList(ArrayList<String> tracklist1){
        long start=System.currentTimeMillis();
        this.trackList.clear();
        this.trackList.addAll(tracklist1);
    }

    /*
    sleep timer runnable and variables
     */
    private int sleepTimerMinutes=0;   //sleep timer minutes
    private int sleepTimeAlreadyOver = -1;  //already over minutes
    private Handler sleepTimerHandler = new Handler();
    public void setSleepTimer(int minutes, boolean enable){
        sleepTimerHandler.removeCallbacksAndMessages(null);
        sleepTimeAlreadyOver=-1;
        sleepTimerMinutes = minutes;
        if(!enable){
            PostNotification(false);
            return;
        }

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if(sleepTimerMinutes==++sleepTimeAlreadyOver) {
                    LocalBroadcastManager.getInstance(getApplicationContext())
                            .sendBroadcast(new Intent().setAction(Constants.ACTION.DISMISS_EVENT));
                    Toast.makeText(getApplicationContext(), "Timer Over", Toast.LENGTH_LONG).show();
                    MyApp.getPref().edit().putInt(getString(R.string.pref_sleep_timer), 0).apply();
                    sleepTimerMinutes=0;
                    sleepTimeAlreadyOver=0;
                }else {
                    if(getStatus()==PLAYING) {
                        PostNotification(false);
                    }
                    sleepTimerHandler.postDelayed(this, 1000*60);
                }
            }
        };
        sleepTimerHandler.post(runnable);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if(getStatus()!=PLAYING) {
            if(MyApp.getPref().getInt(getString(R.string.pref_sleep_timer),0)!=0) {
                MyApp.getPref().edit().putInt(getString(R.string.pref_sleep_timer), 0).apply();
                sleepTimerHandler.removeCallbacksAndMessages(null);
            }
            stopForeground(true);
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        Log.v(Constants.TAG,"SERVICE ON DESTROY");
        updateWidget(true);
        storeTracklist();
        mNotificationManager.cancelAll();
        mediaPlayer.stop();
        mediaPlayer.reset();
        mediaPlayer.release();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setStatus(STOPPED);
            setSessionState();
            mMediaSession.setActive(false);
            mMediaSession.release();
        }
        shakeDetector.stop();
        this.unregisterReceiver(mReceiverHeadset);
        TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if(mgr != null) {
            mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);
        mAudioManager.abandonAudioFocus(this);
        super.onDestroy();
    }

    public void storeTracklist() {

        PlaylistManager.getInstance(getApplicationContext()).StoreLastPlayingQueue(trackList);
        try {
            MyApp.getPref().edit().putString(Constants.PREFERENCES.STORED_SONG_TITLE,currentTrack.getTitle()).apply();
            MyApp.getPref().edit().putInt(Constants.PREFERENCES.STORED_SONG_POSITION_DURATION,getCurrentTrackProgress()).apply();
        }catch (Exception e){

        }
    }

    private void restoreTracklist() {
        trackList.addAll(PlaylistManager.getInstance(getApplicationContext()).RestoreLastPlayingQueue());
        String title = MyApp.getPref().getString(Constants.PREFERENCES.STORED_SONG_TITLE,"");

        if(!trackList.isEmpty() && !trackList.contains(title)){
            //this should not happen.
            //but if does, handle it by loading default tracklist
            trackList.clear();
            trackList.addAll(MusicLibrary.getInstance().getDefaultTracklist());

            if(!trackList.isEmpty()) {
                int random = new Random().nextInt(trackList.size()-1);
                currentTrack = MusicLibrary.getInstance().getTrackItemFromTitle(trackList.get(random));
                currentTrackPosition = random;
            }else {
                currentTrackPosition = -1;
                Toast.makeText(getApplicationContext(),"Empty library!",Toast.LENGTH_LONG).show();
            }
        } else {
            currentTrackPosition = trackList.indexOf(title);
            try {
                currentTrack = MusicLibrary.getInstance().getTrackItemFromTitle(title);
            } catch (Exception e) {
                Log.e(Constants.TAG, e.getStackTrace().toString(), e);
            }
        }
        FileInputStream file = null;
        try {
            file = new FileInputStream(new File(currentTrack.getFilePath()));
            mediaPlayer.setDataSource(file.getFD());
            mediaPlayer.prepare();
            mediaPlayer.seekTo(MyApp.getPref().getInt(Constants.PREFERENCES.STORED_SONG_POSITION_DURATION, 0));
            setStatus(PAUSED);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e){
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        Intent intent;
        Log.v(Constants.TAG,"focus"+focusChange);
        if(focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        {
            if(status==PLAYING){
                pause();
                notifyUI();
                musicPuasedBecauseOfFocusLoss=true;
            }
           /* Log.v(Constants.TAG,"music paused" + musicPausedBecauseOfCall);
            if(!musicPausedBecauseOfCall) {
                gradualIncreaseVolume();
            }*/
        }
        else if(focusChange == AudioManager.AUDIOFOCUS_GAIN)
        {
            if(musicPuasedBecauseOfFocusLoss){
                if(status==PAUSED){
                    play();
                    notifyUI();
                    musicPuasedBecauseOfFocusLoss=false;
                }
            }
        }
        else if(focusChange == AudioManager.AUDIOFOCUS_LOSS)
        {
            if(status==PLAYING) {
                pause();
                notifyUI();
                musicPuasedBecauseOfFocusLoss=true;
            }
        }
    }

    @Override
    public void hearShake() {
        switch (MyApp.getPref().getInt(getString(R.string.pref_shake_action),Constants.SHAKE_ACTIONS.NEXT)){
            case Constants.SHAKE_ACTIONS.NEXT:
                if(status==PLAYING) {
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(Constants.ACTION.NEXT_ACTION));
                }
                break;

            case Constants.SHAKE_ACTIONS.PLAY_PAUSE:
                LocalBroadcastManager.getInstance(getApplicationContext())
                        .sendBroadcast(new Intent(Constants.ACTION.PLAY_PAUSE_ACTION));
                break;

            case Constants.SHAKE_ACTIONS.PREVIOUS:
                if(status==PLAYING) {
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(Constants.ACTION.PREV_ACTION));
                }
                break;
        }
    }

    private void gradualIncreaseVolume(){
        //gradually increase volume from zero to current volume level
        //this is called when call is done.
        //android mutes music stream when call happens
        //to prevent current volume to go to mute, we set it 1/3rd of max volume
        if(musicPausedBecauseOfCall) {
            currentVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)/3;
            return;
        }else if(!fVolumeIsBeingChanged){
            currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        } else {
            mHandler.removeCallbacksAndMessages(gradualVolumeRaiseRunnable);
        }
        //currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,0,0);
        mHandler.post(gradualVolumeRaiseRunnable);
    }

    private final Runnable gradualVolumeRaiseRunnable = new Runnable() {
        @Override
        public void run() {
            if (mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)<currentVolume){
                fVolumeIsBeingChanged=true;
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)+1,0);
                //Log.v(Constants.TAG,"Stream volum"+mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                mHandler.postDelayed(gradualVolumeRaiseRunnable,50);
            }else {
                fVolumeIsBeingChanged = false;
            }
        }
    };


    public class PlayerBinder extends Binder {

        public PlayerService getService() {
            return PlayerService.this;
        }
    }

    private class HeadSetReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        if(!isInitialStickyBroadcast())   //this is for removing any sticky headset broadcast in system
                        {
                            if(status==PLAYING) {
                                pause();
                                notifyUI();
                            }
                            Log.d(getClass().toString(), "Headset unplugged");
                        }
                            break;

                    case 1:
                        Log.d(getClass().toString(), "Headset plugged");
                        break;
                }
            }
        }
    }
}

