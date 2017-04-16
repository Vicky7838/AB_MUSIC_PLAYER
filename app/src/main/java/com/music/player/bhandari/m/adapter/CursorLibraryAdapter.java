package com.music.player.bhandari.m.adapter;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.StringSignature;
import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.UIElemetHelper.ColorHelper;
import com.music.player.bhandari.m.UIElemetHelper.FontFactory;
import com.music.player.bhandari.m.activity.ActivityTagEditor;
import com.music.player.bhandari.m.activity.FragmentLibrary;
import com.music.player.bhandari.m.activity.MainActivity;
import com.music.player.bhandari.m.activity.SecondaryLibraryActivity;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.model.TrackItem;
import com.music.player.bhandari.m.model.dataItem;
import com.music.player.bhandari.m.utils.MusicLibrary;
import com.music.player.bhandari.m.service.PlayerService;
import com.music.player.bhandari.m.utils.MyApp;
import com.music.player.bhandari.m.utils.UtilityFun;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;

/**
 * Created by amit on 24/12/16.
 */

public class CursorLibraryAdapter extends RecyclerView.Adapter<CursorLibraryAdapter.MyViewHolder>
        implements PopupMenu.OnMenuItemClickListener {

    private Context context;
    private LayoutInflater inflater;
    private FragmentLibrary fl;
    private PopupMenu popup;
    private ArrayList<String> title_list =new ArrayList<>();
    private PlayerService playerService;
    private boolean mBound=false;
    private int position;
    private ArrayList<dataItem> dataItems=new ArrayList<>();
    private ArrayList<dataItem> filteredDataItems=new ArrayList<>();
    private ArrayList<dataItem> reference=new ArrayList<>();

    private BroadcastReceiver mDeleteReceiver;
    private int mItemHeight;
    private long mLastClickTime;
    public CursorLibraryAdapter(final FragmentLibrary fl, final Context context, ArrayList<dataItem> data){
        this.context=context;
        inflater= LayoutInflater.from(context);
        this.fl=fl;
        this.dataItems = data;
        filteredDataItems.addAll(dataItems);
        reference = filteredDataItems;
        //sort(MyApp.getPref().getInt(currentPageSort,Constants.SORT_BY.NAME));
        playerService = MyApp.getService();
        mDeleteReceiver= new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //as multiple objects in existence at time
                //it will be received by all the objects
                // we have to skip unwanted ones and proceed on one on which delete action was invoked
                if(intent.getIntExtra("status",-1)==fl.getStatus()) {
                    if (intent.getIntExtra("error", -1) == Constants.ERROR_CODE.SUCCESS) {
                        Toast.makeText(context, "Deleted " + reference.get(position).title, Toast.LENGTH_SHORT).show();
                        dataItems.remove(dataItems.get(position));
                        reference.remove(reference.get(position));
                        notifyItemRemoved(position);
                        notifyDataSetChanged();
                    } else {
                        Toast.makeText(context, "Cannot delete " + reference.get(position).title, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };
    }

    public void filter(String searchQuery){
        if(!searchQuery.equals("")){
            filteredDataItems.clear();
            switch (fl.getStatus()){
                case Constants.FRAGMENT_STATUS.ALBUM_FRAGMENT:
                    for(dataItem d:dataItems){
                        if(d.title.toLowerCase().contains(searchQuery)){
                            filteredDataItems.add(d);
                        }
                    }
                    break;

                case Constants.FRAGMENT_STATUS.ARTIST_FRAGMENT:
                    for(dataItem d:dataItems){
                        if(d.title.toLowerCase().contains(searchQuery)){
                            filteredDataItems.add(d);
                        }
                    }
                    break;

                case Constants.FRAGMENT_STATUS.GENRE_FRAGMENT:
                    for(dataItem d:dataItems){
                        if(d.title.toLowerCase().contains(searchQuery)){
                            filteredDataItems.add(d);
                        }
                    }
                    break;

                case Constants.FRAGMENT_STATUS.TITLE_FRAGMENT:
                    for(dataItem d:dataItems){
                        if(d.title.toLowerCase().contains(searchQuery)
                                || d.artist_name.toLowerCase().contains(searchQuery)
                                || d.albumName.toLowerCase().contains(searchQuery)){
                            filteredDataItems.add(d);
                        }
                    }
                    break;
            }
        }else {
            filteredDataItems.clear();
            filteredDataItems.addAll(dataItems);
        }
        notifyDataSetChanged();
    }

    @Override
    public MyViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        final View parentView = inflater.inflate(R.layout.fragment_library_item, parent, false);

        int color = ColorHelper.getBaseThemeTextColor() ;
        ((TextView)(parentView.findViewById(R.id.header))).setTextColor(color);
        ((TextView)(parentView.findViewById(R.id.secondaryHeader))).setTextColor(color);
        ((TextView)(parentView.findViewById(R.id.count))).setTextColor(color);
        ((ImageView)(parentView.findViewById(R.id.menuPopup))).setColorFilter(color);


        return  new CursorLibraryAdapter.MyViewHolder(parentView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {

       // mItemHeight = holder.itemView.getMeasuredHeight();
        reference = filteredDataItems;

        switch (fl.getStatus()) {
            case Constants.FRAGMENT_STATUS.TITLE_FRAGMENT:
                holder.artistGenreText.setVisibility(View.GONE);
                Glide
                        .with(context)
                        .load(MusicLibrary.getInstance().getAlbumArtUri(reference.get(position).album_id))
                        .placeholder(R.drawable.ic_batman_1)
                        //.centerCrop()
                        .crossFade()
                        .signature(new StringSignature(String.valueOf(System.currentTimeMillis())))
                        .override(100,100)
                        //.signature(new StringSignature(String.valueOf(System.currentTimeMillis())))
                        .into(holder.image);
                holder.title.setText(reference.get(position).title);
                holder.count.setText(UtilityFun.msToString(Integer.parseInt(reference.get(position).duration)));
                String secText = reference.get(position).artist_name + " | " + reference.get(position).albumName;
                holder.secondary.setText(secText);
                break;

            case Constants.FRAGMENT_STATUS.ALBUM_FRAGMENT:
                holder.artistGenreText.setVisibility(View.GONE);
                Glide.with(context)
                        .load(MusicLibrary.getInstance().getAlbumArtUri(reference.get(position).album_id))
                        .placeholder(R.drawable.ic_batman_1)
                        .into(holder.image);
                holder.title.setText(reference.get(position).albumName);
                int trackCount= reference.get(position).numberOfTracks;
                String trackCoun ;
                if(trackCount>1)
                    trackCoun = trackCount+" tracks";
                else
                    trackCoun = trackCount+" track";
                holder.secondary.setText(reference.get(position).artist_name + " | " + trackCoun);
                break;

            case Constants.FRAGMENT_STATUS.ARTIST_FRAGMENT:
                holder.image.setVisibility(View.GONE);
                if(reference.get(position).artist_name.length()>=2) {
                    holder.artistGenreText.setText(reference.get(position).artist_name.substring(0, 2));
                }
                holder.artistGenreText.setTextColor(ColorHelper.getBaseThemeTextColor());
                //holder.cardAlbumArt.setCardBackgroundColor(Color.parseColor(Constants.COLOR_ARRAY.colorArray[new Random()
                  //     .nextInt(Constants.COLOR_ARRAY.COLOR_ARR_SIZE)]));
                holder.cardAlbumArt.setCardBackgroundColor(context.getResources().getColor(R.color.blackTransparentLight));
                holder.title.setText(reference.get(position).artist_name);
                int tracksCount=reference.get(position).numberOfTracks;
                int albumCount=reference.get(position).numberOfAlbums;
                StringBuilder stringBuilder = new StringBuilder().append(tracksCount+" tracks").append(" | ")
                        .append(albumCount+ " albums");
                holder.secondary.setText(stringBuilder);
                break;

            case Constants.FRAGMENT_STATUS.GENRE_FRAGMENT:
                holder.image.setVisibility(View.GONE);
                if(reference.get(position).title.length()>=2) {
                    holder.artistGenreText.setText(reference.get(position).title.substring(0, 2));
                }
                holder.artistGenreText.setTextColor(ColorHelper.getBaseThemeTextColor());
               /* holder.cardAlbumArt.setCardBackgroundColor
                        (Color.parseColor(Constants.COLOR_ARRAY.colorArray[new Random()
                                .nextInt(Constants.COLOR_ARRAY.COLOR_ARR_SIZE)]));*/
                holder.cardAlbumArt.setCardBackgroundColor(context.getResources().getColor(R.color.blackTransparentLight));
                holder.title.setText(reference.get(position).title);
               // holder.numberOfTracks.setText(cursor.getString(MusicLibrary.INDEX_FOR_GENRE_CURSOR.NUMBER_OF_TRACKS));
               // holder.secondary.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return filteredDataItems.size();
    }

    public void clear(){
        title_list.clear();
        inflater=null;
        fl=null;
        popup=null;
        try {
            unregisterReceiver();
        }catch (Exception e){

        }
    }

    public void registerReceiver(){
        LocalBroadcastManager.getInstance(context).registerReceiver(mDeleteReceiver,
                new IntentFilter(Constants.ACTION.DELETE_RESULT));
    }

    public void unregisterReceiver(){
        LocalBroadcastManager.getInstance(context).unregisterReceiver(mDeleteReceiver);
    }

    public void onClick(View view, final int position) {
        this.position=position;
        switch (view.getId()){
            case R.id.trackItem:
                if (SystemClock.elapsedRealtime() - mLastClickTime < 500){
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        String title="";
                        int key=0;
                        switch (fl.getStatus()) {
                            case Constants.FRAGMENT_STATUS.TITLE_FRAGMENT:
                                if(MyApp.isLocked()){
                                    new Handler(context.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(context,"Music is Locked!",Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    return;
                                }
                                Play();
                                break;


                            case Constants.FRAGMENT_STATUS.ALBUM_FRAGMENT:
                                title=reference.get(position).albumName;
                                key=reference.get(position).album_id;
                                break;

                            case Constants.FRAGMENT_STATUS.ARTIST_FRAGMENT:
                                title=reference.get(position).artist_name;
                                key=reference.get(position).artist_id;
                                break;

                            case Constants.FRAGMENT_STATUS.GENRE_FRAGMENT:
                                title=reference.get(position).title;
                                key=reference.get(position).id;
                                break;
                        }
                        if(fl.getStatus()!=Constants.FRAGMENT_STATUS.TITLE_FRAGMENT){
                            Intent intent = new Intent(context,SecondaryLibraryActivity.class);
                            intent.putExtra("status",fl.getStatus());
                            intent.putExtra("key",key);
                            intent.putExtra("title",title);
                            context.startActivity(intent);
                            ((MainActivity)context).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                            //Toast.makeText(context, "Open secondary library", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                break;

            case R.id.menuPopup:
                popup=new PopupMenu(context,view);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_tracks_by_title, popup.getMenu());
                if(fl.getStatus()!=Constants.FRAGMENT_STATUS.TITLE_FRAGMENT){
                    popup.getMenu().removeItem(R.id.action_set_as_ringtone);
                    popup.getMenu().removeItem(R.id.action_track_info);
                    popup.getMenu().removeItem(R.id.action_edit_track_info);
                }
                popup.show();
                popup.setOnMenuItemClickListener(this);
                break;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_play:
                if(MyApp.isLocked()){
                    Toast.makeText(context,"Music is Locked!",Toast.LENGTH_SHORT).show();
                    return true;
                }
                Play();
                break;

            case R.id.action_add_to_playlist:
                //Toast.makeText(context,"Playlists coming soon" ,Toast.LENGTH_SHORT).show();
                AddToPlaylist();
                break;

            case R.id.action_share:
                try {
                    Share();
                }catch (Exception e){
                    Toast.makeText(context,"Something wrong!",Toast.LENGTH_LONG).show();
                }
                break;

            case R.id.action_delete:
                DeleteDialog();
                break;

            case R.id.action_play_next:
                AddToQ(Constants.ADD_TO_Q.IMMEDIATE_NEXT);
                break;

            case R.id.action_add_to_q:
                AddToQ(Constants.ADD_TO_Q.AT_LAST);
                break;

            case R.id.action_set_as_ringtone:
                UtilityFun.SetRingtone(context, filteredDataItems.get(position).file_path
                        ,filteredDataItems.get(position).title);
                break;

            case R.id.action_track_info:
                setTrackInfoDialog();
                break;

            case R.id.action_edit_track_info:
                context.startActivity(new Intent(context, ActivityTagEditor.class)
                        .putExtra("from",Constants.TAG_EDITOR_LAUNCHED_FROM.MAIN_LIB)
                        .putExtra("file_path",filteredDataItems.get(position).file_path)
                        .putExtra("track_title",filteredDataItems.get(position).title)
                        .putExtra("position",position));
                ((MainActivity)context).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
        }
        return true;
    }

    public void sort(int sort_id){
        int sort_order = MyApp.getPref().getInt(context.getResources().getString(R.string.pref_order_by),Constants.SORT_BY.ASC);
        switch (sort_id){
            case Constants.SORT_BY.NAME:
                if(sort_order==Constants.SORT_BY.ASC) {
                    Collections.sort(reference, new Comparator<dataItem>() {
                        @Override
                        public int compare(dataItem o1, dataItem o2) {
                            return o1.title.compareToIgnoreCase(o2.title);
                        }
                    });
                }else {
                    Collections.sort(reference, new Comparator<dataItem>() {
                        @Override
                        public int compare(dataItem o1, dataItem o2) {
                            return o2.title.compareToIgnoreCase(o1.title);
                        }
                    });
                }

                break;

            case Constants.SORT_BY.SIZE:
                if(sort_order==Constants.SORT_BY.ASC) {
                    Collections.sort(reference, new Comparator<dataItem>() {
                        @Override
                        public int compare(dataItem o1, dataItem o2) {
                            return (int) (new File(o1.file_path).length() - new File(o2.file_path).length());
                        }
                    });
                }else {
                    Collections.sort(reference, new Comparator<dataItem>() {
                        @Override
                        public int compare(dataItem o1, dataItem o2) {
                            return (int) (new File(o2.file_path).length() - new File(o1.file_path).length());
                        }
                    });
                }
                break;

            case Constants.SORT_BY.YEAR:
                if(sort_order==Constants.SORT_BY.ASC) {
                    Collections.sort(reference, new Comparator<dataItem>() {
                        @Override
                        public int compare(dataItem o1, dataItem o2) {
                            return o1.year.compareToIgnoreCase(o2.year);
                        }
                    });
                }else {
                    Collections.sort(reference, new Comparator<dataItem>() {
                        @Override
                        public int compare(dataItem o1, dataItem o2) {
                            return o2.year.compareToIgnoreCase(o1.year);
                        }
                    });
                }
                break;

            case Constants.SORT_BY.NO_OF_ALBUMS:
                if(sort_order==Constants.SORT_BY.ASC) {
                    Collections.sort(reference, new Comparator<dataItem>() {
                        @Override
                        public int compare(dataItem o1, dataItem o2) {
                            return o1.numberOfAlbums - o2.numberOfAlbums;
                        }
                    });
                }else {
                    Collections.sort(reference, new Comparator<dataItem>() {
                        @Override
                        public int compare(dataItem o1, dataItem o2) {
                            return o2.numberOfAlbums - o1.numberOfAlbums;
                        }
                    });
                }
                break;

            case Constants.SORT_BY.NO_OF_TRACKS:
                if(sort_order==Constants.SORT_BY.ASC) {
                    Collections.sort(reference, new Comparator<dataItem>() {
                        @Override
                        public int compare(dataItem o1, dataItem o2) {
                            return o1.numberOfTracks - o2.numberOfTracks;
                        }
                    });
                }else {
                    Collections.sort(reference, new Comparator<dataItem>() {
                        @Override
                        public int compare(dataItem o1, dataItem o2) {
                            return o2.numberOfTracks - o1.numberOfTracks;
                        }
                    });
                }
                break;

            case Constants.SORT_BY.DURATION:
                if(sort_order==Constants.SORT_BY.ASC) {
                    Collections.sort(reference, new Comparator<dataItem>() {
                        @Override
                        public int compare(dataItem o1, dataItem o2) {
                            return Integer.valueOf(o1.duration) -Integer.valueOf(o2.duration) ;
                        }
                    });
                }else {
                    Collections.sort(reference, new Comparator<dataItem>() {
                        @Override
                        public int compare(dataItem o1, dataItem o2) {
                            return Integer.valueOf(o2.duration) -Integer.valueOf(o1.duration) ;
                        }
                    });
                }
                break;
        }
        notifyDataSetChanged();
    }

    private void setTrackInfoDialog(){
        final AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle("Track Info");
        LinearLayout linear = new LinearLayout(context);
        linear.setOrientation(LinearLayout.VERTICAL);
        final TextView text = new TextView(context);
        text.setText(UtilityFun.trackInfoBuild(reference.get(position).title).toString());

        text.setPadding(20, 20,20,10);
        text.setTextSize(15);
        //text.setGravity(Gravity.CENTER);

        linear.addView(text);
        alert.setView(linear);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        alert.show();
    }

    private void Play(){

        switch (fl.getStatus()) {
            case Constants.FRAGMENT_STATUS.TITLE_FRAGMENT:

                if (playerService.getStatus() == PlayerService.PLAYING)
                    playerService.pause();
                title_list.clear();
                for(dataItem d:filteredDataItems){
                    title_list.add(d.title);
                }
                playerService.setTrackList(title_list);
                playerService.playAtPosition(position);
                /*
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent()
                        .setAction(Constants.ACTION.PLAY_AT_POSITION)
                        .putExtra("position",position));*/

                break;

            case Constants.FRAGMENT_STATUS.ALBUM_FRAGMENT:
                int album_id=reference.get(position).album_id;
                playerService.setTrackList(MusicLibrary.getInstance().getSongListFromAlbumId(album_id,Constants.SORT_ORDER.ASC));
                break;

            case Constants.FRAGMENT_STATUS.ARTIST_FRAGMENT:
                int artist_id=reference.get(position).artist_id;
                playerService.setTrackList(MusicLibrary.getInstance().getSongListFromArtistId(artist_id,Constants.SORT_ORDER.ASC));
                break;

            case Constants.FRAGMENT_STATUS.GENRE_FRAGMENT:
                int genre_id=reference.get(position).id;
                playerService.setTrackList(MusicLibrary.getInstance().getSongListFromGenreId(genre_id,Constants.SORT_ORDER.ASC));
                break;
        }
        if(fl.getStatus()!=Constants.FRAGMENT_STATUS.TITLE_FRAGMENT){

            playerService.playAtPosition(0);/*
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent()
                    .setAction(Constants.ACTION.PLAY_AT_POSITION)
                    .putExtra("position",0));*/
        }
    }

    private void AddToPlaylist(){
        String[] song_titles;
        ArrayList<String> temp;
        switch (fl.getStatus()) {
            case Constants.FRAGMENT_STATUS.TITLE_FRAGMENT:
                song_titles=new String[]{reference.get(position).title};
                UtilityFun.AddToPlaylist(context, song_titles);
                break;

            case Constants.FRAGMENT_STATUS.ALBUM_FRAGMENT:
                int album_id=reference.get(position).album_id;
                temp = MusicLibrary.getInstance().getSongListFromAlbumId(album_id,Constants.SORT_ORDER.ASC);
                song_titles = temp.toArray(new String[temp.size()]);
                UtilityFun.AddToPlaylist(context, song_titles);
                break;

            case Constants.FRAGMENT_STATUS.ARTIST_FRAGMENT:
                int artist_id=reference.get(position).artist_id;
                temp = MusicLibrary.getInstance().getSongListFromArtistId(artist_id,Constants.SORT_ORDER.ASC);
                song_titles = temp.toArray(new String[temp.size()]);
                UtilityFun.AddToPlaylist(context, song_titles);
                break;

            case Constants.FRAGMENT_STATUS.GENRE_FRAGMENT:
                int genre_id=reference.get(position).id;
                temp = MusicLibrary.getInstance().getSongListFromGenreId(genre_id,Constants.SORT_ORDER.ASC);
                song_titles = temp.toArray(new String[temp.size()]);
                UtilityFun.AddToPlaylist(context, song_titles);
                break;
        }
        //
    }

    private void Share(){
        ArrayList<Uri> files = new ArrayList<Uri>();  //for sending multiple files

        switch (fl.getStatus()) {
            case Constants.FRAGMENT_STATUS.TITLE_FRAGMENT:
                File fileToBeShared = new File(reference.get(position).file_path);
                files.add(Uri.fromFile(fileToBeShared));
                UtilityFun.Share(context, files, reference.get(position).title);
                break;

            case Constants.FRAGMENT_STATUS.ALBUM_FRAGMENT:
                int album_id = reference.get(position).album_id;
                for (String id : MusicLibrary.getInstance().getSongListFromAlbumId(album_id, Constants.SORT_ORDER.ASC)) {
                    File file = new File(MusicLibrary.getInstance().getTrackItemFromTitle(id).getFilePath());
                    Uri fileUri = Uri.fromFile(file);
                    files.add(fileUri);
                }
                break;

            case Constants.FRAGMENT_STATUS.ARTIST_FRAGMENT:
                int artist_id = reference.get(position).artist_id;
                for (String id : MusicLibrary.getInstance().getSongListFromArtistId(artist_id, Constants.SORT_ORDER.ASC)) {
                    File file = new File(MusicLibrary.getInstance().getTrackItemFromTitle(id).getFilePath());
                    Uri fileUri = Uri.fromFile(file);
                    files.add(fileUri);
                }
                break;

            case Constants.FRAGMENT_STATUS.GENRE_FRAGMENT:
                int genre_id = reference.get(position).id;
                for (String id : MusicLibrary.getInstance().getSongListFromAlbumId(genre_id, Constants.SORT_ORDER.ASC)) {
                    File file = new File(MusicLibrary.getInstance().getTrackItemFromTitle(id).getFilePath());
                    Uri fileUri = Uri.fromFile(file);
                    files.add(fileUri);
                }
                break;
        }
        if(fl.getStatus()!=Constants.FRAGMENT_STATUS.TITLE_FRAGMENT) {
            UtilityFun.Share(context, files, "Multiple audio files");
        }
    }

    private void AddToQ(int positionToAdd){
        //we are using same function for adding to q and playing next
        // toastString is to identify which string to disokay as toast
        String toastString=(positionToAdd==Constants.ADD_TO_Q.AT_LAST ? "added to  queue " : "playing next ") ;
        //when adding to playing next, order of songs should be desc
        //and asc for adding at last
        //this is how the function in player service is writte, deal with it
        int sortOrder=(positionToAdd==Constants.ADD_TO_Q.AT_LAST ? Constants.SORT_ORDER.ASC : Constants.SORT_ORDER.DESC);
        switch (fl.getStatus()) {
            case Constants.FRAGMENT_STATUS.TITLE_FRAGMENT:
                playerService.addToQ(reference.get(position).title, positionToAdd);
                Toast.makeText(context
                        ,toastString+reference.get(position).title
                        ,Toast.LENGTH_SHORT).show();
                break;

            case Constants.FRAGMENT_STATUS.ALBUM_FRAGMENT:
                int album_id=reference.get(position).album_id;
                for(String title:MusicLibrary.getInstance().getSongListFromAlbumId(album_id,sortOrder)){
                    playerService.addToQ(title, positionToAdd);
                }
                Toast.makeText(context
                        ,toastString+reference.get(position).title
                        ,Toast.LENGTH_SHORT).show();
                break;

            case Constants.FRAGMENT_STATUS.ARTIST_FRAGMENT:
                int artist_id=reference.get(position).artist_id;
                for(String title:MusicLibrary.getInstance().getSongListFromArtistId(artist_id,sortOrder)){
                    playerService.addToQ(title,  positionToAdd);
                }
                Toast.makeText(context
                        ,toastString+reference.get(position).title
                        ,Toast.LENGTH_SHORT).show();
                break;

            case Constants.FRAGMENT_STATUS.GENRE_FRAGMENT:
                int genre_id=reference.get(position).id;
                for(String title:MusicLibrary.getInstance().getSongListFromGenreId(genre_id,sortOrder)){
                    playerService.addToQ(title, positionToAdd);
                }
                Toast.makeText(context
                        ,toastString+reference.get(position).title
                        ,Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void DeleteDialog(){

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        ArrayList<File> files = new ArrayList<>();
                        ArrayList<Integer> ids = new ArrayList<>();
                        ArrayList<String> song_titles = new ArrayList<>();
                        ArrayList<String> tracklist;
                        String[] titles;
                        switch (fl.getStatus()) {
                            case Constants.FRAGMENT_STATUS.TITLE_FRAGMENT:
                                if(playerService.getCurrentTrack().getTitle().equals(reference.get(position).title)){
                                    Toast.makeText(context,"Cannot delete currently playing song",Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                //delete the file first
                                files.add(new File(reference.get(position).file_path));
                                ids.add(reference.get(position).id);
                                song_titles.add(reference.get(position).title);
                                titles = song_titles.toArray(new String[song_titles.size()]);
                                UtilityFun.Delete(context,ids,files,titles,fl.getStatus());
                                break;

                            case Constants.FRAGMENT_STATUS.ALBUM_FRAGMENT:
                                tracklist = MusicLibrary.getInstance().getSongListFromAlbumId(
                                        reference.get(position).album_id, Constants.SORT_ORDER.ASC);
                                for(String track:tracklist){
                                    if(playerService.getCurrentTrack().getTitle().equals(track)){
                                        Toast.makeText(context,"One of the song is playing currently",Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    TrackItem item = MusicLibrary.getInstance().getTrackItemFromTitle(track);
                                    if(item!=null) {
                                        files.add(new File(item.getFilePath()));
                                        ids.add(item.getId());
                                    }
                                }
                                titles = tracklist.toArray(new String[tracklist.size()]);
                                UtilityFun.Delete(context,ids,files,titles,fl.getStatus());
                                break;

                            case Constants.FRAGMENT_STATUS.ARTIST_FRAGMENT:
                                tracklist = MusicLibrary.getInstance().getSongListFromArtistId(
                                        reference.get(position).artist_id, Constants.SORT_ORDER.ASC);
                                for(String track:tracklist){
                                    if(playerService.getCurrentTrack().getTitle().equals(track)){
                                        Toast.makeText(context,"One of the song is playing currently",Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    TrackItem item = MusicLibrary.getInstance().getTrackItemFromTitle(track);
                                    if(item!=null) {
                                        files.add(new File(item.getFilePath()));
                                        ids.add(item.getId());
                                    }
                                }
                                titles = tracklist.toArray(new String[tracklist.size()]);
                                UtilityFun.Delete(context,ids,files,titles,fl.getStatus());
                                break;

                            case Constants.FRAGMENT_STATUS.GENRE_FRAGMENT:
                                tracklist = MusicLibrary.getInstance().getSongListFromGenreId(
                                        reference.get(position).id, Constants.SORT_ORDER.ASC);
                                for(String track:tracklist){
                                    if(playerService.getCurrentTrack().getTitle().equals(track)){
                                        Toast.makeText(context,"One of the song is playing currently",Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    TrackItem item = MusicLibrary.getInstance().getTrackItemFromTitle(track);
                                    if(item!=null) {
                                        files.add(new File(item.getFilePath()));
                                        ids.add(item.getId());
                                    }
                                }
                                titles = tracklist.toArray(new String[tracklist.size()]);
                                UtilityFun.Delete(context,ids,files,titles,fl.getStatus());
                                break;
                        }
                        //Yes button clicked
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    public void updateItem(int position, String ... param){
        if(param.length==1){
            reference.get(position).title = param[0];
            notifyItemChanged(position);
        }else {
           reference.get(position).title = param[0];
           reference.get(position).artist_name = param[1];
           reference.get(position).albumName = param[2];
           notifyItemChanged(position);
        }
    }

    public int getHeight(){
        return mItemHeight;
    }

    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView title,secondary,count,artistGenreText;
        ImageView image;
        CardView cardAlbumArt;

        MyViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.header);
            title.setTypeface(FontFactory.getFont());

            secondary = (TextView) itemView.findViewById(R.id.secondaryHeader);
            secondary.setTypeface(FontFactory.getFont());

            count = (TextView) itemView.findViewById(R.id.count);
            count.setTypeface(FontFactory.getFont());

            cardAlbumArt=(CardView) itemView.findViewById(R.id.cardViewForAlbumGragment);
            image=(ImageView)itemView.findViewById(R.id.imageVIewForStubAlbumArt);

            artistGenreText=(TextView)itemView.findViewById(R.id.text_view_for_Artist_genre);
            artistGenreText.setTypeface(FontFactory.getFont());

            itemView.setOnClickListener(this);
            itemView.findViewById(R.id.menuPopup).setOnClickListener(this);
        }

        @Override
        public void onClick(View v){
            CursorLibraryAdapter.this.onClick(v,this.getLayoutPosition());
        }


    }
}
