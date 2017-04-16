package com.music.player.bhandari.m.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.annotation.AnyRes;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.model.TrackItem;
import com.music.player.bhandari.m.model.dataItem;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by amit on 29/11/16.
 */


//singleton class
    //maintains music library

public class MusicLibrary{

    private Context context;
    private static MusicLibrary musicLibrary;
    private ContentResolver cr;
    private AtomicInteger atomicInt = new AtomicInteger();
    private int libraryLoadCounter;

    //short clip time
    private int SHORT_CLIPS_TIME_IN_MS;
    private String REMOVE_TRACK_CONTAINING_1, REMOVE_TRACK_CONTAINING_2, REMOVE_TRACK_CONTAINING_3;

    //all the folders in which songs are there
    private ArrayList<String> foldersList=new ArrayList<>();

    //data for all frgaments
    private ArrayList<dataItem> dataItemsForTracks = new ArrayList<>();
    private ArrayList<dataItem> dataItemsForAlbums = new ArrayList<>();
    private ArrayList<dataItem> dataItemsForGenres = new ArrayList<>();
    private ArrayList<dataItem> dataItemsForArtists = new ArrayList<>();

    private MusicLibrary(){
        this.context=MyApp.getContext();
        this.cr = context.getContentResolver();
        RefreshLibrary();
    }

    public void RefreshLibrary(){
        //filter audio based on track duration
        SHORT_CLIPS_TIME_IN_MS = MyApp.getPref().getInt(context.getString(R.string.pref_hide_short_clips),10)*1000;

        //filter audio based on name
        REMOVE_TRACK_CONTAINING_1 = MyApp.getPref().getString(context.getString(R.string.pref_hide_tracks_starting_with_1),"");
        REMOVE_TRACK_CONTAINING_2 = MyApp.getPref().getString(context.getString(R.string.pref_hide_tracks_starting_with_2),"");
        REMOVE_TRACK_CONTAINING_3 = MyApp.getPref().getString(context.getString(R.string.pref_hide_tracks_starting_with_3),"");

        atomicInt.set(0);
        dataItemsForTracks.clear();
        dataItemsForGenres.clear();
        dataItemsForAlbums.clear();
        dataItemsForArtists.clear();
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                Log.v(Constants.TAG,"refresh started");
                fillDataForTracks();
                fillDataForAlbums();
                fillDataForArtist();
                fillDataForGenre();
                while (libraryLoadCounter!=4){
                    //Log.v(Constants.TAG,"waiting..");
                }
                atomicInt.set(0);
                libraryLoadCounter=0;
                Log.v(Constants.TAG,"refreshed");
                PlaylistManager.getInstance(MyApp.getContext()).PopulateUserMusicTable();
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.ACTION.REFRESH_LIB));
            }
        });
    }

    private void fillFoldersList(){
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    for (dataItem item : dataItemsForTracks) {
                        String path = item.file_path;
                        path = path.substring(0, path.lastIndexOf("/"));
                        if (!foldersList.contains(path)) {
                            foldersList.add(path);
                        }
                    }
                }catch (Exception ignored){

                }
            }
        });
    }

    public static MusicLibrary getInstance(){
        if (musicLibrary==null){
            musicLibrary = new MusicLibrary();
        }
        return musicLibrary;
    }

    public ArrayList<String> getDefaultTracklist(){
        if(dataItemsForTracks!=null){
            ArrayList<String> tracklist=new ArrayList<>();
            for (dataItem item:dataItemsForTracks){
                tracklist.add(item.title);
            }
            return tracklist;
        }
        return null;
    }

    public ArrayList<String> getFoldersList(){
        return foldersList;
    }

    private void fillDataForTracks(){
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
                String[] projection = {
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST_ID,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ALBUM_ID,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.YEAR
                };
                String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
                Cursor cursor = cr.query(uri, projection, selection, null, sortOrder);
                if(cursor!=null && cursor.getCount()>0) {
                    while (cursor.moveToNext()) {

                        if(!REMOVE_TRACK_CONTAINING_1.equals("")
                                && cursor.getString(INDEX_FOR_TRACK_CURSOR.TITLE).startsWith(REMOVE_TRACK_CONTAINING_1)) {
                            continue;
                        }
                        if(!REMOVE_TRACK_CONTAINING_2.equals("")
                                && cursor.getString(INDEX_FOR_TRACK_CURSOR.TITLE).startsWith(REMOVE_TRACK_CONTAINING_2)) {
                            continue;
                        }
                        if(!REMOVE_TRACK_CONTAINING_3.equals("")
                                && cursor.getString(INDEX_FOR_TRACK_CURSOR.TITLE).startsWith(REMOVE_TRACK_CONTAINING_3)) {
                            continue;
                        }

                        if (cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)) > SHORT_CLIPS_TIME_IN_MS) {
                            dataItemsForTracks.add(new dataItem(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID))
                                    ,cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
                                    ,cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID))
                                    ,cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                                    ,cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
                                    ,cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM))
                                    ,cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.YEAR))
                                    ,cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                                    ,cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)))
                            );
                        }
                    }
                }
                if(cursor!=null) {
                    cursor.close();
                }

                libraryLoadCounter = atomicInt.incrementAndGet();
                fillFoldersList();
            }
        });
    }

    private void fillDataForArtist(){
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                String[] mProjection =
                        {
                                MediaStore.Audio.Artists._ID,
                                MediaStore.Audio.Artists.ARTIST,
                                MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
                                MediaStore.Audio.Artists.NUMBER_OF_ALBUMS
                        };
                Cursor cursor = cr.query(
                        MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                        mProjection,
                        null,
                        null,
                        MediaStore.Audio.Artists.ARTIST + " ASC");
                if(cursor!=null && cursor.getCount()>0) {
                    while (cursor.moveToNext()) {
                        dataItemsForArtists.add(new dataItem(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Artists._ID))
                                ,cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST))
                                ,cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS))
                                ,cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS))
                        ));
                    }
                }
                if(cursor!=null) {
                    cursor.close();
                }
                libraryLoadCounter = atomicInt.incrementAndGet();
            }
        });

    }

    private void fillDataForAlbums(){
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                String[] mProjection =
                        {
                                MediaStore.Audio.Albums._ID,
                                MediaStore.Audio.Albums.ALBUM,
                                MediaStore.Audio.Albums.NUMBER_OF_SONGS,
                                MediaStore.Audio.Albums.ARTIST,
                                MediaStore.Audio.Albums.FIRST_YEAR,
                                MediaStore.Audio.Albums.ALBUM_ART
                        };
                Cursor cursor = cr.query(
                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        mProjection,
                        null,
                        null,
                        MediaStore.Audio.Albums.ALBUM + " ASC");
                if(cursor!=null && cursor.getCount()>0) {
                    while (cursor.moveToNext()) {
                        dataItemsForAlbums.add(new dataItem(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Albums._ID))
                                ,cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM))
                                ,cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST))
                                ,cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS))
                                ,cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.FIRST_YEAR))
                        ));
                    }
                    cursor.close();
                }

                libraryLoadCounter = atomicInt.incrementAndGet();
            }
        });

    }

    private void fillDataForGenre(){
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                String[] mProjection =
                        {
                                MediaStore.Audio.Genres._ID,
                                MediaStore.Audio.Genres.NAME
                        };
                Cursor cursor = cr.query(
                        MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                        mProjection,
                        null,
                        null,
                        MediaStore.Audio.Genres.NAME + " ASC");
                if(cursor!=null && cursor.getCount()>0) {
                    while (cursor.moveToNext()) {
                        if (getSongListFromGenreId(cursor.getInt(MusicLibrary.INDEX_FOR_GENRE_CURSOR._ID)
                                , Constants.SORT_ORDER.ASC).size() == 0)
                            continue;
                        dataItemsForGenres.add(new dataItem(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Genres._ID))
                                ,cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.NAME))
                                ,0)
                        );
                    }
                    cursor.close();
                }
                libraryLoadCounter = atomicInt.incrementAndGet();
            }
        });

    }

    public void updateTrack(String originalTitle, String... param){
        for(dataItem d : getDataItemsForTracks()){
            if(d.title.equals(originalTitle)){
                d.title = param[0];
                d.artist_name = param[1];
                d.albumName = param[2];
            }
        }
    }

    public ArrayList<dataItem> getDataItemsForAlbums(){
        return dataItemsForAlbums;
    }

    public ArrayList<dataItem> getDataItemsArtist(){
        return dataItemsForArtists;
    }

    public ArrayList<dataItem> getDataItemsForTracks(){
        return dataItemsForTracks;
    }

    public ArrayList<dataItem> getDataItemsForGenres(){
        return dataItemsForGenres;
    }

    public ArrayList<String> getSongListFromArtistId(int artist_id, int sort){
        ArrayList<String> songList=new ArrayList<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0" + " AND " + MediaStore.Audio.Media.ARTIST_ID + "=" +artist_id;
        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION
        };
        String sortOrder="";
        if(sort== Constants.SORT_ORDER.ASC) {
            sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        }else{
            sortOrder = MediaStore.Audio.Media.TITLE + " DESC";
        }
        Cursor cursor=cr.query(uri, projection, selection, null, sortOrder);
        if(cursor!=null){
            while (cursor.moveToNext()){
                if(cursor.getInt(1)> SHORT_CLIPS_TIME_IN_MS) {
                    songList.add(cursor.getString(0));
                }
            }
            cursor.close();
            return songList;
        }
        return null;
    }

    public ArrayList<String> getSongListFromAlbumId(int album_id,int sort){
        ArrayList<String> songList=new ArrayList<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0" + " AND " + MediaStore.Audio.Media.ALBUM_ID + "=" +album_id;
        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION
        };
        String sortOrder="";
        if(sort==Constants.SORT_ORDER.ASC) {
            sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        }else{
            sortOrder = MediaStore.Audio.Media.TITLE + " DESC";
        }
        Cursor cursor=cr.query(uri, projection, selection, null, sortOrder);
        if(cursor!=null){
            while (cursor.moveToNext()){
                if(cursor.getInt(1)> SHORT_CLIPS_TIME_IN_MS) {
                    songList.add(cursor.getString(0));
                }
            }
            cursor.close();
            return songList;
        }
        return null;
    }

    public ArrayList<String> getSongListFromGenreId(int genre_id,int sort){
        ArrayList<String> songList=new ArrayList<>();

        Uri uri = MediaStore.Audio.Genres.Members.getContentUri("external", genre_id);
        String[] projection = new String[]{MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION};
        String sortOrder="";
        if(sort==Constants.SORT_ORDER.ASC) {
            sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        }else{
            sortOrder = MediaStore.Audio.Media.TITLE + " DESC";
        }
        Cursor cursor=cr.query(uri, projection, null, null, sortOrder);
        if(cursor!=null){
            while (cursor.moveToNext()){
                if(cursor.getInt(1)> SHORT_CLIPS_TIME_IN_MS) {
                    songList.add(cursor.getString(0));
                }
            }
            cursor.close();
            return songList;
        }
        return null;
    }

    public TrackItem getTrackItemFromTitle(String title){

        if(title.contains("'")){
           //title = ((char)34+title+(char)34);
            //fuck you bug
            //you bugged my mind
           title = title.replaceAll("'","''");
        }
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection =  MediaStore.Audio.Media.IS_MUSIC + "!= 0" + " AND "
                +MediaStore.Audio.Media.TITLE  + "= '" +   title  +"'";
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ARTIST_ID
        };
        Cursor cursor=cr.query(
                uri,
                projection,
                selection,
                null,
                MediaStore.Audio.Media.TITLE + " ASC");

        if(cursor!=null && cursor.getCount()!=0){
            cursor.moveToFirst();
            TrackItem item = new TrackItem(cursor.getString(INDEX_FOR_TRACK_CURSOR.DATA_PATH),
                    cursor.getString(INDEX_FOR_TRACK_CURSOR.TITLE),
                    cursor.getString(INDEX_FOR_TRACK_CURSOR.ARTIST),
                    cursor.getString(INDEX_FOR_TRACK_CURSOR.ALBUM),
                    "",
                    cursor.getString(INDEX_FOR_TRACK_CURSOR.DURATION),
                    cursor.getInt(INDEX_FOR_TRACK_CURSOR.ALBUM_ID),
                    cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)),
                    cursor.getInt(INDEX_FOR_TRACK_CURSOR._ID));
            cursor.close();
            return  item;
        }
        return null;
    }

    public String getTitleFromFilePath(String filePath){
        if(filePath.contains("\"")){
            filePath = UtilityFun.escapeDoubleQuotes(filePath);
        }
        Uri videosUri = MediaStore.Audio.Media.getContentUri("external");
        String[] projection = {MediaStore.Audio.Media.TITLE};
        Cursor cursor = cr.query(videosUri, projection, MediaStore.Audio.Media.DATA + " LIKE ?", new String[] { filePath }, null);
        if(cursor!=null && cursor.getCount()!=0) {
            cursor.moveToFirst();
            String id = "";
            try {
                id = cursor.getString(0);
            } catch (Exception e) {
                cursor.close();
                return null;
            }
            cursor.close();
            return id;
        }else {
            return null;
        }
    }

    public int getIdFromTitle(String title){
        if(title.contains("'")){
            //title = ((char)34+title+(char)34);
            //fuck you bug
            //you bugged my mind
            title = title.replaceAll("'","''");
        }
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection =  MediaStore.Audio.Media.IS_MUSIC + "!= 0" + " AND "
                +MediaStore.Audio.Media.TITLE  + "= '" +   title  +"'";
        String[] projection = {
                MediaStore.Audio.Media._ID
        };
        Cursor cursor=cr.query(
                uri,
                projection,
                selection,
                null,
                MediaStore.Audio.Media.TITLE + " ASC");

        if(cursor!=null){
            cursor.moveToFirst();
            int id = cursor.getInt(INDEX_FOR_TRACK_CURSOR._ID);
            cursor.close();
            return  id;
        }
        return 0;
    }


    public Bitmap getAlbumArtFromTitle(String title){
        if(title.contains("'")){
            //title = ((char)34+title+(char)34);
            //fuck you bug
            //you bugged my mind
            title = title.replaceAll("'","''");
        }
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0" + " AND "
                +MediaStore.Audio.Media.TITLE  + "=" +  "'" + title + "'" ;
        String[] projection = {
                MediaStore.Audio.Media.ALBUM_ID
        };
        Cursor cursor=cr.query(
                uri,
                projection,
                selection,
                null,
                MediaStore.Audio.Media.TITLE + " ASC");

        if(cursor!=null && cursor.getCount()!=0){
            cursor.moveToFirst();
            int album_id=cursor.getInt(0);
            Bitmap bm = null;
            try
            {
                final Uri sArtworkUri = Uri
                        .parse("content://media/external/audio/albumart");

                uri = ContentUris.withAppendedId(sArtworkUri, album_id);

                ParcelFileDescriptor pfd = cr
                        .openFileDescriptor(uri, "r");

                if (pfd != null)
                {
                    FileDescriptor fd = pfd.getFileDescriptor();
                    bm = BitmapFactory.decodeFileDescriptor(fd);
                }
            } catch (Exception e) {
            }
            if(bm!=null) {
                return bm;
            }
        }
        return null;
    }

    public Uri getAlbumArtUri(int album_id){
        Uri songCover = Uri.parse("content://media/external/audio/albumart");
        Uri uriSongCover = ContentUris.withAppendedId(songCover, album_id);
        if(uriSongCover==null){
            //String packageName = context.getPackageName();
            //Uri uri = Uri.parse("android.resource://"+packageName+"/drawable/ic_batman_1");
            return null;
            //return getUriToDrawable(context,R.drawable.ic_batman_1);
        }
        return uriSongCover;
    }

    private static Uri getUriToDrawable(@NonNull Context context, @AnyRes int drawableId) {
        Uri imageUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + context.getResources().getResourcePackageName(drawableId)
                + '/' + context.getResources().getResourceTypeName(drawableId)
                + '/' + context.getResources().getResourceEntryName(drawableId) );
        return imageUri;
    }



    interface INDEX_FOR_TRACK_CURSOR{
        int _ID=0;
        int TITLE=1;
        int DATA_PATH=2;
        int ARTIST=3;
        int ALBUM=4;
        int DURATION=5;
        int ALBUM_ID=6;

    }

    interface INDEX_FOR_GENRE_CURSOR{
        int _ID=0;
        int GENRE=1;
        int NUMBER_OF_TRACKS=2;
    }
}


