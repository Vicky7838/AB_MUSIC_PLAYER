package com.music.player.bhandari.m.qlyrics.Lyrics.tasks;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.model.TrackItem;
import com.music.player.bhandari.m.qlyrics.Lyrics.ArtistInfo.ArtistInfo;
import com.music.player.bhandari.m.qlyrics.Lyrics.Keys;
import com.music.player.bhandari.m.qlyrics.Lyrics.offlineStorage.OfflineStorageArtistBio;
import com.music.player.bhandari.m.utils.Net;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URL;

/**
 * Created by Amit Bhandari on 3/27/2017.
 */

public class DownloadArtInfoThread extends Thread {

    public static String API_ROOT_URL="http://ws.audioscrobbler.com/2.0";

    public static String FORMAT_STRING="/?method=artist.getinfo&artist=%s&autocorrect=1&api_key=%s&format=json";

    public DownloadArtInfoThread(ArtistInfo.Callback callback, final String artist, TrackItem item){
        super(DownloadArtInfoThread.getRunnable(callback,artist.trim(),item));
    }

    public static Runnable getRunnable(final ArtistInfo.Callback callback,final String artist, final TrackItem item){
        return new Runnable() {
            @Override
            public void run() {
                final ArtistInfo artistInfo = new ArtistInfo(artist);
                String url = String.format(API_ROOT_URL+FORMAT_STRING, artist,Keys.LASTFM);
                JsonObject response=null;
                try {

                    URL queryURL = new URL(url);
                    Connection connection = Jsoup.connect(queryURL.toExternalForm())
                            .header("Authorization", "Bearer " + Keys.LASTFM)
                            .timeout(0)
                            .ignoreContentType(true);
                    Document document = connection.userAgent(Net.USER_AGENT).get();
                    response = new JsonParser().parse(document.text()).getAsJsonObject();
                    if(response!=null){

                        String content = response.getAsJsonObject("artist").getAsJsonObject("bio").get("content").getAsString();

                        JsonArray imageUrlArray = response.getAsJsonObject("artist").getAsJsonArray("image");
                        //0==small 1==medium 2 ==large
                        String imageUrl = imageUrlArray.get(2).getAsJsonObject().get("#text").getAsString();

                        String artistUrl = response.getAsJsonObject("artist").get("url").getAsString();

                        artistInfo.setImageUrl(imageUrl);
                        artistInfo.setArtistContent(content);
                        artistInfo.setArtistUrl(artistUrl);
                        artistInfo.setCorrectedArtist(response.getAsJsonObject("artist").get("name").getAsString());
                        if(!content.equals("")){
                            artistInfo.setFlag(ArtistInfo.POSITIVE);
                        }
                    }
                }catch (Exception e){
                    Log.v(Constants.TAG,e.toString());
                }
                threadMsg(artistInfo);
            }

            private void threadMsg(ArtistInfo artistInfo) {
                if (artistInfo != null) {

                    //put in db
                    if(artistInfo!=null && artistInfo.getFlag()==ArtistInfo.POSITIVE){
                        OfflineStorageArtistBio.putArtistBio(artistInfo,item);
                    }

                    Message msgObj = handler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putSerializable("artist_info", artistInfo);
                    msgObj.setData(b);
                    handler.sendMessage(msgObj);
                }
            }

            // Define the Handler that receives messages from the thread and update the progress
            private final Handler handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    ArtistInfo result = (ArtistInfo) msg.getData().getSerializable("artist_info");
                    if (result != null)
                        callback.onArtInfoDownloaded(result);
                }
            };

        };
    }


}
