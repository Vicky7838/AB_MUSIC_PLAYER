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
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.UIElemetHelper.FontFactory;
import com.music.player.bhandari.m.activity.ActivityTagEditor;
import com.music.player.bhandari.m.activity.MainActivity;
import com.music.player.bhandari.m.activity.NowPlayingActivity;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.model.dataItem;
import com.music.player.bhandari.m.utils.MusicLibrary;
import com.music.player.bhandari.m.model.TrackItem;
import com.music.player.bhandari.m.utils.recyclerviewHelper.ItemTouchHelperAdapter;
import com.music.player.bhandari.m.utils.recyclerviewHelper.OnStartDragListener;
import com.music.player.bhandari.m.service.PlayerService;
import com.music.player.bhandari.m.utils.MyApp;
import com.music.player.bhandari.m.utils.UtilityFun;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executors;

/**
 * Created by amit on 21/12/16.
 */

public class CurrentTracklistAdapter extends RecyclerView.Adapter<CurrentTracklistAdapter.MyViewHolder>
        implements ItemTouchHelperAdapter, PopupMenu.OnMenuItemClickListener{

    private static ArrayList<dataItem> dataItems = new ArrayList<>();
    private PlayerService playerService;
    private long mLastClickTime;
    private OnStartDragListener mDragStartListener;
    private Context context;
    private LayoutInflater inflater;
    //current playing position
    private int position=0;
    private BroadcastReceiver mDeleteReceiver;

    public CurrentTracklistAdapter(Context context, OnStartDragListener dragStartListener){
        mDragStartListener = dragStartListener;
        //bind music service
       /* Intent playerServiceIntent = new Intent(context, PlayerService.class);
        context.bindService(playerServiceIntent, playerServiceConnection, 0);*/
        playerService = MyApp.getService();
        final ArrayList<String> temp = playerService.getTrackList();

        long t = System.currentTimeMillis();

        dataItems.clear();
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                ArrayList<dataItem> data = MusicLibrary.getInstance().getDataItemsForTracks();
                for (String s:temp){
                    for (dataItem d:data){
                        if(d.title.equals(s)){
                            dataItems.add(d);
                            break;
                        }
                    }
                }
                try {
                    notifyDataSetChanged();
                }catch (Exception ignored){}
            }
        });

        Log.v(Constants.TAG,"AMIT "+(System.currentTimeMillis()-t));

        position = playerService.getCurrentTrackPosition();
        this.context=context;
        inflater=LayoutInflater.from(context);
        mDeleteReceiver= new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //as multiple objects in existence at time
                //it will be received by all the objects
                // we have to skip unwanted ones and proceed on one on which delete action was invoked
                    if (intent.getIntExtra("error", -1) == Constants.ERROR_CODE.SUCCESS) {
                        Toast.makeText(context, "Deleted " + dataItems.get(position).title, Toast.LENGTH_SHORT).show();
                        dataItems.remove(position);
                        notifyItemRemoved(position);
                       // notifyDataSetChanged();
                    } else {
                        Toast.makeText(context, "Cannot delete " + dataItems.get(position).title, Toast.LENGTH_SHORT).show();
                    }
            }
        };
        LocalBroadcastManager.getInstance(context).registerReceiver(mDeleteReceiver,
                new IntentFilter(Constants.ACTION.DELETE_RESULT));
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = inflater.inflate(R.layout.track_item_for_dragging, parent, false);
        final CurrentTracklistAdapter.MyViewHolder holder = new CurrentTracklistAdapter.MyViewHolder(view);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 300){
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                int i = 0;
                for (dataItem d:dataItems){
                    if(d.title.equals(((TextView) view.findViewById(R.id.header)).getText().toString())){
                        position = i;
                        break;
                    }
                    i++;
                }
                notifyItemChanged(position);
                if(position==playerService.getCurrentTrackPosition()){
                    playerService.play();
                    playerService.notifyUI();
                }else {
                    playerService.playAtPositionFromNowPlaying(position);
                    Log.v(Constants.TAG,position+"  position");
                    holder.cv.setBackgroundColor(context.getResources().getColor(R.color.colorBlack));
                }

                //notifyDataSetChanged();
            }
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(final CurrentTracklistAdapter.MyViewHolder holder, int position) {

        holder.title.setText(dataItems.get(position).title);
        holder.secondary.setText(dataItems.get(position).artist_name);

        holder.handle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (MotionEventCompat.getActionMasked(motionEvent) ==
                        MotionEvent.ACTION_DOWN) {
                    mDragStartListener.onStartDrag(holder);
                }
                return false;
            }
        });
        if(playerService!=null && position==playerService.getCurrentTrackPosition()) {
            holder.cv.setBackgroundColor(context.getResources().getColor(R.color.colorBlack));
            if (playerService.getStatus()==PlayerService.PLAYING){
                holder.iv.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_pause_black_24dp));
            }else {
                holder.iv.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_play_arrow_black_24dp));
            }
            holder.iv.setVisibility(View.VISIBLE);
        }else {
            holder.cv.setBackgroundColor(context.getResources().getColor(R.color.colorTransparent));
            holder.iv.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return dataItems.size();
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        //no need to update list of in player service.
        //listOfHeader is reference for that list itself
        //it will automatically reflect in current tracklist in player service class
        playerService.swapPosition(fromPosition,toPosition);
        Collections.swap(dataItems,fromPosition,toPosition);
        notifyItemMoved(fromPosition,toPosition);
        return true;
    }

    @Override
    public void onItemDismiss(int position) {
        if(playerService.getCurrentTrackPosition()!=position) {
            //listOfHeader.remove(position);
            playerService.removeTrack(position);
            dataItems.remove(position);
            notifyItemRemoved(position);
        }else {
            notifyItemChanged(position);
            //notifyDataSetChanged();
        }
    }

    public void clear(){

        LocalBroadcastManager.getInstance(context).unregisterReceiver(mDeleteReceiver);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_play:
                playerService.playAtPositionFromNowPlaying(position);
                /*
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent()
                        .setAction(Constants.ACTION.PLAY_AT_POSITION)
                        .putExtra("position",position));*/
                notifyItemChanged(position);
                break;

            case R.id.action_add_to_playlist:
                String[] song_titles = new String[]{dataItems.get(position).title};
                UtilityFun.AddToPlaylist(context,song_titles);
                break;

            case R.id.action_share:
                ArrayList<Uri> files = new ArrayList<>();  //for sending multiple files
                File file = new File(dataItems.get(position).file_path);
                Uri fileUri = Uri.fromFile(file);
                files.add(fileUri);
                UtilityFun.Share(context, files, dataItems.get(position).title);
                break;

            case R.id.action_delete:
                Delete();
                break;

            case R.id.action_track_info:
                setTrackInfoDialog();
                break;

            case R.id.action_edit_track_info:
                context.startActivity(new Intent(context, ActivityTagEditor.class)
                        .putExtra("from",Constants.TAG_EDITOR_LAUNCHED_FROM.NOW_PLAYING)
                        .putExtra("file_path",dataItems.get(position).file_path)
                        .putExtra("track_title",dataItems.get(position).title)
                        .putExtra("position",position));
                ((NowPlayingActivity)context).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
        }
        return true;
    }

    public void updateItem(int position, String... param){
        dataItems.get(position).title=param[0];
        dataItems.get(position).artist_name=param[1];
        dataItems.get(position).albumName=param[2];
        notifyItemChanged(position);
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

    private void Delete(){

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        ArrayList<Integer> ids = new ArrayList<>();
                        ArrayList<File> files = new ArrayList<>();
                        if(playerService.getCurrentTrack().getTitle().equals(dataItems.get(position).title)){
                            Toast.makeText(context,"Song is playing currently",Toast.LENGTH_SHORT).show();
                            return;
                        }

                        files.add(new File(dataItems.get(position).file_path));
                        ids.add(dataItems.get(position).id);
                        String[] titles = new String[]{dataItems.get(position).title};
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

    public void onClick(View view, int position) {
        this.position=position;
        switch (view.getId()){
            case R.id.more:
                PopupMenu popup = new PopupMenu(context, view);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_tracks_by_title, popup.getMenu());
                popup.getMenu().removeItem(R.id.action_set_as_ringtone);
                popup.getMenu().removeItem(R.id.action_add_to_q);
                popup.getMenu().removeItem(R.id.action_play_next);
                popup.show();
                popup.setOnMenuItemClickListener(this);
                break;
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView title,secondary;
        LinearLayout backGround;
        ImageView handle;
        CardView cv;
        ImageView iv;

        MyViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.header);
            title.setTypeface(FontFactory.getFont());

            secondary = (TextView) itemView.findViewById(R.id.secondaryHeader);
            secondary.setTypeface(FontFactory.getFont());

            handle = (ImageView) itemView.findViewById(R.id.handleForDrag);

            backGround = (LinearLayout)itemView.findViewById(R.id.trackItemDraggable);
            cv = (CardView) itemView.findViewById(R.id.card_view_track_item_drag);
            iv = (ImageView)itemView.findViewById(R.id.play_button_item_drag);
            itemView.findViewById(R.id.more).setOnClickListener(this);
        }

        @Override
        public void onClick(View v){
            CurrentTracklistAdapter.this.onClick(v,this.getLayoutPosition());
        }
    }
}
