package com.music.player.bhandari.m.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.UIElemetHelper.ColorHelper;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.service.PlayerService;
import com.music.player.bhandari.m.utils.MusicLibrary;
import com.music.player.bhandari.m.utils.MyApp;

import org.w3c.dom.Text;

import java.util.concurrent.Executors;

/**
 * Created by Amit Bhandari on 1/27/2017.
 */

public class SettingsActivity extends AppCompatActivity {

    private  int launchedFrom = 0;

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
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

        super.onCreate(savedInstanceState);
        launchedFrom = getIntent().getIntExtra("launchedFrom",0);
        setContentView(R.layout.acitivty_settings);

        findViewById(R.id.root_view_settings).setBackgroundDrawable(ColorHelper.getBaseThemeDrawable());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_);
        setSupportActionBar(toolbar);

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ColorHelper.getPrimaryColor()));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ColorHelper.getDarkPrimaryColor());
        }

        setTitle("Settings");

        getFragmentManager().beginTransaction().replace(R.id.linear_layout_fragment, new MyPreferenceFragment()).commit();
    }

    @Override
    public void onBackPressed() {
        switch (launchedFrom){
            case Constants.PREF_LAUNCHED_FROM.MAIN:
                startActivity(new Intent(this, MainActivity.class));
                break;

            case Constants.PREF_LAUNCHED_FROM.DRAWER:
                startActivity(new Intent(this, MainActivity.class));
                break;

            case Constants.PREF_LAUNCHED_FROM.NOW_PLAYING:
                startActivity(new Intent(this, NowPlayingActivity.class));
                break;

            default:
                startActivity(new Intent(this, MainActivity.class));
                break;
        }
        super.onBackPressed();
    }

    @Override
    public void onPause() {
        MyApp.isAppVisible = false;
        super.onPause();
    }

    @Override
    public void onResume() {
        MyApp.isAppVisible = true;
        super.onResume();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class MyPreferenceFragment extends PreferenceFragment {

        final String GRID = "Grid view";
        final String LIST = "List view";

        final String DO_NOTHING = "Do nothing";
        final String LIBRARY_VIEW = "Library View";
        final String DISC_VIEW = "Disc View";

        final String SMALL = "Small";
        final String MEDIUM = "Medium";
        final String BIG = "Big";

        final String PLAY_PAUSE = "Play/Pause Current Track";
        final String NEXT = "Play Next Track";
        final String PREVIOUS = "Play Previous Track";

        final String RED = "RED CARMINE";
        final String GREEN = "GREEN DARTMOUTH";
        final String BLUE = "BLUE CATALINA";
        final String PINK = "PINK CERISE";
        final String YELLOW = "AMBER";
        final String BLACK = "ALL BLACK";
        final String AMBER = "CYBER GRAPE";
        final String BONDI_BLUE = "BONDI BLUE";
        final String BYZANTIUM = "BYZANTIUM";
        final String DARK_SLATE_GRAY = "DARK SLATE GRAY";
        final String ANTIQUE_BRONZE = "ANTIQUE BRONZE";
        final String ANTIQUE_RUBY = "ANTIQUE RUBY";
        final String BLUE_MAGNETA_VIOLET = "BLUE MAGNETA VIOLET";
        final String EGGPLANT = "EGGPLANT";
        final String FRENCH_BISTRE = "FRENCH BISTRE";
        final String DEEP_CHESTNUT = "DEEP CHESTNUT";

        final String RANDOM = "RANDOM SELECT";

        final String DARK = "DARK";
        final String LIGHT = "LIGHT";
        final String GLOSSY = "GLOSSY";

        final String ALBUMS = "Albums";
        final String ARTISTS = "Artists";
        final String TRACKS = "Tracks";
        final String GENRES = "Genres";
        final String PLAYLITS = "Playlists";
        final String FOLDERS = "Folders";

        final String MONOSSPACE = "Monospace";
        final String NORMAL = "Normal";
        final String SANS = "Sans";
        final String SERIF = "Serif";

        //this is for font
        //we are not giving anny buttons, so no way to dismiss
        //so we wll use this object to dismiss dialog
        private AlertDialog dialog;

        @Override
        public void onCreate(final Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);


            //base Theme
            Preference baseTheme = findPreference(getString(R.string.pref_theme));
            int baseThemePref = MyApp.getPref().getInt(getString(R.string.pref_theme),Constants.THEME.LIGHT);
            switch (baseThemePref){

                case Constants.THEME.LIGHT:
                    findPreference(getString(R.string.pref_theme)).setSummary(LIGHT);
                    break;

                case Constants.THEME.DARK:
                    findPreference(getString(R.string.pref_theme)).setSummary(DARK);
                    break;

                case Constants.THEME.GLOSSY:
                    findPreference(getString(R.string.pref_theme)).setSummary(GLOSSY);

            }
            baseTheme.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    //open browser or intent here
                    BaseThemeSelectionDialog();
                    return true;
                }
            });

            //Theme color
            Preference primaryColorPref = findPreference(getString(R.string.pref_theme_color));
            int themePrefRead = MyApp.getPref().getInt(getString(R.string.pref_theme_color),Constants.THEME.BLACK);
            switch (themePrefRead){

                case Constants.THEME.BLACK:
                    findPreference(getString(R.string.pref_theme_color)).setSummary(BLACK);
                    break;

                case Constants.THEME.BLUE_CATALINA:
                    findPreference(getString(R.string.pref_theme_color)).setSummary(BLUE);
                    break;


                case Constants.THEME.RED_CARMINE:
                    findPreference(getString(R.string.pref_theme_color)).setSummary(RED);
                    break;

                case Constants.THEME.AMBER:
                    findPreference(getString(R.string.pref_theme_color)).setSummary(YELLOW);
                    break;

                case Constants.THEME.PINK_CERISE:
                    findPreference(getString(R.string.pref_theme_color)).setSummary(PINK);
                    break;

                case Constants.THEME.GREEN_DARTMOUTH:
                    findPreference(getString(R.string.pref_theme_color)).setSummary(GREEN);
                    break;

                case Constants.THEME.CYBER_GRAPE:
                    findPreference(getString(R.string.pref_theme_color)).setSummary(AMBER);
                    break;

                case Constants.THEME.BONDI_BLUE:
                    findPreference(getString(R.string.pref_theme_color)).setSummary(BONDI_BLUE);
                    break;

                case Constants.THEME.BYZANTIUM:
                    findPreference(getString(R.string.pref_theme_color)).setSummary(BYZANTIUM);
                    break;

                case Constants.THEME.DARK_SLATE_GRAY:
                    findPreference(getString(R.string.pref_theme_color)).setSummary(DARK_SLATE_GRAY);
                    break;

                case Constants.THEME.ANTIQUE_BRONZE:
                    findPreference(getString(R.string.pref_theme_color)).setSummary(ANTIQUE_BRONZE);
                    break;

                case Constants.THEME.ANTIQUE_RUBY:
                    findPreference(getString(R.string.pref_theme_color)).setSummary(ANTIQUE_RUBY);
                    break;

                case Constants.THEME.BLUE_MAGNETA_VIOLET:
                    findPreference(getString(R.string.pref_theme_color)).setSummary(BLUE_MAGNETA_VIOLET);
                    break;

                case Constants.THEME.EGGPLANT:
                    findPreference(getString(R.string.pref_theme_color)).setSummary(EGGPLANT);
                    break;

                case Constants.THEME.FRENCH_BISTRE:
                    findPreference(getString(R.string.pref_theme_color)).setSummary(FRENCH_BISTRE);
                    break;

                case Constants.THEME.DEEP_CHESTNUT:
                    findPreference(getString(R.string.pref_theme_color)).setSummary(DEEP_CHESTNUT);
                    break;

            }
            primaryColorPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    //open browser or intent here
                    PrimarySelectionDialog();
                    return true;
                }
            });

            //text font
            Preference fontPref = findPreference(getString(R.string.pref_text_font));
            int textFontPref = MyApp.getPref().getInt(getString(R.string.pref_text_font),Constants.FONT.MONOSPACE);
            switch (textFontPref){

                case Constants.FONT.MONOSPACE:
                    findPreference(getString(R.string.pref_text_font)).setSummary(MONOSSPACE);
                    break;

                case Constants.FONT.NORMAL:
                    findPreference(getString(R.string.pref_text_font)).setSummary(NORMAL);
                    break;

                case Constants.FONT.SERIF:
                    findPreference(getString(R.string.pref_text_font)).setSummary(SERIF);
                    break;

                case Constants.FONT.SANS:
                    findPreference(getString(R.string.pref_text_font)).setSummary(SANS);
                    break;
            }
            fontPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    //open browser or intent here
                    fontPrefSelectionDialog();
                    return true;
                }
            });

            //lockscrreen albumName art
            CheckBoxPreference lockScreenArt = (CheckBoxPreference)findPreference(getString(R.string.pref_lock_screen_album_Art));
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                lockScreenArt.setEnabled(false);
            }

            lockScreenArt.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                        Toast.makeText(getActivity(),"Feature is only avaiable on lollipop and above!", Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
            });
            lockScreenArt.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if(((boolean) newValue)){
                        MyApp.getPref().edit().putBoolean(getString(R.string.pref_lock_screen_album_Art),true).apply();
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            MyApp.getService().setMediaSessionMetadata(true);
                        }
                    }else {
                        MyApp.getPref().edit().putBoolean(getString(R.string.pref_lock_screen_album_Art),false).apply();
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            MyApp.getService().setMediaSessionMetadata(false);
                        }
                    }
                    return true;
                }
            });

            //shake
            CheckBoxPreference shakeStatus = (CheckBoxPreference)findPreference(getString(R.string.pref_shake));
            shakeStatus.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {

                    if(((boolean) newValue)){
                        MyApp.getPref().edit().putBoolean(getString(R.string.pref_shake),true).apply();
                        PlayerService.setShakeListener(true);
                    }else {
                        MyApp.getPref().edit().putBoolean(getString(R.string.pref_shake),false).apply();
                        PlayerService.setShakeListener(false);
                    }
                    return true;
                }
            });

            //shake
            Preference shakeAction = findPreference(getString(R.string.pref_shake_action));
            int shakeActionRead = MyApp.getPref().getInt(getString(R.string.pref_shake_action),Constants.SHAKE_ACTIONS.NEXT);
            if(shakeActionRead==Constants.SHAKE_ACTIONS.NEXT){
                shakeAction.setSummary(NEXT);
            }else if(shakeActionRead==Constants.SHAKE_ACTIONS.PLAY_PAUSE){
                shakeAction.setSummary(PLAY_PAUSE);
            }else {
                shakeAction.setSummary(PREVIOUS);
            }
            shakeAction.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    //open browser or intent here
                    ShakeActionDialog();
                    return true;
                }
            });

            //remove ads
            Preference removeAdPref = findPreference(getString(R.string.pref_support_dev));
            removeAdPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    selectDonateDialog();
                    return true;
                }
            });

            //hide short clips preference
            Preference hideShortClipsPref = findPreference(getString(R.string.pref_hide_short_clips));
            String summary = String.valueOf(MyApp.getPref().getInt(getString(R.string.pref_hide_short_clips),10)) + " seconds";
            hideShortClipsPref.setSummary(summary);
            hideShortClipsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    //open browser or intent here
                    shortClipDialog();
                    return true;
                }
            });

            Preference hideByStartPref = findPreference(getString(R.string.pref_hide_tracks_starting_with));
            String text1 = MyApp.getPref().getString(getString(R.string.pref_hide_tracks_starting_with_1),"");
            String text2 = MyApp.getPref().getString(getString(R.string.pref_hide_tracks_starting_with_2),"");
            String text3 = MyApp.getPref().getString(getString(R.string.pref_hide_tracks_starting_with_3),"");
            hideByStartPref.setSummary(text1+", "+text2+", "+text3);
            hideByStartPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    //open browser or intent here
                    hideByStartDialog();
                    return true;
                }
            });



            //Disc sizze preference
            Preference discSize = findPreference(getString(R.string.pref_disc_size));
            float discSizeRead = MyApp.getPref().getFloat(getString(R.string.pref_disc_size),Constants.DISC_SIZE.MEDIUM);
            if(discSizeRead==Constants.DISC_SIZE.MEDIUM){
                discSize.setSummary(MEDIUM);
            }else if(discSizeRead==Constants.DISC_SIZE.SMALL){
                discSize.setSummary(SMALL);
            }else {
                discSize.setSummary(BIG);
            }
            discSize.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    //open browser or intent here
                    DiscSizeDialog();
                    return true;
                }
            });

            //opening tab preference
            Preference openingTabPref = findPreference(getString(R.string.pref_opening_tab));
            int openingTab = MyApp.getPref().getInt(getString(R.string.pref_opening_tab),Constants.OPENING_TAB.ALBUMS);
            if(openingTab==Constants.OPENING_TAB.ALBUMS){
                openingTabPref.setSummary(ALBUMS);
            }else if(openingTab==Constants.OPENING_TAB.ARTIST){
                openingTabPref.setSummary(ARTISTS);
            }else if(openingTab==Constants.OPENING_TAB.TRACKS){
                openingTabPref.setSummary(TRACKS);
            }else if(openingTab==Constants.OPENING_TAB.GENRE){
                openingTabPref.setSummary(GENRES);
            }else if(openingTab==Constants.OPENING_TAB.FOLDER){
                openingTabPref.setSummary(FOLDERS);
            }else if(openingTab==Constants.OPENING_TAB.PLAYLIST){
                openingTabPref.setSummary(PLAYLITS);
            }
            openingTabPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    //open browser or intent here
                    openingTabDialog();
                    return true;
                }
            });

            //albumName view  preference
            Preference albumViewPref = findPreference(getString(R.string.pref_album_lib_view));
            if(MyApp.getPref().getBoolean(getString(R.string.pref_album_lib_view),true)){
                albumViewPref.setSummary(GRID);
            }else {
                albumViewPref.setSummary(LIST);
            }
            albumViewPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    //open browser or intent here
                    AlbumViewDialog();
                    return true;
                }
            });

            //notification click view  preference
            Preference notifClickPref = findPreference(getString(R.string.pref_click_on_notif));
            switch (MyApp.getPref().getInt(getString(R.string.pref_click_on_notif), Constants.CLICK_ON_NOTIF.OPEN_LIBRARY_VIEW)){
                case Constants.CLICK_ON_NOTIF.DO_NOTHING:
                    notifClickPref.setSummary(DO_NOTHING);
                    break;

                case Constants.CLICK_ON_NOTIF.OPEN_DISC_VIEW:
                    notifClickPref.setSummary(DISC_VIEW);
                    break;

                case Constants.CLICK_ON_NOTIF.OPEN_LIBRARY_VIEW:
                    notifClickPref.setSummary(LIBRARY_VIEW);
                    break;
            }

            notifClickPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    //open browser or intent here
                    notifyClickDialog();
                    return true;
                }
            });


            //Rescan library  preference
            Preference rescanLibrary = findPreference(getString(R.string.pref_rescan_lib));
            rescanLibrary.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    //open browser or intent here
                    RescanLibrary();
                    return true;
                }
            });

            //about us  preference
            Preference aboutUs = findPreference(getString(R.string.pref_about_us));
            aboutUs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    //open browser or intent here
                    getActivity().startActivity(new Intent(getActivity(), AboutUsActivity.class));
                    return true;
                }
            });

            //licenses  preference
            Preference licenses = findPreference(getString(R.string.pref_about_licenses));
            licenses.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    //open browser or intent here
                    getActivity().startActivity(new Intent(getActivity(), LicensesActivity.class));
                    return true;
                }
            });

            //licenses  preference
            final Preference resetPref = findPreference(getString(R.string.pref_reset_pref));
            resetPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    resetPrefDialog();
                    return true;
                }
            });
        }

        private void openingTabDialog(){

            final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle(getString(R.string.title_opening_tab));
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.select_dialog_item);
            arrayAdapter.add(ALBUMS);
            arrayAdapter.add(ARTISTS);
            arrayAdapter.add(TRACKS);
            arrayAdapter.add(GENRES);
            arrayAdapter.add(PLAYLITS);
            arrayAdapter.add(FOLDERS);

            alert.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(arrayAdapter.getItem(which).equals(ALBUMS)){
                        MyApp.getPref().edit().putInt(getString(R.string.pref_opening_tab),Constants.OPENING_TAB.ALBUMS).apply();
                        findPreference(getString(R.string.pref_opening_tab)).setSummary(ALBUMS);
                    }else if(arrayAdapter.getItem(which).equals(TRACKS)){
                        MyApp.getPref().edit().putInt(getString(R.string.pref_opening_tab),Constants.OPENING_TAB.TRACKS).apply();
                        findPreference(getString(R.string.pref_opening_tab)).setSummary(TRACKS);
                    }else if(arrayAdapter.getItem(which).equals(ARTISTS)){
                        MyApp.getPref().edit().putInt(getString(R.string.pref_opening_tab),Constants.OPENING_TAB.ARTIST).apply();
                        findPreference(getString(R.string.pref_opening_tab)).setSummary(ARTISTS);
                    }else if(arrayAdapter.getItem(which).equals(GENRES)){
                        MyApp.getPref().edit().putInt(getString(R.string.pref_opening_tab),Constants.OPENING_TAB.GENRE).apply();
                        findPreference(getString(R.string.pref_opening_tab)).setSummary(GENRES);
                    }else if(arrayAdapter.getItem(which).equals(PLAYLITS)){
                        MyApp.getPref().edit().putInt(getString(R.string.pref_opening_tab),Constants.OPENING_TAB.PLAYLIST).apply();
                        findPreference(getString(R.string.pref_opening_tab)).setSummary(PLAYLITS);
                    }else if(arrayAdapter.getItem(which).equals(FOLDERS)){
                        MyApp.getPref().edit().putInt(getString(R.string.pref_opening_tab),Constants.OPENING_TAB.FOLDER).apply();
                        findPreference(getString(R.string.pref_opening_tab)).setSummary(FOLDERS);
                    }
                    //Toast.makeText(getActivity(),"Please restart the app for changes to take effect",Toast.LENGTH_LONG).show();
                }
            });

            alert.show();
        }

        private void selectDonateDialog() {
            final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle("Support Development!");
            LinearLayout linear = new LinearLayout(getActivity());
            linear.setOrientation(LinearLayout.VERTICAL);
            final TextView text = new TextView(getActivity());
            text.setText("If you enjoy using " + getActivity().getString(R.string.app_name)
                    + ", please consider supporting the development by buying me a Coffee or something. Thanks for your support!");
            text.setTypeface(null, Typeface.BOLD);
            text.setPadding(20, 10,20,0);
            text.setTextSize(15);

            linear.addView(text);
            alert.setView(linear);
            alert.setPositiveButton("Coffee", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    //RescanLibrary();
                    Intent intent = new Intent(getActivity(),RemoveAdsActivity.class);
                    intent.putExtra("donate_type",Constants.DONATE.COFFEE);
                    startActivity(intent);
                }
            });

            alert.setNegativeButton("Beer", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Intent intent = new Intent(getActivity(),RemoveAdsActivity.class);
                    intent.putExtra("donate_type",Constants.DONATE.BEER);
                    startActivity(intent);
                }
            });

            alert.setNeutralButton("Beer Box", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Intent intent = new Intent(getActivity(),RemoveAdsActivity.class);
                    intent.putExtra("donate_type",Constants.DONATE.BEER_BOX);
                    startActivity(intent);
                }
            });

            alert.show();
        }

        private void notifyClickDialog(){
            final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle(getString(R.string.title_click_on_notif));
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.select_dialog_item);
            arrayAdapter.add(LIBRARY_VIEW);
            arrayAdapter.add(DISC_VIEW);
            arrayAdapter.add(DO_NOTHING);

            alert.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(arrayAdapter.getItem(which).equals(LIBRARY_VIEW)){
                        MyApp.getPref().edit().putInt(getString(R.string.pref_click_on_notif)
                                ,Constants.CLICK_ON_NOTIF.OPEN_LIBRARY_VIEW).apply();
                        findPreference(getString(R.string.pref_click_on_notif)).setSummary(LIBRARY_VIEW);
                    } else if(arrayAdapter.getItem(which).equals(DISC_VIEW)){
                        MyApp.getPref().edit().putInt(getString(R.string.pref_click_on_notif)
                                ,Constants.CLICK_ON_NOTIF.OPEN_DISC_VIEW).apply();
                        findPreference(getString(R.string.pref_click_on_notif)).setSummary(DISC_VIEW);
                    }
                    else {
                        MyApp.getPref().edit().putInt(getString(R.string.pref_click_on_notif)
                                ,Constants.CLICK_ON_NOTIF.DO_NOTHING).apply();
                        findPreference(getString(R.string.pref_click_on_notif)).setSummary(DO_NOTHING);
                    }
                    Toast.makeText(getActivity(),"Please restart the app by clearing from tasks for reflecting the changes", Toast.LENGTH_LONG).show();
                    /*Intent i = getActivity().getPackageManager()
                            .getLaunchIntentForPackage( getActivity().getPackageName() );
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);*/
                }
            });

            alert.show();
        }

        private void RescanLibrary(){
            MusicLibrary.getInstance().RefreshLibrary();
            final ProgressDialog dialog = ProgressDialog.show(getActivity(), "",
                    "Rescanning library. Please wait...", true);
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    dialog.dismiss();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(),"Library Refreshed", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }

        private void AlbumViewDialog(){

            final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle(getString(R.string.title_album_lib_view));
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.select_dialog_item);
            arrayAdapter.add(GRID);
            arrayAdapter.add(LIST);

            alert.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(arrayAdapter.getItem(which).equals(GRID)){
                        MyApp.getPref().edit().putBoolean(getString(R.string.pref_album_lib_view),true).apply();
                        findPreference(getString(R.string.pref_album_lib_view)).setSummary(GRID);
                    }else {
                        MyApp.getPref().edit().putBoolean(getString(R.string.pref_album_lib_view),false).apply();
                        findPreference(getString(R.string.pref_album_lib_view)).setSummary(LIST);
                    }
                    //Toast.makeText(getActivity(),"Please restart the app for changes to take effect",Toast.LENGTH_LONG).show();
                }
            });

            alert.show();
        }

        private void shortClipDialog() {
            final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

            alert.setTitle(getString(R.string.title_hide_short_clips));


            LinearLayout linear = new LinearLayout(getActivity());

            linear.setOrientation(LinearLayout.VERTICAL);
            final TextView text = new TextView(getActivity());
            String summary = String.valueOf(MyApp.getPref().getInt(getString(R.string.pref_hide_short_clips),10)) + " seconds";
            text.setText(summary);
            text.setPadding(0, 10,0,0);
            text.setGravity(Gravity.CENTER);

            SeekBar seek = new SeekBar(getActivity());
            seek.setPadding(40,10,40,10);
            seek.setMax(100);
            seek.setProgress(MyApp.getPref().getInt(getString(R.string.pref_hide_short_clips),10));

            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    text.setText(progress+" seconds");
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int progress = seekBar.getProgress();
                    MyApp.getPref().edit().putInt(getString(R.string.pref_hide_short_clips),progress).apply();
                    findPreference(getString(R.string.pref_hide_short_clips)).setSummary(progress+ " seconds");
                }
            });

            linear.addView(seek);
            linear.addView(text);
            alert.setView(linear);
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    RescanLibrary();
                    //Toast.makeText(getActivity(),"Please restart the app for changes to take effect",Toast.LENGTH_LONG).show();
                }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                }
            });

            alert.show();
        }

        private void hideByStartDialog(){
            final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

            alert.setTitle(getString(R.string.title_hide_tracks_starting_with));
            String text1 = MyApp.getPref().getString(getString(R.string.pref_hide_tracks_starting_with_1),"");
            String text2 = MyApp.getPref().getString(getString(R.string.pref_hide_tracks_starting_with_2),"");
            String text3 = MyApp.getPref().getString(getString(R.string.pref_hide_tracks_starting_with_3),"");
            findPreference(getString(R.string.pref_hide_tracks_starting_with)).setSummary(text1+", "+text2+", "+text3);
            LinearLayout linear = new LinearLayout(getActivity());
            linear.setPadding(10,10,10,0);

            final EditText myEditText1 = new EditText(getActivity()); // Pass it an Activity or Context
            myEditText1.setLayoutParams(new LinearLayout.LayoutParams
                    (LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)); // Pass two args; must be LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, or an integer pixel value.
            myEditText1.setText(text1);
            //myEditText1.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            myEditText1.setInputType(InputType.TYPE_CLASS_TEXT);
            myEditText1.setMaxLines(1);
            linear.addView(myEditText1);

            final EditText myEditText2 = new EditText(getActivity()); // Pass it an Activity or Context
            myEditText2.setLayoutParams(new LinearLayout.LayoutParams
                    (LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)); // Pass two args; must be LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, or an integer pixel value.
            myEditText2.setText(text2);
           // myEditText2.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            myEditText2.setMaxLines(1);
            myEditText2.setInputType(InputType.TYPE_CLASS_TEXT);
            linear.addView(myEditText2);

            final EditText myEditText3 = new EditText(getActivity()); // Pass it an Activity or Context
            myEditText3.setLayoutParams(new LinearLayout.LayoutParams
                    (LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)); // Pass two args; must be LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, or an integer pixel value.
            myEditText3.setText(text3);
            //myEditText3.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            myEditText3.setInputType(InputType.TYPE_CLASS_TEXT);
            myEditText3.setMaxLines(1);
            linear.addView(myEditText3);

            TextView tv = new TextView(getActivity());
            tv.setText("Text is case sensitive!");
            linear.addView(tv);

            linear.setOrientation(LinearLayout.VERTICAL);

            alert.setView(linear);
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    String text1 = myEditText1.getText().toString().trim();
                    MyApp.getPref().edit().putString(getString(R.string.pref_hide_tracks_starting_with_1),text1).apply();

                    String text2 = myEditText2.getText().toString().trim();
                    MyApp.getPref().edit().putString(getString(R.string.pref_hide_tracks_starting_with_2),text2).apply();

                    String text3 = myEditText3.getText().toString().trim();
                    MyApp.getPref().edit().putString(getString(R.string.pref_hide_tracks_starting_with_3),text3).apply();

                    findPreference(getString(R.string.pref_hide_tracks_starting_with)).setSummary(text1+", "+text2+", "+text3);

                    RescanLibrary();
                    //Toast.makeText(getActivity(),"Please restart the app for changes to take effect",Toast.LENGTH_LONG).show();
                }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                }
            });

            alert.show();
        }

        private void DiscSizeDialog(){

            final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle(getString(R.string.title_disc_size));
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.select_dialog_item);
            arrayAdapter.add(SMALL);
            arrayAdapter.add(MEDIUM);
            arrayAdapter.add(BIG);

            alert.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(arrayAdapter.getItem(which).equals(MEDIUM)){
                        MyApp.getPref().edit().putFloat(getString(R.string.pref_disc_size)
                                ,Constants.DISC_SIZE.MEDIUM).apply();
                        findPreference(getString(R.string.pref_disc_size)).setSummary(MEDIUM);
                    }else if(arrayAdapter.getItem(which).equals(BIG)){
                        MyApp.getPref().edit().putFloat(getString(R.string.pref_disc_size)
                                ,Constants.DISC_SIZE.BIG).apply();
                        findPreference(getString(R.string.pref_disc_size)).setSummary(BIG);
                    }
                    else {
                        MyApp.getPref().edit().putFloat(getString(R.string.pref_disc_size)
                                ,Constants.DISC_SIZE.SMALL).apply();
                        findPreference(getString(R.string.pref_disc_size)).setSummary(SMALL);
                    }
                   // Toast.makeText(getActivity(),"Please restart the app for changes to take effect",Toast.LENGTH_LONG).show();
                }
            });

            alert.show();
        }

        private void ShakeActionDialog(){
            final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle(getString(R.string.title_shake_action));
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.select_dialog_item);
            arrayAdapter.add(NEXT);
            arrayAdapter.add(PLAY_PAUSE);
            arrayAdapter.add(PREVIOUS);

            alert.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(arrayAdapter.getItem(which).equals(NEXT)){
                        MyApp.getPref().edit().putInt(getString(R.string.pref_shake_action)
                                ,Constants.SHAKE_ACTIONS.NEXT).apply();
                        findPreference(getString(R.string.pref_shake_action)).setSummary(NEXT);
                    }else if(arrayAdapter.getItem(which).equals(PLAY_PAUSE)){
                        MyApp.getPref().edit().putInt(getString(R.string.pref_shake_action)
                                ,Constants.SHAKE_ACTIONS.PLAY_PAUSE).apply();
                        findPreference(getString(R.string.pref_shake_action)).setSummary(PLAY_PAUSE);
                    }
                    else {
                        MyApp.getPref().edit().putInt(getString(R.string.pref_shake_action)
                                ,Constants.SHAKE_ACTIONS.PREVIOUS).apply();
                        findPreference(getString(R.string.pref_shake_action)).setSummary(PREVIOUS);
                    }
                    // Toast.makeText(getActivity(),"Please restart the app for changes to take effect",Toast.LENGTH_LONG).show();
                }
            });

            alert.show();
        }

        private void BaseThemeSelectionDialog(){
            final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle(getString(R.string.title_theme));
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.select_dialog_item);
            arrayAdapter.add(LIGHT);
            arrayAdapter.add(DARK);
            arrayAdapter.add(GLOSSY);
            // arrayAdapter.add(GLOSSY);

            alert.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    switch (arrayAdapter.getItem(which)){

                        case DARK:
                            MyApp.getPref().edit().putInt(getString(R.string.pref_theme)
                                    ,Constants.THEME.DARK).apply();
                            findPreference(getString(R.string.pref_theme)).setSummary(DARK);
                            break;

                        case LIGHT:
                            MyApp.getPref().edit().putInt(getString(R.string.pref_theme)
                                    ,Constants.THEME.LIGHT).apply();
                            findPreference(getString(R.string.pref_theme)).setSummary(LIGHT);
                            break;

                        case GLOSSY:
                            MyApp.getPref().edit().putInt(getString(R.string.pref_theme)
                                    ,Constants.THEME.GLOSSY).apply();
                            findPreference(getString(R.string.pref_theme)).setSummary(GLOSSY);
                            break;
                    }

                    Intent intent = getActivity().getIntent();
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    getActivity().finish();
                    startActivity(intent);
                    //getActivity().finish();

                    // Toast.makeText(getActivity(),"Please restart the app for changes to take effect",Toast.LENGTH_LONG).show();
                }
            });

            alert.show();
        }

        private void PrimarySelectionDialog(){
            final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle(getString(R.string.title_theme_color));
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.select_dialog_item);
            arrayAdapter.add(BLACK);
            arrayAdapter.add(RED);
            arrayAdapter.add(GREEN);
            arrayAdapter.add(BLUE);
            arrayAdapter.add(YELLOW);
            arrayAdapter.add(PINK);
            arrayAdapter.add(AMBER);
            arrayAdapter.add(BONDI_BLUE);
            arrayAdapter.add(BYZANTIUM);
            arrayAdapter.add(DARK_SLATE_GRAY);
            arrayAdapter.add(ANTIQUE_BRONZE);
            arrayAdapter.add(ANTIQUE_RUBY);
            arrayAdapter.add(BLUE_MAGNETA_VIOLET);
            arrayAdapter.add(EGGPLANT);
            arrayAdapter.add(FRENCH_BISTRE);
            arrayAdapter.add(DEEP_CHESTNUT);

            //arrayAdapter.add(RANDOM);

            alert.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    switch (arrayAdapter.getItem(which)){

                        case BLACK:
                            MyApp.getPref().edit().putInt(getString(R.string.pref_theme_color)
                                    ,Constants.THEME.BLACK).apply();
                            findPreference(getString(R.string.pref_theme_color)).setSummary(BLACK);
                            break;

                        case BLUE:
                            MyApp.getPref().edit().putInt(getString(R.string.pref_theme_color)
                                    ,Constants.THEME.BLUE_CATALINA).apply();
                            findPreference(getString(R.string.pref_theme_color)).setSummary(BLUE);
                            break;

                        case RED:
                            MyApp.getPref().edit().putInt(getString(R.string.pref_theme_color)
                                    ,Constants.THEME.RED_CARMINE).apply();
                            findPreference(getString(R.string.pref_theme_color)).setSummary(RED);
                            break;

                        case YELLOW:
                            MyApp.getPref().edit().putInt(getString(R.string.pref_theme_color)
                                    ,Constants.THEME.AMBER).apply();
                            findPreference(getString(R.string.pref_theme_color)).setSummary(YELLOW);
                            break;

                        case PINK:
                            MyApp.getPref().edit().putInt(getString(R.string.pref_theme_color)
                                    ,Constants.THEME.PINK_CERISE).apply();
                            findPreference(getString(R.string.pref_theme_color)).setSummary(PINK);
                            break;

                        case GREEN:
                            MyApp.getPref().edit().putInt(getString(R.string.pref_theme_color)
                                    ,Constants.THEME.GREEN_DARTMOUTH).apply();
                            findPreference(getString(R.string.pref_theme_color)).setSummary(GREEN);
                            break;

                        case AMBER:
                            MyApp.getPref().edit().putInt(getString(R.string.pref_theme_color)
                                    ,Constants.THEME.CYBER_GRAPE).apply();
                            findPreference(getString(R.string.pref_theme_color)).setSummary(AMBER);
                            break;

                        case BONDI_BLUE:
                            MyApp.getPref().edit().putInt(getString(R.string.pref_theme_color)
                                    ,Constants.THEME.BONDI_BLUE).apply();
                            findPreference(getString(R.string.pref_theme_color)).setSummary(BONDI_BLUE);
                            break;

                        case BYZANTIUM:
                            MyApp.getPref().edit().putInt(getString(R.string.pref_theme_color)
                                    ,Constants.THEME.BYZANTIUM).apply();
                            findPreference(getString(R.string.pref_theme_color)).setSummary(BYZANTIUM);
                            break;

                        case DARK_SLATE_GRAY:
                            MyApp.getPref().edit().putInt(getString(R.string.pref_theme_color)
                                    ,Constants.THEME.DARK_SLATE_GRAY).apply();
                            findPreference(getString(R.string.pref_theme_color)).setSummary(DARK_SLATE_GRAY);
                            break;


                        case ANTIQUE_BRONZE:
                            MyApp.getPref().edit().putInt(getString(R.string.pref_theme_color)
                                    ,Constants.THEME.ANTIQUE_BRONZE).apply();
                            findPreference(getString(R.string.pref_theme_color)).setSummary(ANTIQUE_BRONZE);
                            break;

                        case ANTIQUE_RUBY:
                            MyApp.getPref().edit().putInt(getString(R.string.pref_theme_color)
                                    ,Constants.THEME.ANTIQUE_RUBY).apply();
                            findPreference(getString(R.string.pref_theme_color)).setSummary(ANTIQUE_RUBY);
                            break;

                        case BLUE_MAGNETA_VIOLET:
                            MyApp.getPref().edit().putInt(getString(R.string.pref_theme_color)
                                    ,Constants.THEME.BLUE_MAGNETA_VIOLET).apply();
                            findPreference(getString(R.string.pref_theme_color)).setSummary(BLUE_MAGNETA_VIOLET);
                            break;

                        case EGGPLANT:
                            MyApp.getPref().edit().putInt(getString(R.string.pref_theme_color)
                                    ,Constants.THEME.EGGPLANT).apply();
                            findPreference(getString(R.string.pref_theme_color)).setSummary(EGGPLANT);
                            break;

                        case FRENCH_BISTRE:
                            MyApp.getPref().edit().putInt(getString(R.string.pref_theme_color)
                                    ,Constants.THEME.FRENCH_BISTRE).apply();
                            findPreference(getString(R.string.pref_theme_color)).setSummary(FRENCH_BISTRE);
                            break;

                        case DEEP_CHESTNUT:
                            MyApp.getPref().edit().putInt(getString(R.string.pref_theme_color)
                                    ,Constants.THEME.DEEP_CHESTNUT).apply();
                            findPreference(getString(R.string.pref_theme_color)).setSummary(DEEP_CHESTNUT);
                            break;



                    }

                    Intent intent = getActivity().getIntent();
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    getActivity().finish();
                    startActivity(intent);
                    //getActivity().finish();

                    // Toast.makeText(getActivity(),"Please restart the app for changes to take effect",Toast.LENGTH_LONG).show();
                }
            });

            alert.show();
        }

        private void resetPrefDialog(){
            final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

            alert.setTitle(getString(R.string.title_reset_pref) + " ?");
            LinearLayout linear = new LinearLayout(getActivity());

            linear.setOrientation(LinearLayout.VERTICAL);
            alert.setView(linear);
            alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    //Toast.makeText(getActivity(),"Please restart the app for changes to take effect",Toast.LENGTH_LONG).show();
                    MyApp.getPref().edit().putInt(getString(R.string.pref_theme)
                            ,Constants.THEME.GLOSSY).apply();

                    MyApp.getPref().edit().putInt(getString(R.string.pref_theme_color)
                            ,Constants.THEME.PINK_CERISE).apply();

                    MyApp.getPref().edit().putBoolean(getString(R.string.pref_disc_rotation)
                            ,true).apply();

                    MyApp.getPref().edit().putFloat(getString(R.string.pref_disc_size)
                            ,Constants.DISC_SIZE.MEDIUM).apply();

                    MyApp.getPref().edit().putBoolean(getString(R.string.pref_album_lib_view),true).apply();

                    MyApp.getPref().edit().putBoolean(getString(R.string.pref_lock_screen_album_Art),true).apply();

                    MyApp.getPref().edit().putInt(getString(R.string.pref_click_on_notif)
                            ,Constants.CLICK_ON_NOTIF.OPEN_LIBRARY_VIEW).apply();

                    MyApp.getPref().edit().putBoolean(getString(R.string.pref_shake),false).apply();

                    MyApp.getPref().edit().putInt(getString(R.string.pref_hide_short_clips),10).apply();

                    MyApp.getPref().edit().putString(getString(R.string.pref_hide_tracks_starting_with_1),"").apply();
                    MyApp.getPref().edit().putString(getString(R.string.pref_hide_tracks_starting_with_2),"").apply();
                    MyApp.getPref().edit().putString(getString(R.string.pref_hide_tracks_starting_with_3),"").apply();

                    Intent intent = getActivity().getIntent();
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    getActivity().finish();
                    startActivity(intent);
                }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                }
            });

            alert.show();
        }

        private void fontPrefSelectionDialog(){
            final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            dialog = alert.create();

            dialog.setTitle(getString(R.string.title_text_font));

            int currentPref = MyApp.getPref().getInt(getString(R.string.pref_text_font),Constants.FONT.MONOSPACE);
            switch (currentPref){
                case Constants.FONT.MONOSPACE:
                    break;

                case Constants.FONT.NORMAL:
                    break;

                case Constants.FONT.SANS:
                    break;

                case Constants.FONT.SERIF:
                    break;
            }


            LinearLayout linear = new LinearLayout(getActivity());
            linear.setPadding(10,10,10,10);

            final TextView textView1 = new TextView(getActivity()); // Pass it an Activity or Context
            textView1.setLayoutParams(new LinearLayout.LayoutParams
                    (LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)); // Pass two args; must be LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, or an integer pixel value.
            textView1.setText(MONOSSPACE);
            textView1.setPadding(0,20,0,20);
            textView1.setTextSize(20);
            textView1.setTypeface(Typeface.MONOSPACE);
            textView1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MyApp.getPref().edit().putInt(getString(R.string.pref_text_font),Constants.FONT.MONOSPACE).apply();
                    findPreference(getString(R.string.pref_text_font)).setSummary(MONOSSPACE);
                    dialog.dismiss();
                }
            });
            linear.addView(textView1);

            final TextView textView2 = new TextView(getActivity()); // Pass it an Activity or Context
            textView2.setLayoutParams(new LinearLayout.LayoutParams
                    (LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)); // Pass two args; must be LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, or an integer pixel value.
            textView2.setText(NORMAL);
            textView2.setPadding(0,20,0,20);
            textView2.setTextSize(20);
            textView2.setTypeface(Typeface.DEFAULT);
            textView2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MyApp.getPref().edit().putInt(getString(R.string.pref_text_font),Constants.FONT.NORMAL).apply();
                    findPreference(getString(R.string.pref_text_font)).setSummary(NORMAL);
                    dialog.dismiss();
                }
            });
            linear.addView(textView2);

            final TextView textView3 = new TextView(getActivity()); // Pass it an Activity or Context
            textView3.setLayoutParams(new LinearLayout.LayoutParams
                    (LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)); // Pass two args; must be LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, or an integer pixel value.
            textView3.setText(SANS);
            textView3.setTypeface(Typeface.SANS_SERIF);
            textView3.setPadding(0,20,0,20);
            textView3.setTextSize(20);
            textView3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MyApp.getPref().edit().putInt(getString(R.string.pref_text_font),Constants.FONT.SANS).apply();
                    findPreference(getString(R.string.pref_text_font)).setSummary(SANS);
                    dialog.dismiss();
                }
            });
            linear.addView(textView3);

            final TextView textView4 = new TextView(getActivity()); // Pass it an Activity or Context
            textView4.setLayoutParams(new LinearLayout.LayoutParams
                    (LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)); // Pass two args; must be LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, or an integer pixel value.
            textView4.setText(SERIF);
            textView4.setTypeface(Typeface.SERIF);
            textView4.setPadding(0,20,0,20);
            textView4.setTextSize(20);
            textView4.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MyApp.getPref().edit().putInt(getString(R.string.pref_text_font),Constants.FONT.SERIF).apply();
                    findPreference(getString(R.string.pref_text_font)).setSummary(SERIF);
                    dialog.dismiss();
                }
            });
            linear.addView(textView4);

            linear.setOrientation(LinearLayout.VERTICAL);

            dialog.setView(linear);

            dialog.show();
        }
    }

}
