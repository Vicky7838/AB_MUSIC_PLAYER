package com.music.player.bhandari.m.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.UIElemetHelper.ColorHelper;
import com.music.player.bhandari.m.UIElemetHelper.FontFactory;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.service.PlayerService;
import com.music.player.bhandari.m.utils.AppRater;
import com.music.player.bhandari.m.utils.MusicLibrary;
import com.music.player.bhandari.m.utils.MyApp;
import com.music.player.bhandari.m.utils.PlaylistManager;
import com.music.player.bhandari.m.utils.T;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener
        , SwipeRefreshLayout.OnRefreshListener , PopupMenu.OnMenuItemClickListener{

    private long mLastClickTime = 0;

    //to receive broadcast to update mini player
    private  BroadcastReceiver mReceiverForMiniPLayerUpdate;
    private BroadcastReceiver mReceiverForLibraryRefresh;

    public static final String NOTIFY_BACK_PRESSED="BACK_PRESSED";

    private  ViewPager viewPager;
    private  ViewPagerAdapter viewPagerAdapter;
    private  ImageView buttonPlay;
    private ImageView albumArt;
    private  TextView songNameMiniPlayer,artistNameMiniPlayer;
    private  NavigationView navigationView;
    private FloatingActionButton fab_right_side, fab_lock;

    //bind player service
    private PlayerService playerService;
    private boolean mBound=false;
    private ServiceConnection playerServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            PlayerService.PlayerBinder playerBinder = (PlayerService.PlayerBinder) service;
            playerService = playerBinder.getService();
            MyApp.setService(playerService);
            mBound=true;
            updateUI(false);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound=false;
        }
    };

    //search box relaated things
    private  MenuItem mSearchAction;
    private  boolean isSearchOpened = false;
    private  EditText editSearch;
    private  String searchQuery="";
    private  Handler mHandler;
    private InputMethodManager imm;

    private String currentPageSort = ""; //holds the value for pref id for current page sort by

    @Override
    protected void onNewIntent(Intent intent) {
        //go to tracks tab when clicked on add button in playlist section
        int i = intent.getIntExtra("move_to_tab",-1);
        if(viewPager!=null && i!=-1){
            viewPager.setCurrentItem(i);
        }


        boolean b = intent.getBooleanExtra("refresh",false);
        if(b){
            //data changed in edit track info activity, update item
            String originalTitle = intent.getStringExtra("originalTitle");
            int position = intent.getIntExtra("position", -1);
            String title = intent.getStringExtra("title");
            String artist = intent.getStringExtra("artist");
            String album = intent.getStringExtra("album");

            if(MyApp.getService().getCurrentTrack().getTitle().equals(originalTitle)){
                //current song is playing, update  track item
                MyApp.getService().updateTrackItem(MyApp.getService().getCurrentTrackPosition(),title,artist,album);
                MyApp.getService().PostNotification(false);
                updateUI(false);
            }

            if(viewPagerAdapter.getItem(viewPager.getCurrentItem()) instanceof FragmentAlbumLibrary){
                //this should not happen
            }else if(viewPagerAdapter.getItem(viewPager.getCurrentItem()) instanceof FragmentLibrary){
                ((FragmentLibrary) viewPagerAdapter.getItem(viewPager.getCurrentItem()))
                        .updateItem(position, title, artist, album);
            }

        }

        super.onNewIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //bind music service
        //startService(new Intent(this,PlayerService.class));


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


        setContentView(R.layout.activity_main);

        T t = new T("MainActivity");
        t.start();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_albums);

        navigationView.setBackgroundDrawable(ColorHelper.getBaseThemeDrawable());

        updateNavigationMenuItems();

        getResources().getDrawable(R.drawable.ic_batman_1).setColorFilter(ColorHelper.getPrimaryColor(), PorterDuff.Mode.OVERLAY);

       //findViewById(R.id.app_bar_layout).setBackgroundDrawable(ColorHelper.getColoredThemeDrawable());
        findViewById(R.id.app_bar_layout).setBackgroundColor(ColorHelper.getPrimaryColor());
        findViewById(R.id.tabs).setBackgroundColor(ColorHelper.getPrimaryColor());
        //findViewById(R.id.tabs).setBackgroundDrawable(ColorHelper.getColoredThemeDrawable());
        switch (themeSelector){
            case Constants.THEME.DARK:
                navigationView.getHeaderView(0).findViewById(R.id.navigation_header)
                        .setBackgroundDrawable(ColorHelper.getColoredThemeDrawable());
                break;

            case Constants.THEME.GLOSSY:
                navigationView.getHeaderView(0).findViewById(R.id.navigation_header)
                        .setBackgroundColor(ContextCompat.getColor(this,R.color.colorTransparent));;
                break;

            case Constants.THEME.LIGHT:
                navigationView.getHeaderView(0).findViewById(R.id.navigation_header)
                        .setBackgroundDrawable(ColorHelper.getColoredThemeDrawable());
                break;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ColorHelper.getDarkPrimaryColor());
        }

        setTitle("Library");

        //refresh music library
        //MusicLibrary.getInstance();

        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mHandler = new Handler();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        try {
            toolbar.setCollapsible(false);
        }catch (Exception ignored){

        }
        setSupportActionBar(toolbar);

        mReceiverForMiniPLayerUpdate=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUI(true);
            }
        };

        mReceiverForLibraryRefresh=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //updateUI();
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

        albumArt =(ImageView) findViewById(R.id.album_art_mini_player);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();


        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Log.v(Constants.TAG,"position : " + viewPager.getCurrentItem());
                invalidateOptionsMenu();
                switch (position){
                    case Constants.VIEW_PAGER_FRAGMENT_INDEX.ALBUM:
                        navigationView.setCheckedItem(R.id.nav_albums);
                        break;

                    case Constants.VIEW_PAGER_FRAGMENT_INDEX.ARTIST:
                        navigationView.setCheckedItem(R.id.nav_artists);
                        break;

                    case Constants.VIEW_PAGER_FRAGMENT_INDEX.FOLDER:
                        navigationView.setCheckedItem(R.id.nav_folders);
                        break;

                    case Constants.VIEW_PAGER_FRAGMENT_INDEX.GENRE:
                        navigationView.setCheckedItem(R.id.nav_genres);
                        break;

                    case Constants.VIEW_PAGER_FRAGMENT_INDEX.TITLE:
                        navigationView.setCheckedItem(R.id.nav_tracks);
                        break;

                    case Constants.VIEW_PAGER_FRAGMENT_INDEX.PLAYLIST:
                        navigationView.setCheckedItem(R.id.nav_playlists);
                        break;
                }

                if(position==Constants.VIEW_PAGER_FRAGMENT_INDEX.PLAYLIST){
                    fab_right_side.setImageDrawable(ContextCompat.getDrawable(MainActivity.this,R.drawable.ic_add_black_24dp));
                }else {
                    fab_right_side.setImageDrawable(ContextCompat.getDrawable(MainActivity.this,R.drawable.ic_shuffle_black_24dp));
                }
                if(!searchQuery.equals("")){
                    try {
                        if (position != Constants.VIEW_PAGER_FRAGMENT_INDEX.FOLDER
                                && position != Constants.VIEW_PAGER_FRAGMENT_INDEX.PLAYLIST
                                && position != Constants.VIEW_PAGER_FRAGMENT_INDEX.ALBUM) {
                            ((FragmentLibrary) viewPagerAdapter.getItem(position))
                                    .filter(String.valueOf(searchQuery));
                        }
                        if (position == Constants.VIEW_PAGER_FRAGMENT_INDEX.ALBUM) {
                            if (!MyApp.getPref().getBoolean(getString(R.string.pref_album_lib_view), true)) {
                                ((FragmentLibrary) viewPagerAdapter.getItem(position))
                                        .filter(String.valueOf(searchQuery));
                            } else {
                                ((FragmentAlbumLibrary) viewPagerAdapter.getItem(position))
                                        .filter(String.valueOf(searchQuery));
                            }

                        }
                    }catch (Exception ignored){

                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });


        TabLayout tabLayout=(TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        fab_right_side = (FloatingActionButton) findViewById(R.id.fab_right_side);
        fab_right_side.setBackgroundTintList(ColorStateList.valueOf(ColorHelper.getAccentColor()));
        fab_right_side.setOnClickListener(this);

        fab_lock = (FloatingActionButton) findViewById(R.id.fab_lock);
        fab_lock.setBackgroundTintList(ColorStateList.valueOf(ColorHelper.getAccentColor()));
        fab_lock.setOnClickListener(this);
        if(MyApp.getPref().getBoolean(getString(R.string.pref_hide_lock_button),false)){
            fab_lock.setVisibility(View.GONE);
        }
        if(MyApp.isLocked()){
            findViewById(R.id.border_view).setVisibility(View.VISIBLE);
        }else {
            findViewById(R.id.border_view).setVisibility(View.GONE);
        }

        playerService = MyApp.getService();
        updateUI(false);

        //ask for rating
        AppRater.app_launched(this);

        switch (MyApp.getPref().getInt(getString(R.string.pref_opening_tab),-1)){
            case Constants.OPENING_TAB.ALBUMS:
                viewPager.setCurrentItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.ALBUM);
                break;

            case Constants.OPENING_TAB.ARTIST:
                viewPager.setCurrentItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.ARTIST);
                break;

            case Constants.OPENING_TAB.FOLDER:
                viewPager.setCurrentItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.FOLDER);
                break;

            case Constants.OPENING_TAB.GENRE:
                viewPager.setCurrentItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.GENRE);
                break;

            case Constants.OPENING_TAB.PLAYLIST:
                viewPager.setCurrentItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.PLAYLIST);
                break;

            case Constants.OPENING_TAB.TRACKS:
                viewPager.setCurrentItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.TITLE);
                break;
        }


        t.stop();
        //set color
        firstTimeInfoMange();

    }

    private void firstTimeInfoMange(){
        if(!MyApp.getPref().getBoolean(getString(R.string.pref_lock_button_info_shown),false)){
            showInfo(Constants.FIRST_TIME_INFO.MUSIC_LOCK);
            return;
        }
    }

    private void showInfo(int first_time_info){
        if(first_time_info!=-1){
            switch (first_time_info){
                case Constants.FIRST_TIME_INFO.MUSIC_LOCK:
                    TapTargetView.showFor(this,
                            TapTarget.forView(findViewById(R.id.fab_lock), getString(R.string.music_lock_primary)
                                    , getString(R.string.music_lock_secondary))
                            .outerCircleColorInt(ColorHelper.getPrimaryColor())
                            .outerCircleAlpha(0.9f)
                            .transparentTarget(true)
                            .titleTextColor(R.color.colorwhite)
                            .descriptionTextColor(R.color.colorwhite)
                            .cancelable(true)
                            .drawShadow(true)
                            .tintTarget(true),
                            new TapTargetView.Listener() {
                                @Override
                                public void onTargetClick(TapTargetView view) {
                                    super.onTargetClick(view);
                                    view.dismiss(true);
                                }

                                @Override
                                public void onOuterCircleClick(TapTargetView view) {
                                    super.onOuterCircleClick(view);
                                    view.dismiss(true);
                                }

                                @Override
                                public void onTargetDismissed(TapTargetView view, boolean userInitiated) {
                                    super.onTargetDismissed(view, userInitiated);
                                    showNext();
                                }

                                private void showNext(){
                                    MyApp.getPref().edit().putBoolean(getString(R.string.pref_lock_button_info_shown),true).apply();
                                    showInfo(Constants.FIRST_TIME_INFO.SORTING);
                                }
                            });
                    break;

                case Constants.FIRST_TIME_INFO.SORTING:
                    View menuItemView = findViewById(R.id.action_sort); // SAME ID AS MENU ID
                    if(menuItemView==null){
                        showInfo(Constants.FIRST_TIME_INFO.MINI_PLAYER);
                    }else {
                        TapTargetView.showFor(this,
                                TapTarget.forView(findViewById(R.id.action_sort), getString(R.string.sorting_primary)
                                        , getString(R.string.sorting_secondary))
                                        .outerCircleColorInt(ColorHelper.getPrimaryColor())
                                        .outerCircleAlpha(0.9f)
                                        .transparentTarget(true)
                                        .titleTextColor(R.color.colorwhite)
                                        .descriptionTextColor(R.color.colorwhite)
                                        .drawShadow(true)
                                        .tintTarget(true)  ,
                                new TapTargetView.Listener() {
                                    @Override
                                    public void onTargetClick(TapTargetView view) {
                                        super.onTargetClick(view);
                                        view.dismiss(true);
                                    }

                                    @Override
                                    public void onOuterCircleClick(TapTargetView view) {
                                        super.onOuterCircleClick(view);
                                        view.dismiss(true);
                                    }

                                    @Override
                                    public void onTargetDismissed(TapTargetView view, boolean userInitiated) {
                                        super.onTargetDismissed(view, userInitiated);
                                        showNext();
                                    }

                                    private void showNext(){
                                        showInfo(Constants.FIRST_TIME_INFO.MINI_PLAYER);
                                    }
                                });
                    }

                    break;

                case Constants.FIRST_TIME_INFO.MINI_PLAYER:
                    TapTargetView.showFor(this,
                            TapTarget.forView(findViewById(R.id.album_art_mini_player), getString(R.string.mini_player_primary)
                                    , getString(R.string.mini_player_secondary))
                                    .outerCircleColorInt(ColorHelper.getPrimaryColor())
                                    .outerCircleAlpha(0.9f)
                                    .transparentTarget(true)
                                    .titleTextColor(R.color.colorwhite)
                                    .descriptionTextColor(R.color.colorwhite)
                                    .drawShadow(true)
                                    .tintTarget(true)  ,
                            new TapTargetView.Listener() {
                                @Override
                                public void onTargetClick(TapTargetView view) {
                                    super.onTargetClick(view);
                                }

                                @Override
                                public void onOuterCircleClick(TapTargetView view) {
                                    super.onOuterCircleClick(view);
                                    view.dismiss(true);
                                }

                                @Override
                                public void onTargetDismissed(TapTargetView view, boolean userInitiated) {
                                    super.onTargetDismissed(view, userInitiated);
                                }
                            });
                    break;
            }
        }
    }

    public void updateNavigationMenuItems(){
        Menu menu = navigationView.getMenu();
        menu.findItem(R.id.nav_tracks).setTitle(getString(R.string.nav_tracks)
                + "  (" + MusicLibrary.getInstance().getDataItemsForTracks().size() +")");

        menu.findItem(R.id.nav_albums).setTitle(getString(R.string.nav_albums)
                + "  (" + MusicLibrary.getInstance().getDataItemsForAlbums().size() +")");

        menu.findItem(R.id.nav_artists).setTitle(getString(R.string.nav_artist)
                + "  (" + MusicLibrary.getInstance().getDataItemsArtist().size() +")");

        menu.findItem(R.id.nav_genres).setTitle(getString(R.string.nav_genre)
                + "  (" + MusicLibrary.getInstance().getDataItemsForGenres().size() +")");
    }

    private boolean ValidatePlaylistName(String playlist_name){
        String pattern= "^[a-zA-Z0-9 ]*$";
        if (playlist_name.matches(pattern)){
            if(playlist_name.length()>2) {
                return true;
            }else {
                Toast.makeText(this,"Enter at least 3 characters",Toast.LENGTH_SHORT).show();
                return false;
            }
        }else {
            Toast.makeText(this,"Only alphanumeric characters allowed",Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    //boolean to to let function know if expand is needed for mini player or not
    //in case of resuming activty, no need to expand mini player
    //even when presed back from secondary activity, no need to expand
    private void updateUI(boolean expandNeeded){
        try {
            if (playerService != null) {
                if (playerService.getCurrentTrack() != null) {
                    Uri uri = MusicLibrary.getInstance().getAlbumArtUri(playerService.getCurrentTrack().getAlbumId());

                    //albumArt.setImageDrawable(getResources().getDrawable(R.drawable.ic_batman_1));
                    Glide.with(this)
                            .load(uri)
                            .centerCrop()
                            .signature(new StringSignature(String.valueOf(System.currentTimeMillis())))
                            .override(100,100)
                            .placeholder(R.drawable.ic_batman_1)
                            .crossFade()
                            .into(albumArt);

                    if (playerService.getStatus() == PlayerService.PLAYING) {
                        buttonPlay.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_pause_black_24dp));
                    } else {
                        buttonPlay.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_play_arrow_black_24dp));
                    }
                    songNameMiniPlayer.setText(playerService.getCurrentTrack().getTitle());
                    artistNameMiniPlayer.setText(playerService.getCurrentTrack().getArtist());
                    if(expandNeeded) {
                        ((AppBarLayout) findViewById(R.id.app_bar_layout)).setExpanded(true);
                    }
                }
            } else {
                //this should not happen
                //restart app
                System.exit(0);
            }
        }catch (Exception ignored){

        }
    }

    //intitalize view pager with fragments and tab names
    private void setupViewPager(ViewPager viewPager) {

        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());

        //keep this order in sync with COnstants
        Bundle bundle3=new Bundle();
        bundle3.putInt("status",Constants.FRAGMENT_STATUS.ALBUM_FRAGMENT);
        if(!MyApp.getPref().getBoolean(getString(R.string.pref_album_lib_view),true)) {
            FragmentLibrary musicByAlbumFrag=new FragmentLibrary();
            musicByAlbumFrag.setArguments(bundle3);
            viewPagerAdapter.addFragment(musicByAlbumFrag, "Album");
        }else {
            FragmentAlbumLibrary musicByAlbumFrag = new FragmentAlbumLibrary();
            musicByAlbumFrag.setArguments(bundle3);
            viewPagerAdapter.addFragment(musicByAlbumFrag, "Album");
        }

        Bundle bundle1=new Bundle();
        bundle1.putInt("status",Constants.FRAGMENT_STATUS.TITLE_FRAGMENT);
        FragmentLibrary musicByTitleFrag=new FragmentLibrary();
        musicByTitleFrag.setArguments(bundle1);
        viewPagerAdapter.addFragment(musicByTitleFrag, "Tracks");

        Bundle bundle2=new Bundle();
        bundle2.putInt("status",Constants.FRAGMENT_STATUS.ARTIST_FRAGMENT);
        FragmentLibrary musicByArtistFrag=new FragmentLibrary();
        musicByArtistFrag.setArguments(bundle2);
        viewPagerAdapter.addFragment(musicByArtistFrag, "Artist");

        Bundle bundle4=new Bundle();
        bundle4.putInt("status",Constants.FRAGMENT_STATUS.GENRE_FRAGMENT);
        FragmentLibrary musicByGenreFrag=new FragmentLibrary();
        musicByGenreFrag.setArguments(bundle4);
        viewPagerAdapter.addFragment(musicByGenreFrag, "Genre");

        FragmentPlaylistLibrary playlistFrag = new FragmentPlaylistLibrary();
        viewPagerAdapter.addFragment(playlistFrag, "Playlist");

        FragmentFolderLibrary folderFragment=new FragmentFolderLibrary();
        viewPagerAdapter.addFragment(folderFragment,"Folder");


        viewPager.setAdapter(viewPagerAdapter);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        int count = getSupportFragmentManager().getBackStackEntryCount();

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }else if(count>0) {
            //setTitle(getResources().getString(R.string.app_name));     //to change back title to music player
            findViewById(R.id.mini_player).setVisibility(View.VISIBLE);
            getSupportFragmentManager().popBackStack();
        }//see if current fragment is folder fragmnet, if yes, override onBackPressed with fragments own action
        else if(viewPager.getCurrentItem()==Constants.VIEW_PAGER_FRAGMENT_INDEX.FOLDER) {
            if (viewPagerAdapter.getItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.FOLDER) instanceof FragmentFolderLibrary) {
                Intent intent = new Intent(NOTIFY_BACK_PRESSED);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
        }
        else {
            //finish();
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        if(viewPager.getCurrentItem()==Constants.VIEW_PAGER_FRAGMENT_INDEX.FOLDER
                || viewPager.getCurrentItem()==Constants.VIEW_PAGER_FRAGMENT_INDEX.PLAYLIST) {
            for (int i = 0; i < menu.size(); i++) {
                if (R.id.action_sort == menu.getItem(i).getItemId()
                        ) {
                    menu.getItem(i).setVisible(false);
                }
            }
        }


        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mSearchAction = menu.findItem(R.id.action_search);
        if(isSearchOpened) {
            mSearchAction.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_close_white_24dp));
        }else {
            mSearchAction.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_search_white_48dp));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()){
            case R.id.action_settings:
                finish();
                startActivity(new Intent(this,SettingsActivity.class)
                        .putExtra("launchedFrom",Constants.PREF_LAUNCHED_FROM.MAIN));
                break;

            case R.id.action_search:
                handleSearch();
                break;

            case R.id.action_sort:
                PopupMenu popupMenu;
                View menuItemView = findViewById(R.id.action_sort); // SAME ID AS MENU ID
                if(menuItemView==null){
                     popupMenu = new PopupMenu(this, findViewById(R.id.action_search));
                }else {
                     popupMenu = new PopupMenu(this, menuItemView);
                }
                popupMenu.inflate(R.menu.sort_menu);

                if(viewPager.getCurrentItem()!=Constants.VIEW_PAGER_FRAGMENT_INDEX.TITLE){
                    popupMenu.getMenu().removeItem(R.id.action_sort_size);
                    popupMenu.getMenu().removeItem(R.id.action_sort_by_duration);
                    if(viewPager.getCurrentItem()!=Constants.VIEW_PAGER_FRAGMENT_INDEX.ALBUM){
                        popupMenu.getMenu().removeItem(R.id.action_sort_year);
                    }
                }

                if(viewPager.getCurrentItem()!=Constants.VIEW_PAGER_FRAGMENT_INDEX.ARTIST){
                    popupMenu.getMenu().removeItem(R.id.action_sort_no_of_album);
                    popupMenu.getMenu().removeItem(R.id.action_sort_no_of_tracks);
                }


                if(MyApp.getPref().getInt(getString(R.string.pref_order_by),Constants.SORT_BY.ASC)==Constants.SORT_BY.ASC){
                    popupMenu.getMenu().findItem(R.id.action_sort_asc).setChecked(true);
                }else {
                    popupMenu.getMenu().findItem(R.id.action_sort_asc).setChecked(false);
                }

                switch (viewPager.getCurrentItem()){
                    case Constants.VIEW_PAGER_FRAGMENT_INDEX.ALBUM:
                        currentPageSort = getString(R.string.pref_album_sort_by);
                        break;

                    case Constants.VIEW_PAGER_FRAGMENT_INDEX.ARTIST:
                        currentPageSort = getString(R.string.pref_artist_sort_by);
                        break;

                    case Constants.VIEW_PAGER_FRAGMENT_INDEX.FOLDER:
                        break;

                    case Constants.VIEW_PAGER_FRAGMENT_INDEX.GENRE:
                        currentPageSort = getString(R.string.pref_genre_sort_by);
                        break;

                    case Constants.VIEW_PAGER_FRAGMENT_INDEX.PLAYLIST:
                        break;

                    case Constants.VIEW_PAGER_FRAGMENT_INDEX.TITLE:
                        currentPageSort = getString(R.string.pref_tracks_sort_by);
                        break;
                }

                switch (MyApp.getPref().getInt(currentPageSort,Constants.SORT_BY.NAME)){
                    case Constants.SORT_BY.NAME:
                        popupMenu.getMenu().findItem(R.id.action_sort_name).setChecked(true);
                        break;

                    case Constants.SORT_BY.YEAR:
                        popupMenu.getMenu().findItem(R.id.action_sort_year).setChecked(true);
                        break;

                    case Constants.SORT_BY.SIZE:
                        popupMenu.getMenu().findItem(R.id.action_sort_size).setChecked(true);
                        break;

                    case Constants.SORT_BY.NO_OF_ALBUMS:
                        popupMenu.getMenu().findItem(R.id.action_sort_no_of_album).setChecked(true);
                        break;

                    case Constants.SORT_BY.NO_OF_TRACKS:
                        popupMenu.getMenu().findItem(R.id.action_sort_no_of_tracks).setChecked(true);
                        break;

                    case Constants.SORT_BY.DURATION:
                        popupMenu.getMenu().findItem(R.id.action_sort_by_duration).setChecked(true);
                        break;

                }
                popupMenu.setOnMenuItemClickListener(this);
                popupMenu.show();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    public void refreshLibrary(){
        MusicLibrary.getInstance().RefreshLibrary();
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplication(),"Library Refreshed", Toast.LENGTH_SHORT).show();
                    }
                });
                finish();
                startActivity(new Intent(MainActivity.this,MainActivity.class));
                // notifyAdapters();
                //updateNavigationMenuItems();
            }
        });

    }

    protected void handleSearch(){
        if(isSearchOpened){ //test if the search is open
            if (getSupportActionBar() != null){
                getSupportActionBar().setDisplayShowCustomEnabled(false);
                getSupportActionBar().setDisplayShowTitleEnabled(true);
            }

            //hides the keyboard
            View view = getCurrentFocus();
            if (view == null) {
                view = new View(this);
            }
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

            //add the search icon in the action bar
            mSearchAction.setIcon(ContextCompat.getDrawable(this,R.drawable.ic_search_white_48dp));
            clearSearch();
            searchQuery="";
            findViewById(R.id.mini_player).setVisibility(View.VISIBLE);

            isSearchOpened = false;
        } else { //open the search entry
            findViewById(R.id.mini_player).setVisibility(View.GONE);

            if (getSupportActionBar() != null){
                getSupportActionBar().setDisplayShowCustomEnabled(true); //enable it to display a custom view
                getSupportActionBar().setCustomView(R.layout.search_bar_layout);//add the custom view
                getSupportActionBar().setDisplayShowTitleEnabled(false); //hide the title
            }
            editSearch = (EditText)getSupportActionBar().getCustomView().findViewById(R.id.edtSearch); //the text editor
            editSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery=String.valueOf(s).toLowerCase();
                    searchAdapters(searchQuery);
                }
            });
            editSearch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    imm.showSoftInput(editSearch, InputMethodManager.SHOW_IMPLICIT);
                }
            });

            editSearch.requestFocus();

            //open the keyboard focused in the edtSearch
            imm.showSoftInput(editSearch, InputMethodManager.SHOW_IMPLICIT);

            mSearchAction.setIcon(ContextCompat.getDrawable(this,R.drawable.ic_close_white_24dp));
            //add the close icon
            //mSearchAction.setIcon(getResources().getDrawable(R.drawable.cancel));
            isSearchOpened = true;
        }
    }

    private void clearSearch(){
        ((FragmentLibrary)viewPagerAdapter.getItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.ARTIST))
                .filter("");
        if(!MyApp.getPref().getBoolean(getString(R.string.pref_album_lib_view),true)) {
            ((FragmentLibrary)viewPagerAdapter.getItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.ALBUM))
                    .filter("");
        }else {
            ((FragmentAlbumLibrary)viewPagerAdapter.getItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.ALBUM))
                    .filter("");
        }
        ((FragmentLibrary)viewPagerAdapter.getItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.TITLE))
                .filter("");
        ((FragmentLibrary)viewPagerAdapter.getItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.GENRE))
                .filter("");
    }

    private void notifyAdapters(){
        ((FragmentLibrary)viewPagerAdapter.getItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.ARTIST))
                .initializeAdapter(Constants.FRAGMENT_STATUS.ARTIST_FRAGMENT);
        if(!MyApp.getPref().getBoolean(getString(R.string.pref_album_lib_view),true)) {
            ((FragmentLibrary)viewPagerAdapter.getItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.ALBUM))
                    .initializeAdapter(Constants.FRAGMENT_STATUS.ALBUM_FRAGMENT);
        }else {
            ((FragmentAlbumLibrary)viewPagerAdapter.getItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.ALBUM))
                    .filter("");
        }
        ((FragmentLibrary)viewPagerAdapter.getItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.TITLE))
                .initializeAdapter(Constants.FRAGMENT_STATUS.TITLE_FRAGMENT);
        ((FragmentLibrary)viewPagerAdapter.getItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.GENRE))
                .initializeAdapter(Constants.FRAGMENT_STATUS.GENRE_FRAGMENT);
    }

    private void searchAdapters(String searchQuery){
        if(viewPagerAdapter.getItem(viewPager.getCurrentItem()) instanceof FragmentAlbumLibrary){
            ((FragmentAlbumLibrary) viewPagerAdapter.getItem(viewPager.getCurrentItem()))
                    .filter(String.valueOf(searchQuery));
        }else if(viewPagerAdapter.getItem(viewPager.getCurrentItem()) instanceof FragmentLibrary){
            ((FragmentLibrary) viewPagerAdapter.getItem(viewPager.getCurrentItem()))
                    .filter(String.valueOf(searchQuery));
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_albums) {
            // Handle the camera action
            viewPager.setCurrentItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.ALBUM);
        } else if (id == R.id.nav_artists) {
            viewPager.setCurrentItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.ARTIST);
        } else if (id == R.id.nav_tracks) {
            viewPager.setCurrentItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.TITLE);
        } else if (id == R.id.nav_genres) {
            viewPager.setCurrentItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.GENRE);
        } else if (id == R.id.nav_folders) {
            viewPager.setCurrentItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.FOLDER);
        } else if (id == R.id.nav_playlists) {
            viewPager.setCurrentItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.PLAYLIST);
        } else if(id==R.id.nav_settings){
            finish();
            startActivity(new Intent(this,SettingsActivity.class)
                    .putExtra("launchedFrom",Constants.PREF_LAUNCHED_FROM.DRAWER));
        } else if(id==R.id.nav_sleep_timer){
            setSleepTimerDialog(this);
        } else if(id==R.id.nav_share){
            try {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                String sAux = "\nLet me recommend you this application\n\n";
                sAux = sAux + getString(R.string.share_app) + " \n\n";
                i.putExtra(Intent.EXTRA_TEXT, sAux);
                startActivity(Intent.createChooser(i, "choose one"));
            } catch(Exception e) {
                //e.toString();
            }
        } else if(id==R.id.nav_rate){
            setRateDialog();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setRateDialog(){
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Rate Us!");
        LinearLayout linear = new LinearLayout(this);
        linear.setOrientation(LinearLayout.VERTICAL);
        final TextView text = new TextView(this);
        text.setText(getString(R.string.rate_us));

        text.setPadding(20, 10,20,10);
        text.setTextSize(16);
        //text.setGravity(Gravity.CENTER);

        LinearLayout ratingWrap = new LinearLayout(this);
        ratingWrap.setOrientation(LinearLayout.VERTICAL);
        ratingWrap.setGravity(Gravity.CENTER);

        RatingBar ratingBar = new RatingBar(this);
        ratingBar.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
       //ratingBar.setNumStars(5);
        ratingBar.setRating(5);
        ratingWrap.addView(ratingBar);

        linear.addView(text);
        linear.addView(ratingWrap);
        alert.setView(linear);
        alert.setPositiveButton("Rate now!", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //RescanLibrary();
                final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });
        alert.show();
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
                    ((MainActivity) context).setSleepTimer(0,false);
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
                    ((MainActivity) context).setSleepTimer(seek.getProgress(),true);
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

    private void setSleepTimer(int minutes, boolean enable){
        playerService.setSleepTimer(minutes, enable);
    }

    @Override
    protected void onStart() {
        /*
        try {
            Intent playerServiceIntent = new Intent(this, PlayerService.class);
            bindService(playerServiceIntent, playerServiceConnection, Context.BIND_AUTO_CREATE);
        }catch (Exception e){

        }*/

        super.onStart();
    }

    @Override
    protected void onStop() {
        try {
            if(mBound) {
                unbindService(playerServiceConnection);
                mBound=false;
            }
        }catch (Exception ignored){

        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.v("TAG","Main activity getting destroyed");
       /* mAudioManager.unregisterMediaButtonEventReceiver(
                mRemoteControlResponder);*/
        mHandler.removeCallbacksAndMessages(null);
        viewPager.clearOnPageChangeListeners();
        viewPager = null;
        viewPagerAdapter = null;
        navigationView.setNavigationItemSelectedListener(null);
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        Intent notificationIntent=new Intent(getApplicationContext(),PlayerService.class);
        switch (view.getId()){
            case R.id.mini_player:
                Intent intent=new Intent(getApplicationContext(),NowPlayingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                Log.v(Constants.TAG,"Launch now playing Jarvis");
                break;

            case R.id.play_pause_mini_player:
                ColorSwitchRunnableForImageView colorSwitchRunnablePlay = new ColorSwitchRunnableForImageView((ImageView) view);
                mHandler.post(colorSwitchRunnablePlay);
                if(playerService.getCurrentTrack()==null) {
                    Toast.makeText(this,"Nothing to play!",Toast.LENGTH_LONG).show();
                    return;
                }

                if (SystemClock.elapsedRealtime() - mLastClickTime < 300){
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                playerService.play();
                //playerService.PostNotification(true);

                if (playerService.getStatus() == PlayerService.PLAYING) {
                    buttonPlay.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_pause_black_24dp));
                } else {
                    buttonPlay.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_play_arrow_black_24dp));
                }
                /*
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent()
                        .setAction(Constants.ACTION.PLAY_PAUSE_ACTION));*/
                break;

            case R.id.next_mini_plaayrer:
                ColorSwitchRunnableForImageView colorSwitchRunnableNext = new ColorSwitchRunnableForImageView((ImageView) view);
                mHandler.post(colorSwitchRunnableNext);
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                playerService.nextTrack();
                //no need to expand mini player
                updateUI(false);
                /*
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent()
                        .setAction(Constants.ACTION.NEXT_ACTION));*/
                Log.v(Constants.TAG,"next track please Jarvis");
                break;

            case R.id.fab_lock:
                if(MyApp.isLocked()){
                    MyApp.setLocked(false);
                    fab_lock.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_lock_open_black_24dp));
                    findViewById(R.id.border_view).setVisibility(View.GONE);
                }else {
                    findViewById(R.id.border_view).setVisibility(View.VISIBLE);
                    fab_lock.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_lock_outline_black_24dp));
                    MyApp.setLocked(true);
                }
                Animation shake1 = AnimationUtils.loadAnimation(this, R.anim.shake_animation);
                fab_lock.startAnimation(shake1);
                lockInfoDialog();
                break;

            case R.id.fab_right_side:
                if(viewPager.getCurrentItem()==Constants.VIEW_PAGER_FRAGMENT_INDEX.PLAYLIST){
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Enter playlist name");
                    final EditText input = new EditText(MainActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    builder.setView(input);
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String playlist_name = input.getText().toString().trim();
                            if(ValidatePlaylistName(playlist_name)) {
                                if(PlaylistManager.getInstance(MainActivity.this).CreatePlaylist(playlist_name)) {
                                    ((FragmentPlaylistLibrary)viewPagerAdapter
                                            .getItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.PLAYLIST))
                                            .refreshPlaylistList();
                                    Toast.makeText(MainActivity.this, "Playlist created", Toast.LENGTH_SHORT).show();
                                }else {
                                    Toast.makeText(MainActivity.this, "Playlist already exists", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();
                }else {
                    if(MyApp.isLocked()){
                        Toast.makeText(MainActivity.this,"Music is Locked!",Toast.LENGTH_SHORT).show();
                        return ;
                    }
                    if (playerService.getTrackList().size() > 0) {
                        playerService.shuffleAll();
                    } else {
                        Toast.makeText(MainActivity.this, "Empty Track List", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        MyApp.isAppVisible = false;
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiverForMiniPLayerUpdate);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiverForLibraryRefresh);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v("get intent",getIntent().getIntExtra("move_to_tab",1000)+"");
        MyApp.isAppVisible = true;
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiverForMiniPLayerUpdate
                ,new IntentFilter(Constants.ACTION.UI_UPDATE));
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiverForLibraryRefresh
                ,new IntentFilter(Constants.ACTION.REFRESH_LIB));
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        updateUI(false);
    }

    public void hideFab(boolean hide){
        if(hide && fab_right_side.isShown()) {
            fab_right_side.hide();
            if(!MyApp.getPref().getBoolean(getString(R.string.pref_hide_lock_button),false)) {
                fab_lock.hide();
            }
        }else {
            fab_right_side.show();
            if(!MyApp.getPref().getBoolean(getString(R.string.pref_hide_lock_button),false)) {
                fab_lock.show();
            }
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                playerService.play();
                updateUI(false);
                break;

            case KeyEvent.KEYCODE_MEDIA_NEXT:
                playerService.nextTrack();
                updateUI(false);
                break;

            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                playerService.prevTrack();
                updateUI(false);
                break;

            case KeyEvent.KEYCODE_MEDIA_STOP:
                playerService.stop();
                updateUI(false);
                break;

            case KeyEvent.KEYCODE_BACK:
                onBackPressed();
                break;
        }

        return false;
    }

    @Override
    public void onRefresh() {
        refreshLibrary();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        int sort_id = MyApp.getPref().getInt(currentPageSort,Constants.SORT_BY.NAME);
        switch (item.getItemId()){
            case  R.id.action_sort_name:
                MyApp.getPref().edit().putInt(currentPageSort,Constants.SORT_BY.NAME).apply();
               // ((FragmentAlbumLibrary)viewPagerAdapter.getItem(Constants.VIEW_PAGER_FRAGMENT_INDEX.ALBUM))
                //        .sort(Constants.SORT_BY.NAME);
                sort_id = Constants.SORT_BY.NAME;
                break;

            case R.id.action_sort_year:
                MyApp.getPref().edit().putInt(currentPageSort,Constants.SORT_BY.YEAR).apply();
                sort_id = Constants.SORT_BY.YEAR;
                break;

            case R.id.action_sort_size:
                MyApp.getPref().edit().putInt(currentPageSort,Constants.SORT_BY.SIZE).apply();
                sort_id = Constants.SORT_BY.SIZE;
                break;

            case R.id.action_sort_no_of_album:
                MyApp.getPref().edit().putInt(currentPageSort,Constants.SORT_BY.NO_OF_ALBUMS).apply();
                sort_id = Constants.SORT_BY.NO_OF_ALBUMS;
                break;

            case R.id.action_sort_no_of_tracks:
                MyApp.getPref().edit().putInt(currentPageSort,Constants.SORT_BY.NO_OF_TRACKS).apply();
                sort_id = Constants.SORT_BY.NO_OF_TRACKS;
                break;

            case R.id.action_sort_by_duration:
                MyApp.getPref().edit().putInt(currentPageSort,Constants.SORT_BY.DURATION).apply();
                sort_id = Constants.SORT_BY.DURATION;
                break;

            case R.id.action_sort_asc:
                if(!item.isChecked()) {
                    MyApp.getPref().edit().putInt(getString(R.string.pref_order_by), Constants.SORT_BY.ASC).apply();

                }else {
                    MyApp.getPref().edit().putInt(getString(R.string.pref_order_by), Constants.SORT_BY.DESC).apply();
                }
                break;
        }

        Log.v(Constants.TAG,"view pager item"+viewPager.getCurrentItem()+"");

        if(viewPagerAdapter.getItem(viewPager.getCurrentItem()) instanceof  FragmentAlbumLibrary){
            ((FragmentAlbumLibrary) viewPagerAdapter.getItem(viewPager.getCurrentItem())).sort(sort_id);
        }else {
            ((FragmentLibrary) viewPagerAdapter.getItem(viewPager.getCurrentItem())).sort(sort_id);
        }
        return true;
    }

    private void lockInfoDialog(){
        if(!MyApp.getPref().getBoolean(getString(R.string.pref_show_lock_info_dialog),true)){
            return;
        }
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Music Lock!");
        LinearLayout linear = new LinearLayout(this);
        linear.setOrientation(LinearLayout.VERTICAL);
        final TextView text = new TextView(this);
        text.setText(getString(R.string.lock_info));
        text.setTypeface(null, Typeface.BOLD);
        text.setPadding(20, 10,20,0);
        text.setTextSize(15);

        linear.addView(text);
        alert.setView(linear);
        alert.setPositiveButton("Got it!", null);

        alert.setNegativeButton("Hide this button", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                MyApp.getPref().edit().putBoolean(getString(R.string.pref_hide_lock_button),true).apply();
                fab_lock.hide();
                MyApp.setLocked(false);
                findViewById(R.id.border_view).setVisibility(View.GONE);
            }
        });

        alert.setNeutralButton("Never show again", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                MyApp.getPref().edit().putBoolean(getString(R.string.pref_show_lock_info_dialog),false).apply();
            }
        });

        alert.show();
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
                v.setColorFilter(ColorHelper.getPrimaryColor());
                colorChanged=true;
                mHandler.postDelayed(this,200);
            }else {
                v.setColorFilter(ColorHelper.getColor(R.color.colorwhite));
                colorChanged = false;
            }
        }
    }
}
