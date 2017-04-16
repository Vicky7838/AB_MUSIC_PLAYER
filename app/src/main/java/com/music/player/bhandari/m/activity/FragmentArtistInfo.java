package com.music.player.bhandari.m.activity;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.model.TrackItem;
import com.music.player.bhandari.m.qlyrics.Lyrics.ArtistInfo.ArtistInfo;
import com.music.player.bhandari.m.qlyrics.Lyrics.offlineStorage.OfflineStorageArtistBio;
import com.music.player.bhandari.m.qlyrics.Lyrics.tasks.DownloadArtInfoThread;
import com.music.player.bhandari.m.utils.DoubleClickListener;
import com.music.player.bhandari.m.utils.MyApp;
import com.music.player.bhandari.m.utils.UtilityFun;
import com.wang.avi.AVLoadingIndicatorView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Created by Amit Bhandari on 3/10/2017.
 */

public class FragmentArtistInfo extends Fragment implements ArtistInfo.Callback {
    View layout;
    BroadcastReceiver mReceiver;
    ArtistInfo mArtistInfo;
    TextView artBioText, retryText, updateTagsText;
    AVLoadingIndicatorView lyricLoadAnimation;
    private EditText  artistEdit;
    private Button buttonUpdateMetadata;

    @Override
    public void onResume() {
        super.onResume();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = inflater.inflate(R.layout.fragment_artist_info, container, false);

        artBioText = (TextView) layout.findViewById(R.id.text_view_art_bio_frag) ;
        retryText = (TextView)layout.findViewById(R.id.retry_text_view);
        lyricLoadAnimation = (AVLoadingIndicatorView) layout.findViewById(R.id.loading_lyrics_animation);
        artistEdit = (EditText) layout.findViewById(R.id.track_artist_artsi_bio_frag);
        buttonUpdateMetadata = (Button) layout.findViewById(R.id.button_update_metadata);
        updateTagsText = (TextView) layout.findViewById(R.id.update_track_metadata);
        buttonUpdateMetadata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TrackItem item = MyApp.getService().getCurrentTrack();
                if(item==null){
                    return;
                }

                String edited_artist = artistEdit.getText().toString().trim();

                if(edited_artist.isEmpty()){
                    Toast.makeText(getContext(),"Cannot leave field empty!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(!edited_artist.equals(item.getArtist()) ){
                    //changes made, save those
                    Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Audio.Media.ARTIST, edited_artist);
                    getContext().getContentResolver()
                            .update(uri, values, MediaStore.Audio.Media.TITLE +"=?", new String[] {item.getTitle()});

                    Intent intent = new Intent(getContext(), NowPlayingActivity.class);
                    intent.putExtra("refresh", true);
                    intent.putExtra("position", MyApp.getService().getCurrentTrackPosition());
                    intent.putExtra("originalTitle",item.getTitle());
                    intent.putExtra("title", item.getTitle());
                    intent.putExtra("artist", edited_artist);
                    intent.putExtra("album", item.getAlbum());
                    startActivity(intent);

                    artistEdit.setVisibility(View.GONE);
                    updateTagsText.setVisibility(View.GONE);
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

                    downloadArtInfo();

                }else {
                    Toast.makeText(getContext(),"Please change tags to update!",Toast.LENGTH_SHORT).show();
                }
            }
        });

        //retry click listner
        layout.findViewById(R.id.ll_art_bio).setOnClickListener(new DoubleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if(retryText.getVisibility()==View.VISIBLE) {
                    retryText.setVisibility(View.GONE);
                    artBioText.setVisibility(View.VISIBLE);
                    artistEdit.setVisibility(View.GONE);
                    updateTagsText.setVisibility(View.GONE);
                    buttonUpdateMetadata.setVisibility(View.GONE);
                    buttonUpdateMetadata.setClickable(false);
                    lyricLoadAnimation.setVisibility(View.GONE);
                    downloadArtInfo();
                }
            }

            @Override
            public void onDoubleClick(View v) {

                //if no connection text, do not hide artist content
                if(retryText.getText().toString().equals(getString(R.string.no_connection))){
                    return;
                }

                if(artBioText.getVisibility()==View.VISIBLE){
                    artBioText.setVisibility(View.GONE);
                    if(layout!=null){
                        layout.findViewById(R.id.double_tap_to_see_art_bio).setVisibility(View.VISIBLE);
                    }
                }else {
                    artBioText.setVisibility(View.VISIBLE);
                    layout.findViewById(R.id.double_tap_to_see_art_bio).setVisibility(View.GONE);
                }
            }
        });

        downloadArtInfo();
        mReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //already displayed, skip
                TrackItem item = MyApp.getService().getCurrentTrack();
                if(item==null){
                    return;
                }
                if(mArtistInfo!=null && mArtistInfo.getOriginalArtist()
                        .equals(item.getArtist())){
                    return;
                }

                //set loading  text and animation


                downloadArtInfo();
            }
        };
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver
                ,new IntentFilter(Constants.ACTION.UPDATE_LYRIC_AND_INFO));
        return layout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
    }

    private void downloadArtInfo(){
        TrackItem item = MyApp.getService().getCurrentTrack();
        if(item==null){
            return;
        }

        artBioText.setText(getString(R.string.artist_info_loading));

        //set loading animation
        lyricLoadAnimation.setVisibility(View.VISIBLE);
        lyricLoadAnimation.show();

        //see in offlinne db first
        mArtistInfo = OfflineStorageArtistBio.getArtistBio(item);
        if(mArtistInfo!=null){
            onArtInfoDownloaded(mArtistInfo);
            return;
        }

        if (UtilityFun.isConnectedToInternet()) {
            new DownloadArtInfoThread(this, item.getArtist(), item).start();
        } else {
            //artBioText.setText(getString(R.string.no_connection));
            artBioText.setVisibility(View.GONE);
            retryText.setText(getString(R.string.no_connection));
            retryText.setVisibility(View.VISIBLE);
            lyricLoadAnimation.setVisibility(View.GONE);
            //retryText.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onArtInfoDownloaded(ArtistInfo artistInfo) {

        mArtistInfo = artistInfo;
        if(artistInfo==null || getActivity()==null || !isAdded()){
            return;
        }

        //hide loading animation
        lyricLoadAnimation.setVisibility(View.GONE);

        TrackItem item = MyApp.getService().getCurrentTrack();

        //if song is already changed , return
        if(item!=null && !item.getArtist().trim().equals(artistInfo.getOriginalArtist().trim())){
            //artBioText.setText(getString(R.string.artist_info_loading));
            return;
        }

        if(artistInfo.getArtistContent()==null){
            retryText.setText(getString(R.string.artist_info_no_result));
            retryText.setVisibility(View.VISIBLE);
            artBioText.setVisibility(View.GONE);
            TrackItem tempItem = MyApp.getService().getCurrentTrack();
            if(tempItem!=null) {
                artistEdit.setVisibility(View.VISIBLE);
                updateTagsText.setVisibility(View.VISIBLE);
                buttonUpdateMetadata.setVisibility(View.VISIBLE);
                buttonUpdateMetadata.setClickable(true);
                artistEdit.setText(tempItem.getArtist());
            }
            return;
        }

        if(layout!=null && getActivity()!=null && artistInfo.getArtistContent()!=null){
            String content = artistInfo.getArtistContent();
            int index = content.indexOf("Read more");
            SpannableString ss = new SpannableString(content);
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    if(mArtistInfo.getArtistUrl()==null){
                        Toast.makeText(getContext(),"Invalid URL!",Toast.LENGTH_SHORT).show();
                    }else {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mArtistInfo.getArtistUrl()));
                        startActivity(browserIntent);
                    }
                }
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setTypeface(Typeface.create(ds.getTypeface(), Typeface.BOLD));
                }
            };
            if(index!=-1) {
                ss.setSpan(clickableSpan, index, index+9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

           /* TextView textView = (TextView) findViewById(R.id.hello);
            textView.setText(ss);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            textView.setHighlightColor(Color.TRANSPARENT);*/

            if(!content.equals("")) {
                artBioText.setVisibility(View.VISIBLE);
                retryText.setVisibility(View.GONE);

                artBioText.setText(ss);
                artBioText.setMovementMethod(LinkMovementMethod.getInstance());

                artistEdit.setVisibility(View.GONE);
                updateTagsText.setVisibility(View.GONE);
                buttonUpdateMetadata.setVisibility(View.GONE);
                buttonUpdateMetadata.setClickable(false);
                artistEdit.setText("");

            }else {
                artBioText.setVisibility(View.GONE);
                retryText.setText(getString(R.string.artist_info_no_result));
                //retryText.setVisibility(View.GONE);
                retryText.setVisibility(View.VISIBLE);
                TrackItem tempItem = MyApp.getService().getCurrentTrack();
                if(tempItem!=null) {
                    artistEdit.setVisibility(View.VISIBLE);
                    updateTagsText.setVisibility(View.VISIBLE);
                    buttonUpdateMetadata.setVisibility(View.VISIBLE);
                    buttonUpdateMetadata.setClickable(true);
                    artistEdit.setText(tempItem.getArtist());
                }
            }

            if(!((NowPlayingActivity)getActivity()).isArtistLoadedInBack()) {
                new SetBlurryImagetask().execute(artistInfo);
            }
        }
    }

    private class SetBlurryImagetask extends AsyncTask<ArtistInfo, String, Bitmap>{

        Bitmap b ;

        @Override
        protected Bitmap doInBackground(ArtistInfo... params) {

            //store file in cache with artist id as name
            //create folder in cache for artist images
            String CACHE_ART_THUMBS = MyApp.getContext().getCacheDir()+"/art_thumbs/";
            String actual_file_path = CACHE_ART_THUMBS+params[0].getOriginalArtist();
            File f = new File(CACHE_ART_THUMBS);
            if(!f.exists()){
                f.mkdir();
            }
            if(!new File(actual_file_path).exists()){
                //create file
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(new File(actual_file_path));
                    URL url = new URL(params[0].getImageUrl());
                    InputStream inputStream = url.openConnection().getInputStream();
                    int totalSize = url.openConnection().getContentLength();
                    int downloadedSize = 0;
                    byte[] buffer = new byte[1024];
                    int bufferLength = 0;
                    while ( (bufferLength = inputStream.read(buffer)) > 0 )
                    {
                        fos.write(buffer, 0, bufferLength);
                        downloadedSize += bufferLength;
                    }
                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            b= BitmapFactory.decodeFile(actual_file_path);
            return b;
        }

        protected void onPostExecute(Bitmap b) {

            //set background image

            if(b!=null && getActivity()!=null) {
                ((NowPlayingActivity) getActivity()).setBlurryBackground(b);
            }
        }
    }
}
