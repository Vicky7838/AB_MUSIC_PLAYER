package com.music.player.bhandari.m.adapter;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.UIElemetHelper.FontFactory;
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

/**
 * Created by amit on 24/12/16.
 */

public class AlbumLibraryAdapter extends RecyclerView.Adapter<AlbumLibraryAdapter.MyViewHolder>
        implements PopupMenu.OnMenuItemClickListener {

    private Context context;
    private LayoutInflater inflater;
    private PlayerService playerService;
    private boolean mBound=false;
    private ServiceConnection playerServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            PlayerService.PlayerBinder playerBinder = (PlayerService.PlayerBinder) service;
            playerService = playerBinder.getService();
            mBound=true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound=false;
        }
    };
    private int position;
    private ArrayList<dataItem> dataItems=new ArrayList<>();   //actual data
    private ArrayList<dataItem> filteredDataItems=new ArrayList<>();
    private ArrayList<dataItem> reference=new ArrayList<>();
    private BroadcastReceiver mDeleteReceiver;

    public void clear(){
        try {
            if(mBound){
                context.unbindService(playerServiceConnection);
                Log.v(Constants.TAG,"unbind from albumName adappter");
                mBound=false;
            }
        }catch (Exception ignored){
        }
        unregisterReceiver();
    }

    public void sort(int sort_id){
        int sort_order = MyApp.getPref().getInt(context.getResources().getString(R.string.pref_order_by),Constants.SORT_BY.ASC);
        switch (sort_id){
            case Constants.SORT_BY.NAME:
                if(sort_order==Constants.SORT_BY.ASC) {
                    Collections.sort(reference, new Comparator<dataItem>() {
                        @Override
                        public int compare(dataItem o1, dataItem o2) {
                            return o1.albumName.compareToIgnoreCase(o2.albumName);
                        }
                    });
                }else {
                    Collections.sort(reference, new Comparator<dataItem>() {
                        @Override
                        public int compare(dataItem o1, dataItem o2) {
                            return o2.albumName.compareToIgnoreCase(o1.albumName);
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
        }


        notifyDataSetChanged();
    }

    public void registerReceiver(){
        LocalBroadcastManager.getInstance(context).registerReceiver(mDeleteReceiver,
                new IntentFilter(Constants.ACTION.DELETE_RESULT));
    }

    public void unregisterReceiver(){
        LocalBroadcastManager.getInstance(context).unregisterReceiver(mDeleteReceiver);
    }

    public AlbumLibraryAdapter(Context context, ArrayList<dataItem> data){
        this.context=context;
        inflater= LayoutInflater.from(context);
        Intent playerServiceIntent = new Intent(context, PlayerService.class);
        /*try {
            context.bindService(playerServiceIntent, playerServiceConnection, 0);
        }catch (Exception ignored){

        }*/
        playerService = MyApp.getService();
        this.dataItems = data;
        filteredDataItems.addAll(dataItems);
        mDeleteReceiver= new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getIntExtra("status",-1)==Constants.FRAGMENT_STATUS.ALBUM_FRAGMENT_GRID) {
                    if (intent.getIntExtra("error", -1) == Constants.ERROR_CODE.SUCCESS) {
                        Toast.makeText(context, "Deleted " + reference.get(position).title, Toast.LENGTH_SHORT).show();
                        dataItems.remove(dataItems.get(position));
                        filteredDataItems.remove(filteredDataItems.get(position));
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
            for(dataItem d:dataItems){
                if(d.title.toLowerCase().contains(searchQuery)){
                    filteredDataItems.add(d);
                }
            }
        }else {
            filteredDataItems.clear();
            filteredDataItems.addAll(dataItems);
        }
        notifyDataSetChanged();
    }
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.album_card, parent, false);
        return new AlbumLibraryAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {

        reference = filteredDataItems;
        /*Picasso.with(context)
                .load(MusicLibrary.getInstance().getAlbumArtUri(reference.get(position).album_id))
                .placeholder(context.getResources().getDrawable(R.drawable.ic_batman_1))
                .into(holder.thumbnail);*/
        holder.title.setText(reference.get(position).albumName);

        Glide
                .with(context)
                .load(MusicLibrary.getInstance().getAlbumArtUri(reference.get(position).album_id))
                .asBitmap()
                .signature(new StringSignature(String.valueOf(System.currentTimeMillis())))
                .centerCrop()
                .placeholder(R.drawable.ic_batman_1)
                .into(holder.thumbnail);
        holder.count.setText(reference.get(position).artist_name);
    }

    @Override
    public int getItemCount() {
        return filteredDataItems.size();
    }

    public void onClick(View view, int position) {
        this.position=position;
        String title;
        int key;
        switch (view.getId()){
            case R.id.card_view_album:
                title=reference.get(position).albumName;
                key=reference.get(position).album_id;
                Intent intent = new Intent(context,SecondaryLibraryActivity.class);
                intent.putExtra("status",Constants.FRAGMENT_STATUS.ALBUM_FRAGMENT);
                intent.putExtra("key",key);
                intent.putExtra("title",title);
                context.startActivity(intent);
                ((MainActivity)context).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;

            case R.id.overflow:
                PopupMenu popup = new PopupMenu(context, view);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_tracks_by_title, popup.getMenu());
                popup.getMenu().removeItem(R.id.action_set_as_ringtone);
                popup.getMenu().removeItem(R.id.action_track_info);
                popup.getMenu().removeItem(R.id.action_edit_track_info);
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
                String[] song_titles;
                ArrayList<String> temp;
                int album_id=reference.get(position).album_id;
                temp = MusicLibrary.getInstance().getSongListFromAlbumId(album_id,Constants.SORT_ORDER.ASC);
                song_titles = temp.toArray(new String[temp.size()]);
                UtilityFun.AddToPlaylist(context,song_titles);
                break;

            case R.id.action_share:
                ArrayList<Uri> files = new ArrayList<>();  //for sending multiple files
                for( String id : MusicLibrary.getInstance().getSongListFromAlbumId(
                        reference.get(position).album_id,Constants.SORT_ORDER.ASC)){
                    try {
                        File file = new File(MusicLibrary.getInstance().getTrackItemFromTitle(id).getFilePath());
                        Uri fileUri = Uri.fromFile(file);
                        files.add(fileUri);
                    } catch (Exception e){
                        Toast.makeText(context,"Something wrong!",Toast.LENGTH_LONG).show();
                        return true;
                    }
                }
                UtilityFun.Share(context, files, "Multiple audio files");
                break;

            case R.id.action_delete:
                Delete();
                break;

            case R.id.action_play_next:
                AddToQ(Constants.ADD_TO_Q.IMMEDIATE_NEXT);
                break;

            case R.id.action_add_to_q:
                AddToQ(Constants.ADD_TO_Q.AT_LAST);
                break;
        }
        return true;
    }

    private void Play(){
        int album_id=reference.get(position).album_id;
        playerService.setTrackList(MusicLibrary.getInstance().getSongListFromAlbumId(album_id,Constants.SORT_ORDER.ASC));
        playerService.playAtPosition(0);
        /*
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent()
                .setAction(Constants.ACTION.PLAY_AT_POSITION)
                .putExtra("position",0));*/
    }

    private void AddToQ(int positionToAdd){
        //we are using same function for adding to q and playing next
        // toastString is to identify which string to disokay as toast
        String toastString=(positionToAdd==Constants.ADD_TO_Q.AT_LAST ? "added to  queue " : "playing next ") ;
        //when adding to playing next, order of songs should be desc
        //and asc for adding at last
        //this is how the function in player service is writte, deal with it
        int sortOrder=(positionToAdd==Constants.ADD_TO_Q.AT_LAST ? Constants.SORT_ORDER.ASC : Constants.SORT_ORDER.DESC);
        int album_id=reference.get(position).album_id;
        for(String title:MusicLibrary.getInstance().getSongListFromAlbumId(album_id,sortOrder)){
            playerService.addToQ(title, positionToAdd);
        }
        Toast.makeText(context
                ,toastString+reference.get(position).title
                ,Toast.LENGTH_SHORT).show();

    }

    private void Delete(){

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        ArrayList<Integer> ids = new ArrayList<>();
                        ArrayList<File> files = new ArrayList<>();
                        ArrayList<String> tracklist = MusicLibrary.getInstance().getSongListFromAlbumId( reference.get(position).album_id, Constants.SORT_ORDER.ASC);
                        for(String track:tracklist){
                            if(playerService.getCurrentTrack().getTitle().equals(track)){
                                Toast.makeText(context,"One of the song is playing currently",Toast.LENGTH_SHORT).show();
                                return;
                            }
                            TrackItem item = MusicLibrary.getInstance().getTrackItemFromTitle(track);
                            if(item==null){
                                Toast.makeText(context,"Something wrong!",Toast.LENGTH_LONG).show();
                                return;
                            }
                            files.add(new File(item.getFilePath()));
                            ids.add(item.getId());
                        }
                        String[] titles = tracklist.toArray(new String[tracklist.size()]);
                        UtilityFun.Delete(context,ids,files,titles,Constants.FRAGMENT_STATUS.ALBUM_FRAGMENT_GRID);  //last parameter not needed
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

    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView title, count;
        ImageView thumbnail, overflow;

        MyViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            title.setTypeface(FontFactory.getFont());

            count = (TextView) itemView.findViewById(R.id.count);
            count.setTypeface(FontFactory.getFont());

            thumbnail = (ImageView) itemView.findViewById(R.id.thumbnail);
            overflow = (ImageView) itemView.findViewById(R.id.overflow);
            itemView.setOnClickListener(this);
            itemView.findViewById(R.id.overflow).setOnClickListener(this);
        }

        @Override
        public void onClick(View v){
            AlbumLibraryAdapter.this.onClick(v,this.getLayoutPosition());
        }
    }

}
