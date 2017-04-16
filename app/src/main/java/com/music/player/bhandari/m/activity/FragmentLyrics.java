package com.music.player.bhandari.m.activity;

import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.model.TrackItem;
import com.music.player.bhandari.m.qlyrics.Lyrics.lyrics.Lyrics;
import com.music.player.bhandari.m.qlyrics.Lyrics.offlineStorage.OfflineStorageLyrics;
import com.music.player.bhandari.m.qlyrics.Lyrics.tasks.DownloadLyricThread;
import com.music.player.bhandari.m.service.PlayerService;
import com.music.player.bhandari.m.utils.DoubleClickListener;
import com.music.player.bhandari.m.utils.MyApp;
import com.music.player.bhandari.m.utils.UtilityFun;
import com.wang.avi.AVLoadingIndicatorView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Amit Bhandari on 3/10/2017.
 */

public class FragmentLyrics extends Fragment implements  Lyrics.Callback{

    private Lyrics mLyrics;
    private View layout;
    private TrackItem item;
    private AVLoadingIndicatorView lyricLoadAnimation;

    private BroadcastReceiver mLyricChange;
    private TextView lyricTextStatic, lyricStatus, updateTags;
    private LinearLayout ll_static, ll_dynamic;
    private ScrollView sv;
    private boolean fIsStaticLyrics =true;
    private EditText titleEdit, artistEdit;
    private Button buttonUpdateMetadata;

    //dynamic lyrics related variables
    //TreeMap<,TextView> lyricLines = new ArrayList<>();
    private Boolean fThreadCancelled =false;
    private Boolean fIsThreadRunning =false;
    private Handler handler ;
    private TreeMap<Long, TextView> dictionnary = new TreeMap<>();
    private long mCurrentTime = 0L;
    private long mNextTime = 0L;
    private long mPrevTime = 0L;
    private long mScrollToThisTime = 0L;
    private List<Long> mTimes = new ArrayList<>();
    private String uploader;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = inflater.inflate(R.layout.fragment_lyrics, container, false);

        titleEdit = (EditText) layout.findViewById(R.id.track_title_lyric_frag);
        artistEdit = (EditText) layout.findViewById(R.id.track_artist_lyric_frag);
        updateTags = (TextView) layout.findViewById(R.id.update_track_metadata);
        lyricLoadAnimation = (AVLoadingIndicatorView) layout.findViewById(R.id.loading_lyrics_animation);
        lyricTextStatic = (TextView) layout.findViewById(R.id.text_view_lyric_static);
        lyricStatus = (TextView) layout.findViewById(R.id.text_view_lyric_status);
        buttonUpdateMetadata = (Button) layout.findViewById(R.id.button_update_metadata);
        buttonUpdateMetadata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TrackItem item = MyApp.getService().getCurrentTrack();
                if(item==null){
                    return;
                }
                String edited_title = titleEdit.getText().toString();
                String edited_artist = artistEdit.getText().toString();

                if(edited_title.isEmpty() || edited_artist.isEmpty()){
                    Toast.makeText(getContext(),"Cannot leave field empty!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(!edited_title.equals(item.getTitle()) ||
                        !edited_artist.equals(item.getArtist()) ){
                    //changes made, save those
                    Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Audio.Media.TITLE, edited_title);
                    values.put(MediaStore.Audio.Media.ARTIST, edited_artist);
                    getContext().getContentResolver()
                            .update(uri, values, MediaStore.Audio.Media.TITLE +"=?", new String[] {item.getTitle()});

                    Intent intent = new Intent(getContext(), NowPlayingActivity.class);
                    intent.putExtra("refresh", true);
                    intent.putExtra("position", MyApp.getService().getCurrentTrackPosition());
                    intent.putExtra("originalTitle",item.getTitle());
                    intent.putExtra("title", edited_title);
                    intent.putExtra("artist", edited_artist);
                    intent.putExtra("album", item.getAlbum());
                    startActivity(intent);

                    artistEdit.setVisibility(View.GONE);
                    titleEdit.setVisibility(View.GONE);
                    updateTags.setVisibility(View.GONE);
                    buttonUpdateMetadata.setVisibility(View.GONE);
                    buttonUpdateMetadata.setClickable(false);

                    if(getActivity()!=null) {
                        View view = getActivity().getCurrentFocus();
                        if (view != null) {
                            InputMethodManager imm = (InputMethodManager) getActivity()
                                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        }
                    }

                    updateLyrics();

                }else {
                    Toast.makeText(getContext(),"Please change tags to update!",Toast.LENGTH_SHORT).show();
                }
            }
        });

        lyricStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lyricStatus.setText(getString(R.string.lyrics_loading));
                updateLyrics();
            }
        });

        ll_static =  ((LinearLayout)layout.findViewById(R.id.ll_static_lyric_view));
        ll_dynamic = (LinearLayout)layout.findViewById(R.id.ll_dynamic_lyric_view);
        ll_dynamic.setOnClickListener(new DoubleClickListener() {
            @Override
            public void onSingleClick(View v) {

            }

            @Override
            public void onDoubleClick(View v) {
                if(!fIsStaticLyrics){
                    fIsStaticLyrics = true;;
                    ll_dynamic.setVisibility(View.GONE);
                    ll_static.setVisibility(View.VISIBLE);
                    lyricTextStatic.setText(Html.fromHtml(getStaticLyrics()));
                    if(fIsThreadRunning){
                        fThreadCancelled=true;
                    }
                }
            }
        });

        ll_static.setOnClickListener(new DoubleClickListener() {
            @Override
            public void onSingleClick(View v) {

            }

            @Override
            public void onDoubleClick(View v) {
                if( mLyrics.isLRC()  &&  fIsStaticLyrics ){
                    fIsStaticLyrics = false;
                    ll_dynamic.setVisibility(View.VISIBLE);
                    ll_static.setVisibility(View.GONE);
                    seekLyrics(MyApp.getService().getCurrentTrackProgress());
                    if(!fIsThreadRunning){
                        fThreadCancelled=false;
                        Executors.newSingleThreadExecutor().execute(lyricUpdater);
                    }
                }
            }
        });

        sv = (ScrollView) layout.findViewById(R.id.scroll_view_lyrics);

        updateLyrics();

        mLyricChange = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v(Constants.TAG, "update lyrics please Jarvis");
                TrackItem item = MyApp.getService().getCurrentTrack();
                if(item==null){
                    return;
                }
                if(mLyrics!=null){
                    //if lyrics are already displayed for current song, skip this
                    if(mLyrics.getTrack().equals(item.getTitle())){
                        return;
                    }
                }
                updateLyrics();
            }
        };

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mLyricChange
                ,new IntentFilter(Constants.ACTION.UPDATE_LYRIC_AND_INFO));

        return layout;
    }

    private void updateLyrics(){
        //hide edit metadata things
        artistEdit.setVisibility(View.GONE);
        titleEdit.setVisibility(View.GONE);
        updateTags.setVisibility(View.GONE);
        buttonUpdateMetadata.setVisibility(View.GONE);
        buttonUpdateMetadata.setClickable(false);

        //set loading animation
        lyricLoadAnimation.setVisibility(View.VISIBLE);
        lyricLoadAnimation.show();

        ll_static.setVisibility(View.GONE);
        ll_dynamic.setVisibility(View.GONE);
        ll_dynamic.removeAllViews();
        fThreadCancelled =true;

        lyricStatus.setVisibility(View.VISIBLE);
        lyricStatus.setText(getString(R.string.lyrics_loading));

        item = MyApp.getService().getCurrentTrack();
        if(item!=null) {

            //check in offline storage
            mLyrics = OfflineStorageLyrics.getLyrics(item);
            if(mLyrics!=null){
                onLyricsDownloaded(mLyrics);
                return;
            }

            if (UtilityFun.isConnectedToInternet()) {
                fetchLyrics(item.getArtist(), item.getTitle(), null);
            } else {
                lyricStatus.setText(getString(R.string.no_connection));
                lyricLoadAnimation.setVisibility(View.GONE);
            }
        }
    }

    private void fetchLyrics(String... params) {
        if (getActivity() == null )
            return;

        String artist = params[0];
        String title = params[1];
        String url = null;
        if (params.length > 2)
            url = params[2];

        //download thread will look into db for lyrics, if not found, then will look online
        if (artist != null && title != null) {
            if (url == null)
                new DownloadLyricThread(this, true, item, artist, title).start();
            else
                new DownloadLyricThread(this, true, item, url, artist, title).start();
        }
    }

    @Override
    public void onLyricsDownloaded(Lyrics lyrics){

        //control comes here no matter where lyrics found, in db or online
        //so update the view here
        if(lyrics==null || getActivity()==null || !isAdded()) {
            return;
        }

        //hide loading animation
        lyricLoadAnimation.setVisibility(View.GONE);
        //before lyrics getting displayed, song has been changed already, display loading lyrics and return,
        //background thread already working to fetch latest lyrics
        //track id is -1 if lyrics are downloaded from internet and have
        //id of track from content resolver if lyrics came from offline storage
        if(lyrics.getTrackId()!=-1 && lyrics.getTrackId() != MyApp.getService().getCurrentTrack().getId()){
            //lyricTextStatic.setText(getString(R.string.lyrics_loading));
            return;
        }

        mLyrics = lyrics;
        if(layout!=null){
            //LrcView lrcView = (LrcView) layout.findViewById(R.id.lrc_view);
            if (lyrics.getFlag() == Lyrics.POSITIVE_RESULT){
                //  lrcView.setVisibility(View.VISIBLE);
                //lrcView.setOriginalLyrics(lyrics);
                //lrcView.setSourceLrc(lyrics.getText());
                //((TextView)layout.findViewById(R.id.textView3)).setVisibility(View.GONE);
                //updateLRC();

                //see if timing information available and update view accordingly
                if(lyrics.isLRC()){
                    ll_static.setVisibility(View.GONE);
                    lyricStatus.setVisibility(View.GONE);

                    fIsStaticLyrics = false;
                    fThreadCancelled=false;

                    ll_dynamic.setVisibility(View.VISIBLE);
                    updateRunningLyricsView();
                }else {
                    ll_static.setVisibility(View.VISIBLE);

                    fIsStaticLyrics =true;
                    fThreadCancelled = true;
                    ll_dynamic.setVisibility(View.GONE);
                    lyricStatus.setVisibility(View.GONE);

                    lyricTextStatic.setText(Html.fromHtml(lyrics.getText()));
                }
            }else {
                //in case no lyrics found, set staticLyric flag true as we start lyric thread based on its value
                //and we dont want our thread to run even if no lyrics found
                TrackItem item = MyApp.getService().getCurrentTrack();
                if(item!=null) {
                    artistEdit.setVisibility(View.VISIBLE);
                    titleEdit.setVisibility(View.VISIBLE);
                    updateTags.setVisibility(View.VISIBLE);
                    buttonUpdateMetadata.setVisibility(View.VISIBLE);
                    buttonUpdateMetadata.setClickable(true);
                    titleEdit.setText(item.getTitle());
                    artistEdit.setText(item.getArtist());
                }
                fIsStaticLyrics =true;
                lyricStatus.setText(getString(R.string.tap_to_refresh_lyrics));
                lyricStatus.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onDestroy() {
        fThreadCancelled =true;
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mLyricChange);
        super.onDestroyView();
    }

    private void updateRunningLyricsView(){
        if(mLyrics==null){
            return;
        }

        //reset any current dynamic lyrics related stuff
        dictionnary.clear();
        mCurrentTime = 0L;
        mNextTime = 0L;
        mPrevTime = 0L;
        mScrollToThisTime = 0L;
        mTimes.clear();
        uploader = "";

        if(handler==null) {
            handler = new Handler(Looper.getMainLooper());
        }

        //get lyric text in dictionary form
        setDictionary(mLyrics.getText());
        //dictionary is ready, create that many text views and store in lyricLines
        if(dictionnary==null || dictionnary.isEmpty()){
            //set statci lyric @// TODO: 3/31/2017
            return;
        }


        for(Map.Entry<Long,TextView> entry : dictionnary.entrySet()) {
            //String key = entry.getKey();
            TextView tv = entry.getValue();
            ll_dynamic.addView(tv);
        }
        //ll_dynamic.scrollBy(0,3000);
        //ll_dynamic.scr
        fThreadCancelled =false;

        //make sure highlighted lyrics comes on displya as soon as view is created
        changeCurrent(MyApp.getService().getCurrentTrackProgress(), true);

        if(MyApp.getService().getStatus()==PlayerService.PLAYING && !fIsThreadRunning){
            Executors.newSingleThreadExecutor().execute(lyricUpdater);
        }
    }

    private TextView getTextView(String line){
        TextView tv = new TextView(getContext());
        tv.setText(line);
        tv.setTextSize(20);
        tv.setPadding(0,0,0,20);
        tv.setTextColor(Color.WHITE);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }

    private void setDictionary(String lrc) {
        mNextTime = 0;
        mCurrentTime = 0;

        List<String> texts = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new StringReader(lrc));

        String line;
        String[] arr;
        try {
            while (null != (line = reader.readLine())) {

                arr = parseLine(line);
                if (null == arr) {
                    continue;
                }

                if (1 == arr.length) {
                    String last = texts.remove(texts.size() - 1);
                    texts.add(last + arr[0]);
                    continue;
                }
                for (int i = 0; i < arr.length - 1; i++) {
                    mTimes.add(Long.parseLong(arr[i]));
                    texts.add(arr[arr.length - 1]);
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Collections.sort(mTimes);
        for (int i = 0; i < mTimes.size(); i++) {
            if (!(texts.get(i).startsWith("Album:") || texts.get(i).startsWith("Title:")
                    || texts.get(i).startsWith("Artist:"))

                    && (i > 2 || (!texts.get(i).contains(mLyrics.getArtist()) &&
                    !texts.get(i).contains(mLyrics.getTrack()) /*&&
                    (uploader == null || !texts.get(i).contains(uploader))))//*/
            )))
                if (!(dictionnary.isEmpty() && texts.get(i).replaceAll("\\s", "").isEmpty())) {
                    Log.v(Constants.L_TAG+" chavan",texts.get(i));
                    dictionnary.put(mTimes.get(i), getTextView(texts.get(i)));
                }
        }

        Collections.sort(mTimes);
    }

    private String[] parseLine(String line) {
        Matcher matcher = Pattern.compile("\\[.+\\].+").matcher(line);
        if (!matcher.matches() || line.contains("By:")) {
            if (line.contains("[by:") && line.length() > 6)
                this.uploader = line.substring(5, line.length() - 1);
            return null;
        }

        if (line.endsWith("]"))
            line += " ";
        line = line.replaceAll("\\[", "");
        String[] result = line.split("\\]");
        try {
            for (int i = 0; i < result.length - 1; ++i)
                result[i] = String.valueOf(parseTime(result[i]));
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
            return null;
        }

        return result;
    }

    private Long parseTime(String time) {
        String[] min = time.split(":");
        String[] sec;
        if (!min[1].contains("."))
            min[1] += ".00";
        sec = min[1].split("\\.");

        long minInt = Long.parseLong(min[0].replaceAll("\\D+", "")
                .replaceAll("\r", "").replaceAll("\n", "").trim());
        long secInt = Long.parseLong(sec[0].replaceAll("\\D+", "")
                .replaceAll("\r", "").replaceAll("\n", "").trim());
        long milInt = Long.parseLong(sec[1].replaceAll("\\D+", "")
                .replaceAll("\r", "").replaceAll("\n", "").trim());

        return minInt * 60 * 1000 + secInt * 1000 + milInt * 10;
    }

    private synchronized void changeCurrent(long time, boolean justCreatedView) {
        //Log.v("Amit ",time+"");
        if (dictionnary == null || dictionnary.isEmpty())
            return;

        mScrollToThisTime = 0L;
        mPrevTime = mCurrentTime;
        mNextTime = dictionnary.lastKey();
        if (time < mNextTime)
            mNextTime = dictionnary.higherKey(time);
        mCurrentTime = dictionnary.firstKey();
        if (time > mCurrentTime)
            mCurrentTime = dictionnary.floorKey(time);
        //update UI here

        //Log.v(Constants.L_TAG,"Current time : " + mCurrentTime);
        //Log.v(Constants.L_TAG,"Next time : " + mNextTime);

        if(mCurrentTime!=0){
            try {
                mScrollToThisTime = dictionnary.lowerKey(dictionnary.lowerKey(mCurrentTime));
            }catch (Exception ignored){}
        }

        if(handler==null){
            handler=new Handler(Looper.getMainLooper());
        }

        //view is created just now, make sure lyrics highlighted comes on display
        if(justCreatedView){
            handler.post(new Runnable() {
                @Override
                public void run() {
                    //dictionnary.get(mCurrentTime).setTextSize(22);
                    //dictionnary.get(mCurrentTime).setTextColor(Color.YELLOW);
                    for(Map.Entry<Long,TextView> entry : dictionnary.entrySet()) {
                        long key = entry.getKey();
                        if(key<=mCurrentTime){
                            entry.getValue().setTextColor(Color.YELLOW);
                        }else {
                            entry.getValue().setTextColor(Color.WHITE);
                        }
                        //ll_dynamic.addView(tv);
                    }

                    Log.v(Constants.L_TAG+"gandu","damn "+ mScrollToThisTime );
                    if(mScrollToThisTime!=0) {
                        ObjectAnimator.ofInt(sv, "scrollY", dictionnary.get(mScrollToThisTime).getTop())
                                .setDuration(1000).start();
                    }else {
                        ObjectAnimator.ofInt(sv, "scrollY", dictionnary.get(dictionnary.firstKey()).getTop())
                                .setDuration(1000).start();
                    }

                }
            });
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                try {

                    //
                    if (mCurrentTime != mPrevTime && mPrevTime != 0) {

                        //if highlighted view is visible, scroll down
                        ObjectAnimator.ofInt(dictionnary.get(mCurrentTime),"textColor",Color.YELLOW).setDuration(500).start();
                        dictionnary.get(mCurrentTime).setTextSize(22);
                        Rect scrollBounds = new Rect();
                        sv.getHitRect(scrollBounds);
                        Log.v(Constants.TAG,"rect "+scrollBounds.toShortString());
                        if (dictionnary.get(mCurrentTime).getLocalVisibleRect(scrollBounds)) {
                            //ll_dynamic.scrollBy(0,30);
                            ObjectAnimator.ofInt(sv, "scrollY",  dictionnary.get(mPrevTime).getTop())
                                    .setDuration(1000).start();
                            Log.v(Constants.L_TAG,"Visible");
                        } else {
                            // imageView is not within the visible window
                            Log.v(Constants.L_TAG,"Invisible");
                        }

                    }

                }catch (Exception e){

                }
            }
        });
    }

    @Override
    public void onResume() {
        /* This code together with the one in onDestroy()
         * will make the screen be always on until this Activity gets destroyed. */
        if(!fIsStaticLyrics && !fIsThreadRunning && MyApp.getService().getStatus()==PlayerService.PLAYING){
            fThreadCancelled =false;
            Executors.newSingleThreadExecutor().execute(lyricUpdater);
        }
        seekLyrics(MyApp.getService().getCurrentTrackProgress());
        super.onResume();
    }

    @Override
    public void onPause() {
        if(fIsThreadRunning){
            fThreadCancelled =true;
        }
        super.onPause();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if(!fIsStaticLyrics && !fIsThreadRunning && MyApp.getService().getStatus()==PlayerService.PLAYING){
                fThreadCancelled =false;
                Executors.newSingleThreadExecutor().execute(lyricUpdater);
            }

            seekLyrics(MyApp.getService().getCurrentTrackProgress());
        }
        else {
            if(fIsThreadRunning){
                fThreadCancelled =true;
            }
        }

        Log.v(Constants.L_TAG,"Called...." + isVisibleToUser);
    }

    public void runLyricThread(){
        if(!fIsStaticLyrics && !fIsThreadRunning &&MyApp.getService().getStatus()==PlayerService.PLAYING){
            fThreadCancelled=false;
            Executors.newSingleThreadExecutor().execute(lyricUpdater);
        }else {
            fThreadCancelled=true;
        }
    }

    public void seekLyrics(long duration){
        changeCurrent(duration,true);
    }

    public String getStaticLyrics() {
        StringBuilder text = new StringBuilder();
        Iterator<TextView> iterator = dictionnary.values().iterator();
        while (iterator.hasNext()) {
            TextView next = iterator.next();
            if (text.length() == 0 && next.getText().toString().replaceAll("\\s", "").isEmpty())
                continue;
            text.append(next.getText().toString());
            if (iterator.hasNext())
                text.append("<br/>\n");
        }
        return text.toString();
    }

    public void clearLyrics(){

        TrackItem item = MyApp.getService().getCurrentTrack();
        if(item!=null) {
            try {
                if (!fIsStaticLyrics) {
                    ll_dynamic.setVisibility(View.GONE);
                } else {
                    lyricTextStatic.setText("");
                }
                fIsStaticLyrics = true;
                lyricStatus.setText(getString(R.string.tap_to_refresh_lyrics));
                lyricStatus.setVisibility(View.VISIBLE);
                buttonUpdateMetadata.setVisibility(View.VISIBLE);
                buttonUpdateMetadata.setClickable(true);
                titleEdit.setText(item.getTitle());
                artistEdit.setText(item.getArtist());
                artistEdit.setVisibility(View.VISIBLE);
                titleEdit.setVisibility(View.VISIBLE);
                updateTags.setVisibility(View.VISIBLE);

            }catch (NullPointerException e){

            }
        }

    }

    private final Runnable lyricUpdater = new Runnable() {
        @Override
        public void run() {
            while (true){
                if(fThreadCancelled){
                    break;
                }

                fIsThreadRunning =true;
                Log.v(Constants.L_TAG,"Lyric thread running");

                if(getActivity()!=null ){
                    changeCurrent(MyApp.getService().getCurrentTrackProgress(), false);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            fIsThreadRunning = false;
            Log.v(Constants.L_TAG,"Lyric thread stopped");
        }
    };

}
