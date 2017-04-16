package com.music.player.bhandari.m.utils.DBHelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by amit on 17/1/17.
 */

public class DbHelperUserMusicData extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "beta_player";

    //columns
    static public final String KEY_TITLE = "song_title";
    static public final String KEY_TIME_STAMP = "last_time_played";
    static public final String KEY_COUNT ="number_of_times_played";
    public static final String KEY_FAV = "My_Fav";
    public static final String KEY_LAST_PLAYING_QUEUE = "last_playing_queue";


    public static final String TABLE_NAME = "user_music_data";
    private static final String TABLE_CREATE= "CREATE TABLE IF NOT EXISTS "
            +TABLE_NAME+" ("+ KEY_TITLE+" TEXT, " + KEY_COUNT + " INTEGER, " +KEY_TIME_STAMP+" INTEGER, "
            +KEY_LAST_PLAYING_QUEUE + " INTEGER, "+KEY_FAV+" INTEGER);";

    public DbHelperUserMusicData(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME );
        onCreate(db);
    }
}
