package com.music.player.bhandari.m.adapter;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
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
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.UIElemetHelper.ColorHelper;
import com.music.player.bhandari.m.UIElemetHelper.FontFactory;
import com.music.player.bhandari.m.activity.MainActivity;
import com.music.player.bhandari.m.activity.SecondaryLibraryActivity;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.service.PlayerService;
import com.music.player.bhandari.m.utils.MusicLibrary;
import com.music.player.bhandari.m.utils.MyApp;
import com.music.player.bhandari.m.utils.PlaylistManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by amit on 12/1/17.
 */

public class PlaylistLibraryAdapter extends RecyclerView.Adapter<PlaylistLibraryAdapter.MyViewHolder>
        implements PopupMenu.OnMenuItemClickListener{

    private ArrayList<String> headers=new ArrayList<>();
    private Context context;
    private LayoutInflater inflater;
    private int position=0;
    private PlayerService playerService;
    private boolean mBound=false;
    private ServiceConnection playerServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            PlayerService.PlayerBinder playerBinder = (PlayerService.PlayerBinder) service;
            playerService = playerBinder.getService();
            mBound=true;
            Log.v(getClass().toString(),"Serive bound to library adapter");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound=false;
        }
    };

    public PlaylistLibraryAdapter(Context context){
        //create first page for folder fragment
        this.context=context;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        inflater=LayoutInflater.from(context);
        headers = PlaylistManager.getInstance(context).GetPlaylistList(false);
        /*
        Intent playerServiceIntent = new Intent(context, PlayerService.class);
        context.bindService(playerServiceIntent, playerServiceConnection, Context.BIND_AUTO_CREATE);*/
        playerService = MyApp.getService();
    }

    public void clear(){
        try {
            if(mBound){
                context.unbindService(playerServiceConnection);
                mBound=false;
            }
        }catch (Exception e){
        }
    }

    public void refreshPlaylistList(){
        headers = PlaylistManager.getInstance(context).GetPlaylistList(false);
        notifyDataSetChanged();
    }
    @Override
    public PlaylistLibraryAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.fragment_library_item, parent, false);
        final PlaylistLibraryAdapter.MyViewHolder holder=new PlaylistLibraryAdapter.MyViewHolder (view);
        int color = ColorHelper.getBaseThemeTextColor() ;
        ((TextView)(view.findViewById(R.id.header))).setTextColor(color);
        ((TextView)(view.findViewById(R.id.secondaryHeader))).setTextColor(color);
        ((TextView)(view.findViewById(R.id.count))).setTextColor(color);
        ((ImageView)(view.findViewById(R.id.menuPopup))).setColorFilter(color);


        return holder;
    }

    @Override
    public void onBindViewHolder(PlaylistLibraryAdapter.MyViewHolder holder, int position) {
        holder.artistGenreText.setText(headers.get(position).substring(0,2));
        /*holder.cardAlbumArt.setCardBackgroundColor(Color.parseColor(Constants.COLOR_ARRAY.colorArray[new Random()
                .nextInt(Constants.COLOR_ARRAY.COLOR_ARR_SIZE)]));*/
        holder.cardAlbumArt.setCardBackgroundColor(context.getResources().getColor(R.color.blackTransparentLight));
        holder.artistGenreText.setTextColor(ColorHelper.getBaseThemeTextColor());
        holder.title.setText(headers.get(position));
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

            case R.id.action_share:
                Share();
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
        ArrayList<String> trackList = PlaylistManager.getInstance(context).GetPlaylist(headers.get(position));
        if(!trackList.isEmpty()) {
            playerService.setTrackList(trackList);
            playerService.playAtPosition(0);
            /*
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent()
                    .setAction(Constants.ACTION.PLAY_AT_POSITION)
                    .putExtra("position",0));*/
        }else {
            Toast.makeText(context,"empty playlist",Toast.LENGTH_SHORT).show();
        }
    }

    private void Share(){
        ArrayList<Uri> files = new ArrayList<Uri>();  //for sending multiple files
        for( String title : PlaylistManager.getInstance(context).GetPlaylist(headers.get(position))){
            try {
                File file = new File(MusicLibrary.getInstance().getTrackItemFromTitle(title).getFilePath());
                Uri fileUri = Uri.fromFile(file);
                files.add(fileUri);
            }
            catch (Exception e ){
                Toast.makeText(context,"Something wrong!",Toast.LENGTH_LONG).show();
                return;
            }
        }
        if(!files.isEmpty()) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("*/*");
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
            context.startActivity(Intent.createChooser(intent, "multiple audio files"));
        }else {
            Toast.makeText(context,"empty playlist",Toast.LENGTH_SHORT).show();
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
        ArrayList<String> trackList=PlaylistManager.getInstance(context).GetPlaylist(headers.get(position));
        if(!trackList.isEmpty()) {
            for (String title : trackList) {
                playerService.addToQ(title, positionToAdd);
            }
            Toast.makeText(context
                    , toastString + headers.get(position)
                    , Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(context,"empty playlist",Toast.LENGTH_SHORT).show();
        }
    }

    private void Delete(){

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //if tried deleting system playlist, give error
                        if(headers.get(position).equals(Constants.SYSTEM_PLAYLISTS.RECENTLY_ADDED)
                                || headers.get(position).equals(Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED)
                                || headers.get(position).equals(Constants.SYSTEM_PLAYLISTS.MOST_PLAYED)
                                || headers.get(position).equals(Constants.SYSTEM_PLAYLISTS.MY_FAV))
                        {
                            Toast.makeText(context,"Cannot delete "+headers.get(position),Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if(PlaylistManager.getInstance(context).DeletePlaylist(headers.get(position))){
                            Toast.makeText(context,"Deleted "+headers.get(position),Toast.LENGTH_SHORT).show();
                            headers.remove(headers.get(position));
                            notifyDataSetChanged();
                        }else {
                            Toast.makeText(context,"Cannot delete "+headers.get(position),Toast.LENGTH_SHORT).show();
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
    @Override
    public int getItemCount() {
        return headers.size();
    }

    public void onClick(View view, int position) {
        this.position=position;
        switch (view.getId()){
            case R.id.trackItem:
                Intent intent = new Intent(context,SecondaryLibraryActivity.class);
                intent.putExtra("status",Constants.FRAGMENT_STATUS.PLAYLIST_FRAGMENT);
                intent.putExtra("title",headers.get(position));
                context.startActivity(intent);
                ((MainActivity)context).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;

            case R.id.menuPopup:
                PopupMenu popup = new PopupMenu(context, view);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_tracks_by_title, popup.getMenu());
                popup.getMenu().removeItem(R.id.action_set_as_ringtone);
                popup.getMenu().removeItem(R.id.action_add_to_playlist);
                popup.getMenu().removeItem(R.id.action_track_info);
                popup.getMenu().removeItem(R.id.action_edit_track_info);
                popup.show();
                popup.setOnMenuItemClickListener(this);
                break;
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView title,artistGenreText;
        CardView cardAlbumArt;

        public MyViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.header);
            title.setTypeface(FontFactory.getFont());

            cardAlbumArt=(CardView) itemView.findViewById(R.id.cardViewForAlbumGragment);

            artistGenreText=(TextView)itemView.findViewById(R.id.text_view_for_Artist_genre);
            artistGenreText.setTypeface(FontFactory.getFont());

            itemView.findViewById(R.id.imageVIewForStubAlbumArt).setVisibility(View.GONE);
            itemView.setOnClickListener(this);
            itemView.findViewById(R.id.menuPopup).setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            PlaylistLibraryAdapter.this.onClick(view,getLayoutPosition());
        }
    }
}
