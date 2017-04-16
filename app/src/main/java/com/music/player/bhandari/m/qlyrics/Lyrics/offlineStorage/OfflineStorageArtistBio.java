package com.music.player.bhandari.m.qlyrics.Lyrics.offlineStorage;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;
import com.music.player.bhandari.m.model.TrackItem;
import com.music.player.bhandari.m.qlyrics.Lyrics.ArtistInfo.ArtistInfo;
import com.music.player.bhandari.m.utils.MyApp;

/**
 * Created by Amit Bhandari on 3/29/2017.
 */

public class OfflineStorageArtistBio {
    public static ArtistInfo getArtistBio(TrackItem item){
        if(item==null){
            return null;
        }
        ArtistInfo artistInfo = null;

        try {
            DbHelperArtistBio dbHelperArtistBio = new DbHelperArtistBio(MyApp.getContext());
            SQLiteDatabase db = dbHelperArtistBio.getReadableDatabase();
            dbHelperArtistBio.onCreate(db);

            String where = DbHelperArtistBio.ARTIST_ID + " = " + item.getArtist_id()
                    + " OR " + DbHelperArtistBio.KEY_ARTIST + "= '" + item.getArtist().replace("'","''") +"'" ;;

            Cursor cursor = db.query(DbHelperArtistBio.TABLE_NAME,new String[]{DbHelperArtistBio.ARTIST_BIO}
                    ,where,null,null,null,null,"1");

            if(cursor!=null && cursor.getCount()!=0){
                cursor.moveToFirst();
                //retrieve and fill lyrics object
                Gson gson = new Gson();
                artistInfo = gson.fromJson(cursor.getString
                        (cursor.getColumnIndex(DbHelperArtistBio.ARTIST_BIO)),ArtistInfo.class);
                cursor.close();
            }

        }catch (Exception e){
            return null;
        }

        return artistInfo;
    }

    public static void putArtistBio(ArtistInfo artistInfo, TrackItem item){
        if(item==null || artistInfo==null){
            return;
        }

        try {
            DbHelperArtistBio dbHelperArtistBio = new DbHelperArtistBio(MyApp.getContext());
            SQLiteDatabase db = dbHelperArtistBio.getWritableDatabase();
            dbHelperArtistBio.onCreate(db);

            //check if already exists, if yes, return
            String where = DbHelperArtistBio.ARTIST_ID + " = " + item.getArtist_id()
                    + " OR " + DbHelperArtistBio.KEY_ARTIST + "= '" + item.getArtist().replace("'","''") +"'" ;;

            Cursor cursor = db.query(DbHelperArtistBio.TABLE_NAME,new String[]{DbHelperArtistBio.KEY_ARTIST}
                    ,where,null,null,null,null,"1");
            if(cursor!=null && cursor.getCount()>0){
                cursor.close();
                return;
            }

            //convert lyrics to json
            Gson gson = new Gson();
            String jsonInString = gson.toJson(artistInfo);

            ContentValues c = new ContentValues();
            c.put(DbHelperArtistBio.ARTIST_BIO, jsonInString);
            c.put(DbHelperArtistBio.KEY_ARTIST, item.getArtist());
            c.put(DbHelperArtistBio.ARTIST_ID, item.getArtist_id());
            db.insert(DbHelperArtistBio.TABLE_NAME, null, c);
        }catch (Exception ignored){

        }
    }
}
