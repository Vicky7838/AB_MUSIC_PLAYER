package com.music.player.bhandari.m.qlyrics.Lyrics.offlineStorage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.model.TrackItem;
import com.music.player.bhandari.m.qlyrics.Lyrics.lyrics.Lyrics;
import com.music.player.bhandari.m.utils.DBHelper.DbHelperUserMusicData;
import com.music.player.bhandari.m.utils.MyApp;

/**
 * Created by Amit Bhandari on 3/29/2017.
 */

public class OfflineStorageLyrics {

    //look into db for lyrics, if not found, return null
    public static Lyrics getLyrics(TrackItem item){
        if(item==null){
            return null;
        }
        Lyrics lyrics = null;

        try {
            DbHelperLyrics dbHelperLyrics = new DbHelperLyrics(MyApp.getContext());
            SQLiteDatabase db = dbHelperLyrics.getReadableDatabase();
            dbHelperLyrics.onCreate(db);

            String where = DbHelperLyrics._ID + " = " + item.getId();


            Cursor cursor = db.query(DbHelperLyrics.TABLE_NAME, new String[]{DbHelperLyrics.LYRICS}
                    , where, null, null, null, null, "1");

            if (cursor != null && cursor.getCount() != 0) {
                cursor.moveToFirst();
                //retrieve and fill lyrics object
                Gson gson = new Gson();
                lyrics = gson.fromJson(cursor.getString(cursor.getColumnIndex(DbHelperLyrics.LYRICS)), Lyrics.class);
                lyrics.setTrackId(item.getId());
                cursor.close();
            }
        }catch (Exception e){
            return null;
        }

        return lyrics;
    }

    public static void putLyrics(Lyrics lyrics,TrackItem item){
        if(item==null || lyrics==null){
            return;
        }

        try {
            DbHelperLyrics dbHelperLyrics = new DbHelperLyrics(MyApp.getContext());
            SQLiteDatabase db = dbHelperLyrics.getWritableDatabase();
            dbHelperLyrics.onCreate(db);

            //check if data already exists, if it does, return
            String where = DbHelperLyrics._ID + " = " + item.getId()
                    + " OR " + DbHelperLyrics.KEY_TITLE + "= '" + item.getTitle().replace("'", "''") + "'";


            Cursor cursor = db.query(DbHelperLyrics.TABLE_NAME, new String[]{DbHelperLyrics.KEY_TITLE}
                    , where, null, null, null, null, "1");
            if(cursor!=null && cursor.getCount()>0){
                cursor.close();
                return;
            }

            //convert lyrics to json
            Gson gson = new Gson();
            String jsonInString = gson.toJson(lyrics);

            ContentValues c = new ContentValues();
            c.put(DbHelperLyrics.LYRICS, jsonInString);
            c.put(DbHelperLyrics.KEY_TITLE, item.getTitle());
            c.put(DbHelperLyrics._ID, item.getId());
            db.insert(DbHelperLyrics.TABLE_NAME, null, c);
        }catch (Exception ignored){}

    }

    public static boolean clearLyrics(TrackItem item){
        if(item==null){
            return false;
        }

        try {
            DbHelperLyrics dbHelperLyrics = new DbHelperLyrics(MyApp.getContext());
            SQLiteDatabase db = dbHelperLyrics.getReadableDatabase();
            dbHelperLyrics.onCreate(db);

            String where = DbHelperLyrics._ID + " = " + item.getId();

            int i = db.delete(DbHelperLyrics.TABLE_NAME,where,null);

            return i >= 1;
        }catch (Exception e){
            return false;
        }

    }
}
