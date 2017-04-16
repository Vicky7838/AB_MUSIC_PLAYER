package com.music.player.bhandari.m.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.model.dataItem;
import com.music.player.bhandari.m.utils.DBHelper.DbHelperListOfPlaylist;
import com.music.player.bhandari.m.utils.DBHelper.DbHelperUserMusicData;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.Executors;

/**
 * Created by amit on 13/1/17.
 */

public class PlaylistManager {
    private Context context;
    private static PlaylistManager playlistManager;
    private static DbHelperUserMusicData dbHelperUserMusicData;
    private static DbHelperListOfPlaylist dbHelperListOfPlaylist;

    private PlaylistManager(Context context){
        this.context=context;
         dbHelperUserMusicData = new DbHelperUserMusicData(context);
         dbHelperListOfPlaylist  = new DbHelperListOfPlaylist(context);
    }

    public static PlaylistManager getInstance(Context context){
        if (playlistManager==null){
            playlistManager = new PlaylistManager(context);
        }
        return playlistManager;
    }

    void PopulateUserMusicTable(){
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.v(Constants.TAG,"populating");
                    //DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
                    SQLiteDatabase db = dbHelperUserMusicData.getWritableDatabase();
                    Cursor cur;
                    ArrayList<dataItem> dataItems = MusicLibrary.getInstance().getDataItemsForTracks();
                    for (dataItem item : dataItems) {
                        //check if song already contains in db, if no, add it
                        String where = DbHelperUserMusicData.KEY_TITLE + "= '"
                                + item.title.replace("'", "''") + "'";
                        cur = db.query(DbHelperUserMusicData.TABLE_NAME
                                , new String[]{DbHelperUserMusicData.KEY_TITLE}
                                , where, null, null, null, null, null);
                        if (cur.getCount() == 0) {
                            ContentValues c = new ContentValues();
                            c.put(DbHelperUserMusicData.KEY_TITLE, item.title);
                            c.put(DbHelperUserMusicData.KEY_COUNT, 0);
                            c.put(DbHelperUserMusicData.KEY_FAV, 0);
                            c.put(DbHelperUserMusicData.KEY_TIME_STAMP, 0);
                            c.put(DbHelperUserMusicData.KEY_LAST_PLAYING_QUEUE, 0);
                            db.insert(DbHelperUserMusicData.TABLE_NAME, null, c);
                        }
                        cur.close();
                    }
                }catch (Exception e){
                    //igore any exception
                    //concurrent modification exception
                }
            }
        });
    }

    /*
    user_addable = only playlist in which user can add songs
     */

    public ArrayList<String> GetPlaylistList(boolean userAddable){
        ArrayList<String> listOfPlaylist=new ArrayList<>();
        SQLiteDatabase db = dbHelperListOfPlaylist.getReadableDatabase();
        dbHelperListOfPlaylist.onCreate(db);
        Cursor cursor=db.query(Constants.SYSTEM_PLAYLISTS.PLAYLIST_LIST,null,null,null,null,null,null);
        while (cursor.moveToNext()){
            if(!userAddable) {
                String s = cursor.getString(0).replace("_"," ");
                listOfPlaylist.add(s);
            }else {
                //REMOVE LAST PLAYED,LAST ADDED AND MOST PLAYED FROM LIST AS USER CANNOT ADD SONG IN THIS
                if(!cursor.getString(0).equals(Constants.SYSTEM_PLAYLISTS.MOST_PLAYED)
                        && !cursor.getString(0).equals(Constants.SYSTEM_PLAYLISTS.RECENTLY_ADDED)
                        && !cursor.getString(0).equals(Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED))
                {
                    String s = cursor.getString(0).replace("_"," ");
                    listOfPlaylist.add(s);
                }
            }
        }
        cursor.close();
        return listOfPlaylist;
    }

    public boolean CreatePlaylist(String playlist_name){

        playlist_name = playlist_name.replace(" ","_");
        //try adding column
        //DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
        SQLiteDatabase db = dbHelperUserMusicData.getWritableDatabase();
        dbHelperUserMusicData.onCreate(db);
        //create column for newly created playlist
        String insertQuery = "ALTER TABLE " + DbHelperUserMusicData.TABLE_NAME + " ADD COLUMN "
                + playlist_name + " INTEGER DEFAULT 0";
        try {
            db.execSQL(insertQuery);
        }catch (Exception e){

        }
        //try creating entry in playlist list
        //DbHelperListOfPlaylist dbHelperListOfPlaylist = new DbHelperListOfPlaylist(context);
        db = dbHelperListOfPlaylist.getWritableDatabase();
        dbHelperListOfPlaylist.onCreate(db);

        String where = DbHelperListOfPlaylist.KEY_TITLE + "= '" + playlist_name.replace("'", "''") + "'";
        if(db.query(Constants.SYSTEM_PLAYLISTS.PLAYLIST_LIST, new String[]{DbHelperListOfPlaylist.KEY_TITLE}
                ,where, null, null, null, null).getCount()==0){
            ContentValues c = new ContentValues();
            c.put(DbHelperListOfPlaylist.KEY_TITLE, playlist_name);
            db.insert(Constants.SYSTEM_PLAYLISTS.PLAYLIST_LIST, null, c);
            return true;
        }
        return false;
    }

    public boolean DeletePlaylist(String playlist_name){
        playlist_name = playlist_name.replace(" ","_");
        //delete column is not possible in sqlite
        //just a remove playlist entry from playlist list
        //and clear the column
        if(playlist_name.equals(Constants.SYSTEM_PLAYLISTS.MOST_PLAYED)
                || playlist_name.equals(Constants.SYSTEM_PLAYLISTS.RECENTLY_ADDED)
                || playlist_name.equals(Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED)
                || playlist_name.equals(Constants.SYSTEM_PLAYLISTS.MY_FAV)){
            return false;
        }


        //DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
        SQLiteDatabase db = dbHelperUserMusicData.getWritableDatabase();
        dbHelperUserMusicData.onCreate(db);
        ContentValues c = new ContentValues();
        c.put(playlist_name, 0);
        db.update(DbHelperUserMusicData.TABLE_NAME, c, null, null);

        //DbHelperListOfPlaylist dbHelperListOfPlaylist
          //      = new DbHelperListOfPlaylist(context);
        db = dbHelperListOfPlaylist.getWritableDatabase();
        dbHelperListOfPlaylist.onCreate(db);

        if(db.delete(Constants.SYSTEM_PLAYLISTS.PLAYLIST_LIST
                ,DbHelperListOfPlaylist.KEY_TITLE+"= '" + playlist_name.replace("'","''") + "'"
                ,null)!=0){
            return true;
        }else {
            return false;
        }
    }

    void RemoveEntryFromUserMusicDb(String[] song_titles){
        //DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
        SQLiteDatabase db = dbHelperUserMusicData.getWritableDatabase();
        dbHelperUserMusicData.onCreate(db);

        for(String song_title:song_titles) {
            String where = DbHelperUserMusicData.KEY_TITLE + "= '" + song_title.replace("'", "''") + "'";
            db.delete(DbHelperUserMusicData.TABLE_NAME, where, null);
        }
    }

    public  void AddSongToPlaylist(String playlist_name_arg, final String[] song_titles){

        final String playlist_name =  playlist_name_arg.replace(" ","_");
        final Handler hand = new Handler();
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
          //      DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
                SQLiteDatabase db = dbHelperUserMusicData.getWritableDatabase();
                dbHelperUserMusicData.onCreate(db);

                //check if song exists
                if(song_titles.length==1) {
                    String where = DbHelperUserMusicData.KEY_TITLE + "= '" +song_titles[0].replace("'", "''") + "'"
                            + " AND " + playlist_name + " != 0" ;
                    if(db.query(DbHelperUserMusicData.TABLE_NAME
                            , new String[]{playlist_name}, where, null, null, null, null)
                            .getCount()>0){
                        hand.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Songs already exists in " + playlist_name, Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }
                }

                String max = "MAX(" + playlist_name + ")";
                Cursor cursor = db.query(DbHelperUserMusicData.TABLE_NAME, new String [] {max}, null, null, null, null, null);
                cursor.moveToFirst();
                int maxValue = cursor.getInt(0);
                cursor.close();

                for (String song:song_titles) {
                    ContentValues c = new ContentValues();
                    c.put(playlist_name, ++maxValue);
                    db.update(DbHelperUserMusicData.TABLE_NAME,c,DbHelperUserMusicData.KEY_TITLE + "= ?", new String[] {song});
                }

                hand.post(new Runnable() {
                    @Override
                    public void run() {

                        if(playlist_name.equals(Constants.SYSTEM_PLAYLISTS.MY_FAV)){
                            return;
                        }

                        Toast toast;
                            if(song_titles.length>1) {
                                toast = Toast.makeText(context, "Songs added in " + playlist_name.replace("_", " "), Toast.LENGTH_SHORT);
                            }else {
                                toast =  Toast.makeText(context, "Song added in " + playlist_name.replace("_", " "), Toast.LENGTH_SHORT);
                            }
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                });
            }
        });
    }

    public void RemoveSongFromPlaylist(String playlist_name, String song_title){
        playlist_name = playlist_name.replace(" ","_");
        {
            //user playlist
            //DbHelperUserMusicData dbHelperMusicData
              //      = new DbHelperUserMusicData(context);
            SQLiteDatabase db = dbHelperUserMusicData.getReadableDatabase();
            dbHelperUserMusicData.onCreate(db);
            try {
                String where = DbHelperUserMusicData.KEY_TITLE + "='" + song_title.replace("'","''") + "'";
                ContentValues c = new ContentValues();
                c.put(playlist_name, 0);
                db.update(DbHelperUserMusicData.TABLE_NAME, c, where, null);

                Toast.makeText(context,"Removed from playlist",Toast.LENGTH_SHORT).show();
            }catch (Exception e){
                Toast.makeText(context,"Error occured while removing",Toast.LENGTH_SHORT).show();
            }finally {
            }
        }
    }

    public ArrayList<String> GetPlaylist(String playlist_name){
        playlist_name = playlist_name.replace(" ","_");
        ArrayList<String> trackList = new ArrayList<>();
        switch (playlist_name){
            case Constants.SYSTEM_PLAYLISTS.MOST_PLAYED:
                trackList = GetMostPlayed();
                break;

            case Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED:
                trackList=GetRecentlyPlayed();
                break;

            case Constants.SYSTEM_PLAYLISTS.RECENTLY_ADDED:
                trackList=GetRecentlyAdded();
                break;

            case Constants.SYSTEM_PLAYLISTS.MY_FAV:
                trackList = GetFav();
                break;

            default:
                trackList = GetUserPlaylist(playlist_name);
                break;
        }
        return trackList;
    }

    public void AddToRecentlyPlayedAndUpdateCount(final String title){

        //thread for updating play numberOfTracks
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {


                    //DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
                    SQLiteDatabase db = dbHelperUserMusicData.getWritableDatabase();
                    dbHelperUserMusicData.onCreate(db);
                try {
                    String getCurrentCountQuery = "SELECT " + DbHelperUserMusicData.KEY_COUNT
                            + " FROM " + DbHelperUserMusicData.TABLE_NAME + " WHERE "
                            + DbHelperUserMusicData.KEY_TITLE + " = '" + title.replace("'", "''") + "'";
                    Cursor getCurrentCountCursor = db.rawQuery(getCurrentCountQuery, null);
                    getCurrentCountCursor.moveToFirst();
                    int currentCount = getCurrentCountCursor.getInt
                            (getCurrentCountCursor.getColumnIndex(DbHelperUserMusicData.KEY_COUNT));
                    getCurrentCountCursor.close();

                    ContentValues c = new ContentValues();
                    c.put(DbHelperUserMusicData.KEY_COUNT, currentCount + 1);
                    db.update(DbHelperUserMusicData.TABLE_NAME, c, DbHelperUserMusicData.KEY_TITLE + "= ?", new String[]{title});

                }
                catch (Exception e){
                }
            }
        });

        //thread for adding entry in recently played
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                //DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
                SQLiteDatabase db = dbHelperUserMusicData.getWritableDatabase();
                dbHelperUserMusicData.onCreate(db);

                ContentValues c = new ContentValues();
                c.put(DbHelperUserMusicData.KEY_TIME_STAMP, System.currentTimeMillis());
                db.update(DbHelperUserMusicData.TABLE_NAME,c,DbHelperUserMusicData.KEY_TITLE + "= ?", new String[] {title});

            }
        });
    }

    public void AddToFav(String title){
        final String songTitle = title;
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                //DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
                SQLiteDatabase db = dbHelperUserMusicData.getWritableDatabase();
                dbHelperUserMusicData.onCreate(db);

                ContentValues c = new ContentValues();
                c.put(DbHelperUserMusicData.KEY_FAV, 1);
                db.update(DbHelperUserMusicData.TABLE_NAME,c,DbHelperUserMusicData.KEY_TITLE + "= ?", new String[] {songTitle});

            }
        });

    }

    public boolean isFav(String title){

        boolean returnValue=false;

        //DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
        SQLiteDatabase db = dbHelperUserMusicData.getReadableDatabase();
        dbHelperUserMusicData.onCreate(db);

        String where = DbHelperUserMusicData.KEY_TITLE + "= '" + title.replace("'","''") +"'" ;
        Cursor cursor = db.query(DbHelperUserMusicData.TABLE_NAME
                ,new String[]{DbHelperUserMusicData.KEY_FAV},where,null,null,null
                ,null,null);

        if(cursor.getCount()!=0){
            cursor.moveToFirst();
            if(cursor.getInt(cursor.getColumnIndex(DbHelperUserMusicData.KEY_FAV))>0){
                returnValue =true;
            }
        }
        cursor.close();
        return  returnValue;
    }

    public void RemoveFromFav(final String title){
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {

         //       DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
                SQLiteDatabase db = dbHelperUserMusicData.getWritableDatabase();
                dbHelperUserMusicData.onCreate(db);

                ContentValues c = new ContentValues();
                c.put(DbHelperUserMusicData.KEY_FAV, 0);
                db.update(DbHelperUserMusicData.TABLE_NAME,c,DbHelperUserMusicData.KEY_TITLE + "= ?", new String[] {title});

            }
        });
    }

    public void StoreLastPlayingQueue(final ArrayList<String> tracklist){
        SQLiteDatabase db = dbHelperUserMusicData.getWritableDatabase();
        dbHelperUserMusicData.onCreate(db);

        //clear column first
        ContentValues c = new ContentValues();
        c.put(DbHelperUserMusicData.KEY_LAST_PLAYING_QUEUE, 0);
        db.update(DbHelperUserMusicData.TABLE_NAME,c,null,null);

        for(String song_title:tracklist){
            c = new ContentValues();
            c.put(DbHelperUserMusicData.KEY_LAST_PLAYING_QUEUE, 1);
            db.update(DbHelperUserMusicData.TABLE_NAME,c,DbHelperUserMusicData.KEY_TITLE + "= ?", new String[] {song_title});
        }
    }

    public ArrayList<String> RestoreLastPlayingQueue(){
//        DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
        SQLiteDatabase db = dbHelperUserMusicData.getReadableDatabase();
        dbHelperUserMusicData.onCreate(db);

        String where = DbHelperUserMusicData.KEY_LAST_PLAYING_QUEUE + " = 1";
        Cursor c = db.query(DbHelperUserMusicData.TABLE_NAME, new String[]{DbHelperUserMusicData.KEY_TITLE}
                , where, null
                , null, null, null);
        ArrayList<String> tracklist=new ArrayList<>();
        while (c.moveToNext()) {
            tracklist.add(c.getString(0));
        }
        c.close();
        return tracklist;
    }


    //private methods
    private ArrayList<String> GetFav(){

        //DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
        SQLiteDatabase db = dbHelperUserMusicData.getReadableDatabase();
        dbHelperUserMusicData.onCreate(db);

        String where = DbHelperUserMusicData.KEY_FAV  + " != 0";

        Cursor c = db.query(DbHelperUserMusicData.TABLE_NAME, new String[]{DbHelperUserMusicData.KEY_TITLE}
                , where, null
                , null, null, DbHelperUserMusicData.KEY_FAV  );
        ArrayList<String> tracklist=new ArrayList<>();
        while (c.moveToNext()) {
            tracklist.add(c.getString(0));
        }
        c.close();
        return tracklist;
    }

    private ArrayList<String> GetMostPlayed(){
     //   DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
        SQLiteDatabase db = dbHelperUserMusicData.getReadableDatabase();
        dbHelperUserMusicData.onCreate(db);

        ArrayList<String> tracklist= new ArrayList<>();
        String where = DbHelperUserMusicData.KEY_COUNT + " > 0 ";
        Cursor cursor = db.query(DbHelperUserMusicData.TABLE_NAME,new String[]{DbHelperUserMusicData.KEY_TITLE}
                ,where,null,null,null,DbHelperUserMusicData.KEY_COUNT+" DESC",""+Constants.SYSTEM_PLAYLISTS.MOST_PLAYED_MAX);

        while (cursor.moveToNext()){
            tracklist.add(cursor.getString(0));
        }
        cursor.close();
        return tracklist;
    }

    private ArrayList<String> GetRecentlyPlayed(){
       // DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
        SQLiteDatabase db = dbHelperUserMusicData.getReadableDatabase();
        dbHelperUserMusicData.onCreate(db);

        ArrayList<String> tracklist= new ArrayList<>();
        String where = DbHelperUserMusicData.KEY_TIME_STAMP + " > 0 ";
        Cursor cursor = db.query(DbHelperUserMusicData.TABLE_NAME,new String[]{DbHelperUserMusicData.KEY_TITLE}
                ,where,null,null,null,DbHelperUserMusicData.KEY_TIME_STAMP+" DESC",""+Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED_MAX);

        while (cursor.moveToNext()){
            tracklist.add(cursor.getString(0));
        }
        cursor.close();

        return tracklist;
    }

    private ArrayList<String> GetRecentlyAdded(){
        HashMap<String, String> pathToTitle=new HashMap<>();
        ArrayList<File> musicFiles=new ArrayList<>();
        for (dataItem item:MusicLibrary.getInstance().getDataItemsForTracks()){
            musicFiles.add(new File(item.file_path));
            pathToTitle.put(item.file_path
                    ,item.title);
        }

        Collections.sort( musicFiles, new Comparator()
        {
            public int compare(Object o1, Object o2) {

                if (((File)o1).lastModified() > ((File)o2).lastModified()) {
                    return -1;
                } else if (((File)o1).lastModified() < ((File)o2).lastModified()) {
                    return +1;
                } else {
                    return 0;
                }
            }
        });

        ArrayList<String> tracklist = new ArrayList<>();
        for(int i=0;i<50;i++){
            tracklist.add(pathToTitle.get(musicFiles.get(i).getAbsolutePath()));
        }
        return tracklist;
    }

    private ArrayList<String> GetUserPlaylist(String playlist_name){

        //DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
        SQLiteDatabase db = dbHelperUserMusicData.getReadableDatabase();
        dbHelperUserMusicData.onCreate(db);

        String where = playlist_name + " != 0";

        Cursor c = db.query(DbHelperUserMusicData.TABLE_NAME, new String[]{DbHelperUserMusicData.KEY_TITLE}
                , where, null
                , null, null, playlist_name );
        ArrayList<String> tracklist=new ArrayList<>();
        while (c.moveToNext()) {
            tracklist.add(c.getString(0));
        }
        c.close();
        return tracklist;
    }
}



