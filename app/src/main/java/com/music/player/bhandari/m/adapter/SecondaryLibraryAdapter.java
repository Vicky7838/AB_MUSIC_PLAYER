package com.music.player.bhandari.m.adapter;

import android.app.Activity;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.UIElemetHelper.ColorHelper;
import com.music.player.bhandari.m.UIElemetHelper.FontFactory;
import com.music.player.bhandari.m.activity.ActivityTagEditor;
import com.music.player.bhandari.m.activity.NowPlayingActivity;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.model.dataItem;
import com.music.player.bhandari.m.utils.MusicLibrary;
import com.music.player.bhandari.m.model.TrackItem;
import com.music.player.bhandari.m.service.PlayerService;
import com.music.player.bhandari.m.utils.MyApp;
import com.music.player.bhandari.m.utils.PlaylistManager;
import com.music.player.bhandari.m.utils.UtilityFun;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by amit on 6/12/16.
 */

public class SecondaryLibraryAdapter extends RecyclerView.Adapter<SecondaryLibraryAdapter.MyViewHolder>
        implements PopupMenu.OnMenuItemClickListener {

    private final String REMOVE = "Remove";
    private ArrayList<dataItem> dataItems = new ArrayList<>();


    private Context context;
    private LayoutInflater inflater;
    private int position=0;

    private String clikedON;
    private int status;
    private String playlist_name;
    BroadcastReceiver mDeleteReceiver;

    private PlayerService playerService;

    public SecondaryLibraryAdapter(Context context, ArrayList<String> data){
        this.context=context;
        inflater=LayoutInflater.from(context);

        for (dataItem d: MusicLibrary.getInstance().getDataItemsForTracks()){
            if(data.contains(d.title))
                dataItems.add(d);
        }

        //add extra empty element
        dataItems.add(new dataItem(0,"",0,"",0,"","","",""));
        bindService();
        InitializeDeleteReceiver();
    }

    public SecondaryLibraryAdapter(Context context, ArrayList<String> data, int status, String playlist_name){
        this.playlist_name = playlist_name;
        this.context=context;
        inflater=LayoutInflater.from(context);
        if(data.size()==0){
            Toast.makeText(context,"No songs to display!",Toast.LENGTH_LONG).show();
        }

        for (dataItem d: MusicLibrary.getInstance().getDataItemsForTracks()){
            if(data.contains(d.title))
                dataItems.add(d);
        }

        //add extra empty element
        dataItems.add(new dataItem(0,"",0,"",0,"","","",""));
        bindService();
        this.status=status;
        InitializeDeleteReceiver();
    }

    private void InitializeDeleteReceiver(){
        mDeleteReceiver= new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getIntExtra("status",-1)==Constants.FRAGMENT_STATUS.SECONDARY_LIB_FRAG) {
                    if (intent.getIntExtra("error", -1) == Constants.ERROR_CODE.SUCCESS) {
                        Toast.makeText(context, "Deleted " + dataItems.get(position).title, Toast.LENGTH_SHORT).show();
                        dataItems.remove(dataItems.get(position));
                        notifyItemRemoved(position);
                        notifyDataSetChanged();
                    } else {
                        Toast.makeText(context, "Cannot delete " + dataItems.get(position).title, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };
        LocalBroadcastManager.getInstance(context).registerReceiver(mDeleteReceiver,
                new IntentFilter(Constants.ACTION.DELETE_RESULT));
    }

    public void shuffleAll() {
        if (dataItems != null) {
            //remove empty element from list of headers
            ArrayList<String> temp = new ArrayList<>();
            for(dataItem d:dataItems){
                if(!d.title.equals("")) {
                    temp.add(d.title);
                }
            }
            ArrayList<String> tempList = new ArrayList<>();
            tempList.addAll(temp);
            Collections.shuffle(tempList);
            playerService.setTrackList(tempList);
            playerService.playAtPosition(0);
            /*
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent()
                    .setAction(Constants.ACTION.PLAY_AT_POSITION)
                    .putExtra("position", 0));*/
        }
    }

    public void bindService(){
        /*
        if(!mBound){
            //bind music service
            Intent playerServiceIntent = new Intent(context, PlayerService.class);
            context.bindService(playerServiceIntent, playerServiceConnection, 0);
        }*/
        playerService = MyApp.getService();
    }

    public void clear(){
        LocalBroadcastManager.getInstance(context).unregisterReceiver(mDeleteReceiver);
    }

    public ArrayList<dataItem> getList(){return dataItems;}
    @Override
    public SecondaryLibraryAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.fragment_library_item, parent, false);
        //int color = ColorHelper.getColor(R.color.colorwhite) ;
        int color = ColorHelper.getBaseThemeTextColor();
        ((TextView)(view.findViewById(R.id.header))).setTextColor(color);
        ((TextView)(view.findViewById(R.id.secondaryHeader))).setTextColor(color);
        ((TextView)(view.findViewById(R.id.count))).setTextColor(color);
        ((ImageView)(view.findViewById(R.id.menuPopup))).setColorFilter(color);

        return new SecondaryLibraryAdapter.MyViewHolder (view);
    }

    @Override
    public void onBindViewHolder(SecondaryLibraryAdapter.MyViewHolder holder, int position) {

        if(dataItems.get(position).title.equals("")){
            holder.title.setText("");
            holder.secondary.setText("");
            holder.count.setText("");
            holder.popUp.setVisibility(View.INVISIBLE);
            return;
        }

        holder.title.setText(dataItems.get(position).title);
        holder.secondary.setText(dataItems.get(position).artist_name);
        holder.count.setText(dataItems.get(position).durStr);
    }

    @Override
    public int getItemCount() {
        return dataItems.size();
    }

    public void onClick(View view,int position) {
        this.position=position;
        if(dataItems.get(position).title.equals("")){
            return;
        }
        switch (view.getId()) {
            case R.id.trackItem:
                if(MyApp.isLocked()){
                    Toast.makeText(context,"Music is Locked!",Toast.LENGTH_SHORT).show();
                    return ;
                }
                Play();
            break;

            case R.id.menuPopup:
                clikedON = ((TextView) ((ViewGroup) (view.getParent()).getParent())
                        .findViewById(R.id.header))
                        .getText()
                        .toString();
                PopupMenu popup = new PopupMenu(context, view);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_tracks_by_title, popup.getMenu());
                if(status==Constants.FRAGMENT_STATUS.PLAYLIST_FRAGMENT){
                    //popup.getMenu().removeItem(R.id.action_delete);
                    if( !( playlist_name.replace(" ","_").equals(Constants.SYSTEM_PLAYLISTS.RECENTLY_ADDED)
                            || playlist_name.replace(" ","_").equals(Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED)
                            || playlist_name.replace(" ","_").equals(Constants.SYSTEM_PLAYLISTS.MOST_PLAYED))) {
                        popup.getMenu().add(REMOVE);
                    }
                }

                popup.show();
                popup.setOnMenuItemClickListener(this);
                break;

        }
    }

    public void updateItem(int position, String ... param){
        dataItems.get(position).title = param[0];
        dataItems.get(position).artist_name = param[0];
        dataItems.get(position).albumName = param[0];
        notifyItemChanged(position);
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
                song_titles=new String[]{dataItems.get(position).title};
                UtilityFun.AddToPlaylist(context, song_titles);
                break;

            case R.id.action_share:
                ArrayList<Uri> files = new ArrayList<>();
                try {
                    File fileToBeShared = new File(MusicLibrary.getInstance().getTrackItemFromTitle(clikedON).getFilePath());
                    files.add(Uri.fromFile(fileToBeShared));
                    UtilityFun.Share(context, files, clikedON);
                }catch (Exception e) {
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

            case R.id.action_track_info:
                setTrackInfoDialog();
                break;

            case R.id.action_edit_track_info:
                TrackItem editItem = MusicLibrary.getInstance().getTrackItemFromTitle(dataItems.get(position).title);
                if(editItem==null){
                    Toast.makeText(context,"Error occured!",Toast.LENGTH_SHORT).show();
                    return true;
                }
                context.startActivity(new Intent(context, ActivityTagEditor.class)
                        .putExtra("from",Constants.TAG_EDITOR_LAUNCHED_FROM.SECONDARY_LIB)
                        .putExtra("file_path",editItem.getFilePath())
                        .putExtra("track_title",editItem.getTitle())
                        .putExtra("position",position));
                break;

            case R.id.action_set_as_ringtone:
                TrackItem tempItem = MusicLibrary.getInstance().getTrackItemFromTitle(dataItems.get(position).title);
                if(tempItem==null){
                    Toast.makeText(context,"Something wrong!",Toast.LENGTH_LONG).show();
                    return false;
                }
                UtilityFun.SetRingtone(context, tempItem.getFilePath()
                        ,tempItem.getTitle());
                break;
        }
        if (item.getTitle().equals(REMOVE)) {
            PlaylistManager.getInstance(context).RemoveSongFromPlaylist(playlist_name,dataItems.get(position).title);
            dataItems.remove(position);
            notifyItemRemoved(position);
        }
        return true;
    }

    private void setTrackInfoDialog(){
        final AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle("Track Info");
        LinearLayout linear = new LinearLayout(context);
        linear.setOrientation(LinearLayout.VERTICAL);
        final TextView text = new TextView(context);
        text.setText(UtilityFun.trackInfoBuild(dataItems.get(position).title).toString());

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
        ArrayList<String> temp = new ArrayList<>();
        for(dataItem d: dataItems){
            if(!d.title.equals("")) {
                temp.add(d.title);
            }
        }
        playerService.setTrackList(temp);
        playerService.playAtPosition(position);
    }

    private void AddToQ(int positionToAdd){
        //we are using same function for adding to q and playing next
        // toastString is to identify which string to disokay as toast
        String toastString=(positionToAdd==Constants.ADD_TO_Q.AT_LAST ? "added to  queue " : "playing next ") ;
        playerService.addToQ(clikedON, positionToAdd);
        Toast.makeText(context
                ,toastString+clikedON
                ,Toast.LENGTH_SHORT).show();
    }

    private void DeleteDialog(){

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        if(playerService.getCurrentTrack().getTitle().equals(dataItems.get(position).title)){
                            Toast.makeText(context,"Cannot delete currently playing song",Toast.LENGTH_SHORT).show();
                            return;
                        }

                        File file;
                        try {
                            file = new File(MusicLibrary.getInstance().getTrackItemFromTitle(dataItems.get(position).title)
                                    .getFilePath());
                        }catch (Exception e){
                            return;
                        }
                        //delete the file first
                        ArrayList<File> files = new ArrayList<>();
                        files.add(file);
                        ArrayList<Integer> ids = new ArrayList<>();
                        ids.add(MusicLibrary.getInstance().getIdFromTitle(dataItems.get(position).title));
                        ArrayList<String> song_titles = new ArrayList<String>();
                        song_titles.add(dataItems.get(position).title);
                        String[] titles = song_titles.toArray(new String[song_titles.size()]);
                        UtilityFun.Delete(context,ids,files,titles,Constants.FRAGMENT_STATUS.SECONDARY_LIB_FRAG);
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
        TextView title,secondary,count;
        ImageButton popUp;

        public MyViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.header);
            title.setTypeface(FontFactory.getFont());

            secondary = (TextView) itemView.findViewById(R.id.secondaryHeader);
            secondary.setTypeface(FontFactory.getFont());

            count = (TextView) itemView.findViewById(R.id.count);
            count.setTypeface(FontFactory.getFont());

            popUp = (ImageButton) itemView.findViewById(R.id.menuPopup);
            itemView.setOnClickListener(this);
            itemView.findViewById(R.id.menuPopup).setOnClickListener(this);
            itemView.findViewById(R.id.cardViewForAlbumGragment).setVisibility(View.GONE);
        }

        @Override
        public void onClick(View view) {
            SecondaryLibraryAdapter.this.onClick(view,getLayoutPosition());
        }
    }
}
