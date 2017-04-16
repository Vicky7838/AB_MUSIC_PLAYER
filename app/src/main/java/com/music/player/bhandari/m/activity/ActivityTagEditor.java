package com.music.player.bhandari.m.activity;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.UIElemetHelper.ColorHelper;
import com.music.player.bhandari.m.UIElemetHelper.FontFactory;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.model.TrackItem;
import com.music.player.bhandari.m.utils.MusicLibrary;
import com.music.player.bhandari.m.utils.MyApp;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Amit Bhandari on 3/2/2017.
 */

public class ActivityTagEditor extends AppCompatActivity implements  View.OnClickListener {

    private EditText title, artist, album, genre;
    private ImageView album_art;
    private String original_title, original_artist, original_album, original_genre;
    private String edited_title = "", edited_artist="", edited_album="", edited_genre="", edited_artwork_file_path="";
    private String file_path;
    private String track_title;
    private final int SAVE=10;
    boolean fChanged=false;
    private TrackItem item;
    private String ALBUM_ART_PATH="";
    //file path where changed image file is stored
    private String new_artwork_path = "";

    private final String GALLERY = "Gallery";
    private final String REMOVE_PHOTO = "Remove Photo";


    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {

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
        setContentView(R.layout.activity_tag_editor);

        //show info dialog
        showInfoDialog();

        ALBUM_ART_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+getString(R.string.album_art_dir_name);

        findViewById(R.id.root_view_tag_editor).setBackgroundDrawable(ColorHelper.getColoredThemeDrawable());

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        //get file path
        file_path = getIntent().getStringExtra("file_path");
        if(file_path==null){
            finish();
        }

        track_title = getIntent().getStringExtra("track_title");
        item = MusicLibrary.getInstance().getTrackItemFromTitle(track_title);

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
        setTitle(getString(R.string.title_tag_editor));

        title = (EditText)findViewById(R.id.title_te);
        title.setTypeface(FontFactory.getFont());

        artist = (EditText)findViewById(R.id.artist_te);
        artist.setTypeface(FontFactory.getFont());

        album = (EditText)findViewById(R.id.album_te);
        album.setTypeface(FontFactory.getFont());

        genre = (EditText)findViewById(R.id.genre_te);
        genre.setTypeface(FontFactory.getFont());
        genre.setVisibility(View.GONE);
        findViewById(R.id.genre_text_te).setVisibility(View.GONE);

        album_art = (ImageView) findViewById(R.id.album_art_te);
        album_art.setOnClickListener(this);

        //get current tags from audio file and populate the fields
        setTagsFromContent();

    }

    private void setTagsFromContent(){
        if(item==null){
            return;
        }
        title.setText(item.getTitle());
        original_title = item.getTitle();

        album.setText(item.getAlbum());
        original_album = item.getAlbum();

        artist.setText(item.getArtist());
        original_artist = item.getArtist();

        //genre.setText(item.getGenre());
        //original_genre = item.getGenre();

        Glide.with(this)
                .load(MusicLibrary.getInstance().getAlbumArtUri(item.getAlbumId()))
                .signature(new StringSignature(String.valueOf(System.currentTimeMillis())))
                .animate(AnimationUtils.loadAnimation(this, R.anim.fade_in))
                .placeholder(R.drawable.ic_batman_1)
                .into(album_art);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.add(0, SAVE , 0, "Save")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                readValues();
                if(fChanged){
                    unsavedDataAlert();
                }else {
                    overridePendingTransition(android.R.anim.slide_in_left,android.R.anim.slide_out_right);
                    finish();
                }
                break;

            case SAVE:
                readValues();
                if(fChanged){
                    try {
                        save();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this,"Error while saving tags!",Toast.LENGTH_LONG).show();
                    }
                    Log.v(Constants.TAG,edited_title);
                    Log.v(Constants.TAG,edited_artist);
                    Log.v(Constants.TAG,edited_album);
                    Log.v(Constants.TAG,edited_genre);
                }else {
                    Intent intent;
                    int launchedFrom = getIntent().getIntExtra("from",Constants.TAG_EDITOR_LAUNCHED_FROM.MAIN_LIB);
                    if(launchedFrom==Constants.TAG_EDITOR_LAUNCHED_FROM.MAIN_LIB) {
                        intent = new Intent(this, MainActivity.class);
                    }else if(launchedFrom == Constants.TAG_EDITOR_LAUNCHED_FROM.NOW_PLAYING){
                        intent = new Intent(this, NowPlayingActivity.class);
                    }else {
                        intent = new Intent(this, SecondaryLibraryActivity.class);
                    }
                    startActivity(intent);
                    finish();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void readValues(){
        edited_title = title.getText().toString();
        edited_artist = artist.getText().toString();
        edited_album = album.getText().toString();
        //edited_genre = genre.getText().toString();
        if(!edited_title.equals(original_title) ||
                !edited_artist.equals(original_artist) ||
                !edited_album.equals(original_album) ||
                //!edited_genre.equals(original_genre) ||
                !new_artwork_path.equals("")){
            fChanged = true;
        }
    }

    private void unsavedDataAlert() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        LinearLayout linear = new LinearLayout(this);
        final TextView text = new TextView(this);
        text.setText(getString(R.string.changes_discard_alert_te));
        text.setTypeface(null, Typeface.BOLD);
        text.setPadding(20, 10,20,0);
        text.setTextSize(15);

        linear.addView(text);
        alert.setView(linear);

        alert.setTitle("Unsaved Data!");

        alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //RescanLibrary();
                try {
                    save();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(ActivityTagEditor.this,"Error while saving tags!",Toast.LENGTH_LONG).show();
                }
                finish();
            }
        });

        alert.setNegativeButton("Discard", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });

        alert.show();
    }

    private void save() {

        if(edited_title.isEmpty() || edited_album.isEmpty() || edited_artist.isEmpty()){
            Toast.makeText(getApplicationContext(),"Cannot leave field empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        //change content in android
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.TITLE, edited_title);
        values.put(MediaStore.Audio.Media.ARTIST, edited_artist);
        values.put(MediaStore.Audio.Media.ALBUM, edited_album);
        getContentResolver().update(uri, values, MediaStore.Audio.Media.TITLE +"=?", new String[] {track_title});
        if(!new_artwork_path.equals("")){
            Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
            int deleted = getContentResolver().delete(ContentUris.withAppendedId(sArtworkUri, item.getAlbumId()), null, null);
            Log.v(Constants.TAG,"delete "+deleted);
            values = new ContentValues();
            values.put("album_id", item.getAlbumId());
            values.put("_data", new_artwork_path);
            getContentResolver().insert(sArtworkUri, values);
        }

        MusicLibrary.getInstance().updateTrack(original_title, edited_title, edited_artist, edited_album);

        Intent intent;
        int launchedFrom = getIntent().getIntExtra("from",Constants.TAG_EDITOR_LAUNCHED_FROM.MAIN_LIB);
        if(launchedFrom==Constants.TAG_EDITOR_LAUNCHED_FROM.MAIN_LIB) {
            intent = new Intent(this, MainActivity.class);
        }else if(launchedFrom == Constants.TAG_EDITOR_LAUNCHED_FROM.NOW_PLAYING){
            intent = new Intent(this, NowPlayingActivity.class);
        }else {
            intent = new Intent(this, SecondaryLibraryActivity.class);
        }
            intent.putExtra("refresh", true);
            intent.putExtra("position", getIntent().getIntExtra("position", -1));
            intent.putExtra("originalTitle",original_title);
            intent.putExtra("title", edited_title);
            intent.putExtra("artist", edited_artist);
            intent.putExtra("album", edited_album);
        startActivity(intent);
        overridePendingTransition(android.R.anim.slide_in_left,android.R.anim.slide_out_right);
        finish();
    }

    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.album_art_te){
            pickImage();
        }
    }

    public void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    private void deletePhoto(){
        if(album_art!=null){
            album_art.setImageDrawable(getResources().getDrawable(R.drawable.ic_batman_1));
        }
        Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
        getContentResolver().delete(ContentUris.withAppendedId(sArtworkUri, item.getAlbumId()), null, null);
        String customAlbumArt = Environment.getExternalStorageDirectory().getAbsolutePath()
                +"/"+getString(R.string.album_art_dir_name)+"/"
                +item.getAlbumId();
        File f = new File(customAlbumArt);
        if(f.exists()){
            try {
                f.delete();
            }catch (Exception e){

            }
        }
    }

    public static void dumpIntent(Intent i){

        Bundle bundle = i.getExtras();
        if (bundle != null) {
            Set<String> keys = bundle.keySet();
            Iterator<String> it = keys.iterator();
            Log.e(Constants.TAG,"Dumping Intent start");
            while (it.hasNext()) {
                String key = it.next();
                Log.e(Constants.TAG,"[" + key + "=" + bundle.get(key)+"]");
            }
            Log.e(Constants.TAG,"Dumping Intent end");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == 1) {
            //dumpIntent(data);
            checkAndCreateAlbumArtDirectory();
            Uri uri = data.getData();
            Log.v(Constants.TAG,data.toString());
            String file_path_artwprk = getRealPathFromURI(uri);
            Glide.with(this)
                    .load(new File(file_path_artwprk)) // Uri of the picture
                    .into(album_art);
            new_artwork_path = file_path_artwprk;

            try {

                /*new_artwork_path = Environment.getExternalStorageDirectory().getAbsolutePath()
                        +"/"+getString(R.string.album_art_dir_name)+"/"
                        +item.getAlbumId();
                FileOutputStream out = new FileOutputStream(new_artwork_path);
                bmp.compress(Bitmap.CompressFormat.JPEG, 70, out);
                if(new File(new_artwork_path).exists())
                Log.v(Constants.TAG,"File saved");*/
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void checkAndCreateAlbumArtDirectory(){
        File f = new File(ALBUM_ART_PATH);
        if(f.exists()){
            return;
        }
        try {
            f.mkdir();
        }catch (Exception ignored){

        }
    }

    public String getRealPathFromURI(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        @SuppressWarnings("deprecation")
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
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
    public void onBackPressed() {
        readValues();
        if(fChanged){
            unsavedDataAlert();
        }else {
            finish();
            overridePendingTransition(android.R.anim.slide_in_left,android.R.anim.slide_out_right);
        }
    }

    private void showInfoDialog(){
        if(!MyApp.getPref().getBoolean(getString(R.string.pref_show_edit_track_info_dialog),true)){
            return;
        }
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Alert!");
        LinearLayout linear = new LinearLayout(this);
        linear.setOrientation(LinearLayout.VERTICAL);
        final TextView text = new TextView(this);
        text.setText(getString(R.string.tag_edit_info));
        text.setTypeface(null, Typeface.BOLD);
        text.setPadding(20, 10,20,0);
        text.setTextSize(16);

        linear.addView(text);
        alert.setView(linear);
        alert.setNegativeButton("Got it!", null);

        alert.setPositiveButton("Never show again", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                MyApp.getPref().edit().putBoolean(getString(R.string.pref_show_edit_track_info_dialog),false).apply();
            }
        });

        alert.show();
    }
}
