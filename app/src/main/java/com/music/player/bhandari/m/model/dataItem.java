package com.music.player.bhandari.m.model;

import android.util.Log;

/**
 * Created by Amit Bhandari on 1/26/2017.
 */

public class dataItem{


    public dataItem(int id,String title,int artist_id, String artist_name, int album_id, String albumName
            , String year, String file_path, String duration){
        this.title=title;
        this.id=id;

        this.album_id=album_id;
        this.albumName=albumName;

        this.artist_id=artist_id;
        this.artist_name=artist_name;

        this.file_path=file_path;
        if(year!=null)
        this.year=year;

        //Log.v("Year", title + " : " + year);

        this.duration=duration;
        if(!duration.equals("")) {
            this.durStr = getDurStr();
        }

    }

    public dataItem(int id,String title,int numberOfTracks, int numberOfAlbums){
        this.artist_name=title;
        this.artist_id=id;

        this.title = title;

        this.numberOfTracks = numberOfTracks;
        this.numberOfAlbums=numberOfAlbums;
    }


    public dataItem(int id,String title,String artist_name, int numberOfTracks, String year){
        this.artist_name=artist_name;
        this.title = title;

        this.albumName=title;
        this.album_id=id;

       // Log.v("Year", title + " : " + year);

        if(year!=null)
        this.year=year;
        this.numberOfTracks = numberOfTracks;
    }

    public dataItem(int genre_id,String genre_name, int numberOfTracks){
        this.id=genre_id;
        this.title=genre_name;
        this.numberOfTracks = numberOfTracks;
    }

    public int id;
    public String title;

    public int artist_id;
    public String artist_name;

    public int album_id;
    public String albumName;

    public String year="zzzz";

    public int numberOfTracks;
    public int numberOfAlbums;
    public String file_path;


    public String duration;
    public String durStr;

    private String getDurStr(){
        int minutes=(Integer.parseInt(duration)/1000)/60;
        int seconds= (Integer.parseInt(duration)/1000)%60;
        String durFormatted=String.format("%02d",seconds);
        return  String.valueOf(minutes)+":"+durFormatted;
    }
}