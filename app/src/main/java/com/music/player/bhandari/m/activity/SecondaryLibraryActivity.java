package com.music.player.bhandari.m.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.UIElemetHelper.ColorHelper;
import com.music.player.bhandari.m.UIElemetHelper.FontFactory;
import com.music.player.bhandari.m.adapter.SecondaryLibraryAdapter;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.model.TrackItem;
import com.music.player.bhandari.m.service.PlayerService;
import com.music.player.bhandari.m.utils.MusicLibrary;
import com.music.player.bhandari.m.utils.MyApp;
import com.music.player.bhandari.m.utils.PlaylistManager;
import com.music.player.bhandari.m.utils.UtilityFun;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by amit on 6/12/16.
 */

public class SecondaryLibraryActivity extends AppCompatActivity implements View.OnClickListener {


    private RecyclerView mRecyclerView;
    private SecondaryLibraryAdapter adapter;
    private BroadcastReceiver mReceiverForMiniPLayerUpdate;
    private TextView songNameMiniPlayer,artistNameMiniPlayer;
    private  ImageView buttonPlay;
    private ImageView albumArtIv;
    private long mLastClickTime;

    private int status;
    private int key=0;  //text view on which clicked
    private String title;


    public SecondaryLibraryActivity(){}

    @Override
    protected void onNewIntent(Intent intent) {
        boolean b = intent.getBooleanExtra("refresh",false);
        if(b){
            int position = intent.getIntExtra("position", -1);
            String title = intent.getStringExtra("title");
            String artist = intent.getStringExtra("artist");
            String album = intent.getStringExtra("album");
            String originalTitle = intent.getStringExtra("originalTitle");

            if(MyApp.getService().getCurrentTrack().getTitle().equals(originalTitle)){
                //current song is playing, update  track item
                    MyApp.getService().updateTrackItem(MyApp.getService().getCurrentTrackPosition(),title, artist, album);
                    MyApp.getService().PostNotification(false);
                    updateMiniplayerUI();
            }

            //data changed in edit track info activity, update item
            adapter.updateItem(position,title,artist,album);
        }
        super.onNewIntent(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
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
        setContentView(R.layout.activity_secondary_library);

        final ImageView collapsingImage = ((ImageView) (findViewById(R.id.main_backdrop)));
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_right_side);

        Toolbar toolbar = (Toolbar) findViewById(R.id.play_queue_title);
        try {
            toolbar.setCollapsible(false);
        }catch (Exception e){}
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        if(getIntent()!=null) {
            status = getIntent().getIntExtra("status",0);
            key = getIntent().getIntExtra("key",0);
            title = getIntent().getStringExtra("title");
        }
        //remove _ from playlist name

        if(status==Constants.FRAGMENT_STATUS.PLAYLIST_FRAGMENT){
            setTitle(title.replace("_"," "));
        }else {
            setTitle(title);
        }

        if(MyApp.isLocked()){
            findViewById(R.id.border_view).setVisibility(View.VISIBLE);
        }else {
            findViewById(R.id.border_view).setVisibility(View.GONE);
        }

        mRecyclerView = (RecyclerView) findViewById(R.id.secondaryLibraryList);
        Bitmap albumArtBitmap = null;
        switch (status) {
                case Constants.FRAGMENT_STATUS.ARTIST_FRAGMENT:

                    adapter = new SecondaryLibraryAdapter(this, MusicLibrary.getInstance().getSongListFromArtistId(key, Constants.SORT_ORDER.ASC));
                    if(adapter.getList().isEmpty()) {
                        break;
                    }
                    break;

                case Constants.FRAGMENT_STATUS.ALBUM_FRAGMENT:
                    adapter = new SecondaryLibraryAdapter(this,
                            MusicLibrary.getInstance().getSongListFromAlbumId(key, Constants.SORT_ORDER.ASC));
                    if(adapter.getList().isEmpty()) {
                        break;
                    }
                    break;

                case Constants.FRAGMENT_STATUS.GENRE_FRAGMENT:
                    adapter = new SecondaryLibraryAdapter(this,
                            MusicLibrary.getInstance().getSongListFromGenreId(key, Constants.SORT_ORDER.ASC));
                    if(adapter.getList().isEmpty()) {
                        break;
                    }
                    break;

                case Constants.FRAGMENT_STATUS.PLAYLIST_FRAGMENT:
                    ArrayList<String> trackList;
                    switch (title) {
                        case Constants.SYSTEM_PLAYLISTS.MOST_PLAYED:
                            trackList = PlaylistManager.getInstance(getApplicationContext())
                                    .GetPlaylist(Constants.SYSTEM_PLAYLISTS.MOST_PLAYED);
                            adapter = new SecondaryLibraryAdapter(this, trackList, status, Constants.SYSTEM_PLAYLISTS.MOST_PLAYED);
                            break;

                        case Constants.SYSTEM_PLAYLISTS.MY_FAV:
                            trackList = PlaylistManager.getInstance(getApplicationContext())
                                    .GetPlaylist(Constants.SYSTEM_PLAYLISTS.MY_FAV);
                            adapter = new SecondaryLibraryAdapter(this, trackList, status, Constants.SYSTEM_PLAYLISTS.MY_FAV);
                            break;

                        case Constants.SYSTEM_PLAYLISTS.RECENTLY_ADDED:
                            trackList = PlaylistManager.getInstance(getApplicationContext())
                                    .GetPlaylist(Constants.SYSTEM_PLAYLISTS.RECENTLY_ADDED);
                            adapter = new SecondaryLibraryAdapter(this, trackList, status, Constants.SYSTEM_PLAYLISTS.RECENTLY_ADDED);
                            break;

                        case Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED:
                            trackList = PlaylistManager.getInstance(getApplicationContext())
                                    .GetPlaylist(Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED);
                            adapter = new SecondaryLibraryAdapter(this, trackList, status, Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED);
                            break;

                        default:
                            trackList = PlaylistManager.getInstance(getApplicationContext())
                                    .GetPlaylist(title);
                            adapter = new SecondaryLibraryAdapter(this, trackList, status, title);
                            break;
                    }
                    if(trackList.isEmpty()){
                        fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_add_black_24dp));
                    }
                    break;
            }

        if(adapter!=null) {
            mRecyclerView.setAdapter(adapter);
        }

        mRecyclerView.setLayoutManager(new WrapContentLinearLayoutManager(this));

        mReceiverForMiniPLayerUpdate=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateMiniplayerUI();
            }
        };

        LinearLayout miniPlayer = (LinearLayout) findViewById(R.id.mini_player);
        miniPlayer.setOnClickListener(this);

        buttonPlay=(ImageView) findViewById(R.id.play_pause_mini_player);
        buttonPlay.setOnClickListener(this);

        ImageView buttonNext = (ImageView) findViewById(R.id.next_mini_plaayrer);
        buttonNext.setOnClickListener(this);

        songNameMiniPlayer=(TextView) findViewById(R.id.song_name_mini_player);
        songNameMiniPlayer.setTypeface(FontFactory.getFont());

        artistNameMiniPlayer=(TextView) findViewById(R.id.artist_mini_player);
        artistNameMiniPlayer.setTypeface(FontFactory.getFont());

        albumArtIv =(ImageView) findViewById(R.id.album_art_mini_player);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        TrackItem item=null;
        if (adapter.getList().size() > 1) {
            item = MusicLibrary.getInstance().getTrackItemFromTitle(adapter.getList().get(0).title);
        }
        //if only item is emoty item

        //very ugly part dpwn, please improve for god sake

        if(status==Constants.FRAGMENT_STATUS.ARTIST_FRAGMENT){
            //look in cache for artist image
            String CACHE_ART_THUMBS = this.getCacheDir()+"/art_thumbs/";
            String actual_file_path = CACHE_ART_THUMBS+title;
            albumArtBitmap = BitmapFactory.decodeFile(actual_file_path);
        }else {
            if(item!=null)
            albumArtBitmap = MusicLibrary.getInstance().getAlbumArtFromTitle(item.getTitle());
        }

        if(status!=Constants.FRAGMENT_STATUS.ARTIST_FRAGMENT && item!=null &&  item.haveAlbumArt()) {
            Glide.with(getApplication())
                    .load(MusicLibrary.getInstance().getAlbumArtUri(item.getAlbumId()))
                    .placeholder(R.drawable.ic_batman_1)
                    .signature(new StringSignature(String.valueOf(System.currentTimeMillis())))
                    .into(((ImageView) (findViewById(R.id.main_backdrop))));
           // ((ImageView) (findViewById(R.id.main_backdrop))).setImageBitmap(albumArtBitmap);
        }else  if(status==Constants.FRAGMENT_STATUS.ARTIST_FRAGMENT) {
            if(albumArtBitmap==null && item!=null){
                Glide.with(getApplication())
                        .load(MusicLibrary.getInstance().getAlbumArtUri(item.getAlbumId()))
                        .placeholder(R.drawable.ic_batman_1)
                        .signature(new StringSignature(String.valueOf(System.currentTimeMillis())))
                        .into(((ImageView) (findViewById(R.id.main_backdrop))));
            }else {
                ((ImageView) (findViewById(R.id.main_backdrop)))
                        .setImageBitmap(albumArtBitmap);
            }
        }
        else {
            ((ImageView) (findViewById(R.id.main_backdrop)))
                    .setImageDrawable(getResources().getDrawable(R.drawable.ic_batman_1));
        }



        final Drawable d;
        if(MyApp.getPref().getInt(getString(R.string.pref_theme),Constants.THEME.GLOSSY)==Constants.THEME.GLOSSY){
            int color = 0 ;
            if(albumArtBitmap!=null) {
                color = UtilityFun.GetDominantColor(albumArtBitmap);
            }else {
                color = ColorHelper.getPrimaryColor();
            }


            d = new GradientDrawable(
                    GradientDrawable.Orientation.BR_TL,
                    new int[] {color,0xFF131313});
        }else {
            d = ColorHelper.getBaseThemeDrawable();
        }


        findViewById(R.id.root_view_secondary_lib).setBackgroundDrawable(d);

       // findViewById(R.id.mini_player).setBackgroundDrawable(ColorHelper.getColoredThemeDrawable());
        findViewById(R.id.mini_player).setBackgroundColor(ColorHelper.getPrimaryColor());
        ((CollapsingToolbarLayout)findViewById(R.id.main_collapsing)).setContentScrimColor(ColorHelper.getPrimaryColor());
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(status==Constants.FRAGMENT_STATUS.PLAYLIST_FRAGMENT && adapter.getItemCount()==0){
                    startActivity(new Intent(SecondaryLibraryActivity.this,MainActivity.class)
                            .putExtra("move_to_tab",Constants.VIEW_PAGER_FRAGMENT_INDEX.TITLE));
                    //Toast.makeText(SecondaryLibraryActivity.this, "Add tracks to playlist by clicking on ", Toast.LENGTH_SHORT).show();
                }else {
                    //one empty element is added
                    if (adapter.getItemCount() <= 1) {
                        Toast.makeText(SecondaryLibraryActivity.this, "Empty Track List", Toast.LENGTH_SHORT).show();
                    } else {
                        adapter.shuffleAll();
                        Intent playerServiceIntent = new Intent(SecondaryLibraryActivity.this, NowPlayingActivity.class);
                        startActivity(playerServiceIntent);
                    }
                }
            }
        });
        fab.setBackgroundTintList(ColorStateList.valueOf(ColorHelper.getAccentColor()));
    }

    private void updateMiniplayerUI(){
        try {
            if (MyApp.getService() != null) {
                if (MyApp.getService().getCurrentTrack() != null) {

                    Glide.with(getApplication())
                            .load(MusicLibrary.getInstance().getAlbumArtUri(MyApp.getService().getCurrentTrack().getAlbumId()))
                            .asBitmap()
                            .signature(new StringSignature(String.valueOf(System.currentTimeMillis())))
                            .centerCrop()
                            .placeholder(R.drawable.ic_batman_1)
                            .animate(R.anim.fade_in)
                            .into(albumArtIv);

                    //albumArtIv.setImageBitmap(MyApp.getService().getAlbumArt());
                    if (MyApp.getService().getStatus() == PlayerService.PLAYING) {
                        buttonPlay.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_pause_black_24dp));
                    } else {
                        buttonPlay.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_play_arrow_black_24dp));
                    }
                    songNameMiniPlayer.setText(MyApp.getService().getCurrentTrack().getTitle());
                    artistNameMiniPlayer.setText(MyApp.getService().getCurrentTrack().getArtist());
                    ((AppBarLayout) findViewById(R.id.app_bar_layout)).setExpanded(true);
                    //mHandler.post(getDominantColorRunnable());
                }
            } else {
                //this should not happen
                //restart app
                System.exit(0);
            }
        }catch (Exception ignored){

        }
    }

    @Override
    public void onClick(View view) {
        Intent notificationIntent=new Intent(getApplicationContext(),PlayerService.class);
        switch (view.getId()){
            case R.id.mini_player:
                Intent intent=new Intent(getApplicationContext(),NowPlayingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(R.anim.abc_slide_in_bottom, android.R.anim.fade_out);
                Log.v(Constants.TAG,"Launch now playing Jarvis");
                //finish();
                break;

            case R.id.play_pause_mini_player:
                if(MyApp.getService().getCurrentTrack()==null) {
                    Toast.makeText(this,"Nothing to play!",Toast.LENGTH_LONG).show();
                    return;
                }

                if (SystemClock.elapsedRealtime() - mLastClickTime < 300){
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                MyApp.getService().play();
                MyApp.getService().PostNotification(true);

                if (MyApp.getService().getStatus() == PlayerService.PLAYING) {
                    buttonPlay.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_pause_black_24dp));
                } else {
                    buttonPlay.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_play_arrow_black_24dp));
                }
                /*
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent()
                        .setAction(Constants.ACTION.PLAY_PAUSE_ACTION));*/
                break;

            case R.id.next_mini_plaayrer:
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                MyApp.getService().nextTrack();
                updateMiniplayerUI();
                /*
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent()
                        .setAction(Constants.ACTION.NEXT_ACTION));*/
                Log.v(Constants.TAG,"next track please Jarvis");
                break;
        }
    }

    @Override
    protected void onStop() {
        adapter.clear();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        for(int i = 0; i < menu.size(); i++){
            if(R.id.action_search==menu.getItem(i).getItemId()
                   || R.id.action_sort==menu.getItem(i).getItemId() ) {
                menu.getItem(i).setVisible(false);
            }
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.action_settings:
                startActivity(new Intent(this,SettingsActivity.class));
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        mRecyclerView=null;
        super.onDestroy(); //get search icon back on action bar
    }

    @Override
    public void onResume() {
        super.onResume();
        MyApp.isAppVisible = true;
        if(adapter!=null) {
            adapter.bindService();
        }
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiverForMiniPLayerUpdate
                ,new IntentFilter(Constants.ACTION.UI_UPDATE));
        updateMiniplayerUI();
    }

    @Override
    protected void onPause() {
        MyApp.isAppVisible = false;
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiverForMiniPLayerUpdate);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.slide_in_left,android.R.anim.slide_out_right);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                MyApp.getService().play();
                break;

            case KeyEvent.KEYCODE_MEDIA_NEXT:
                MyApp.getService().nextTrack();
                break;

            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                MyApp.getService().prevTrack();
                break;

            case KeyEvent.KEYCODE_MEDIA_STOP:
                MyApp.getService().stop();
                break;

            case KeyEvent.KEYCODE_BACK:
                onBackPressed();
                break;
        }

        return false;
    }
    //for catching exception generated by recycler view which was causing abend, no other way to handle this
    class WrapContentLinearLayoutManager extends LinearLayoutManager {
        WrapContentLinearLayoutManager(Context context) {
            super(context);
        }

        //... constructor
        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                super.onLayoutChildren(recycler, state);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }
    }

}
