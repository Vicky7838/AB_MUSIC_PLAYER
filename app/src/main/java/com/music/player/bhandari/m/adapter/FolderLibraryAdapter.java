package com.music.player.bhandari.m.adapter;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.UIElemetHelper.ColorHelper;
import com.music.player.bhandari.m.UIElemetHelper.FontFactory;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.utils.MusicLibrary;
import com.music.player.bhandari.m.service.PlayerService;
import com.music.player.bhandari.m.utils.MyApp;
import com.music.player.bhandari.m.utils.UtilityFun;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;

/**
 * Created by amit on 7/12/16.
 */

public class FolderLibraryAdapter extends RecyclerView.Adapter<FolderLibraryAdapter.MyViewHolder>
        implements PopupMenu.OnMenuItemClickListener{

    private static final File STARTING_PATH1=new File("/storage/");

    private LinkedHashMap<String,File> files =new LinkedHashMap<>();   //for getting file from inflated list string value
    private ArrayList<String> headers=new ArrayList<>();   //for inflating list

    private static boolean isHomeFolder =true;
    private Context context;
    private LayoutInflater inflater;
    private View stub;
    private int clickedItemPosition;
    private File clickedFile;

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

    public FolderLibraryAdapter(Context context){
        //create first page for folder fragment
        this.context=context;
        inflater=LayoutInflater.from(context);
        initializeFirstPage();
        /*
        Intent playerServiceIntent = new Intent(context, PlayerService.class);
        try {
            context.bindService(playerServiceIntent, playerServiceConnection, 0);
        }catch (Exception ignored){

        }*/
        playerService = MyApp.getService();
    }

    public void clear(){
        if(mBound){
            context.unbindService(playerServiceConnection);mBound=false;
        }
        headers.clear();
        inflater=null;
        files.clear();
    }

    private void initializeFirstPage(){
        headers.clear();
        files.clear();
        //list all the folders having songs
        for(String path:MusicLibrary.getInstance().getFoldersList()){
            if(path.equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                continue;
            }
            File file = new File(path);
            if(file.canRead()){
                files.put(file.getName(), file);
            }
        }
        headers.addAll(files.keySet());
        // add songs which are in sdcard
        File sd = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        for (File f : sd.listFiles()){
            if(!f.isDirectory() && f.getName().endsWith("mp3")){
                files.put(f.getName(), f);
                headers.add(f.getName());
            }
        }
        notifyDataSetChanged();
        isHomeFolder =true;
    }



    @Override
    public FolderLibraryAdapter.MyViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.fragment_library_item, parent, false);
        stub=((ViewStub)view.findViewById(R.id.stub_in_fragment_library_item)).inflate();
        final FolderLibraryAdapter.MyViewHolder holder=new FolderLibraryAdapter.MyViewHolder (view);
        int color = ColorHelper.getBaseThemeTextColor() ;
        ((TextView)(view.findViewById(R.id.header))).setTextColor(color);
        ((TextView)(view.findViewById(R.id.secondaryHeader))).setTextColor(color);
        ((TextView)(view.findViewById(R.id.count))).setTextColor(color);
        ((ImageView)(view.findViewById(R.id.menuPopup))).setColorFilter(color);

        return holder;
    }

    private void refreshList(File fNavigate){
        files.clear();
        headers.clear();
        //previousPath=fNavigate;
        if(fNavigate.canRead()) {
            for (File f : fNavigate.listFiles()) {

                if(f.isFile() && f.getName().endsWith("mp3")) {
                    files.put(f.getName(), f);
                }
            }

            headers.addAll(files.keySet());
        }
        notifyDataSetChanged();
    }

    public void onStepBack(){

        if(isHomeFolder){
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return;
        }

        initializeFirstPage();
    }

    @Override
    public void onBindViewHolder(FolderLibraryAdapter.MyViewHolder holder, int position) {

        holder.title.setText(headers.get(position));
        if(files.get(headers.get(position)).isDirectory()){
            Drawable mDrawable = context.getResources().getDrawable(R.drawable.ic_folder_special_black_24dp);
              mDrawable.setColorFilter(new
                    PorterDuffColorFilter(ColorHelper.getDarkPrimaryColor(), PorterDuff.Mode.SRC_IN));
            holder.image.setImageDrawable(mDrawable);
        }else{
            Drawable mDrawable = context.getResources().getDrawable(R.drawable.ic_audiotrack_black_24dp);
            /*mDrawable.setColorFilter(new
                    PorterDuffColorFilter(Color.parseColor(Constants.COLOR_ARRAY.colorArray[new Random()
                    .nextInt(Constants.COLOR_ARRAY.COLOR_ARR_SIZE)]), PorterDuff.Mode.SRC_IN));*/
            mDrawable.setColorFilter(new
                    PorterDuffColorFilter(ColorHelper.getDarkPrimaryColor(), PorterDuff.Mode.SRC_IN));
            holder.image.setImageDrawable(mDrawable);
        }
    }


    @Override
    public int getItemCount() {
        return files.keySet().size();
    }

    public void onClick(View view, int position){
        clickedItemPosition = position;
        clickedFile = files.get(headers.get(clickedItemPosition));
        switch (view.getId()){
            case R.id.trackItem:
                File clickedFile=files.get(headers.get(position));
                if(clickedFile.isDirectory()) {
                    //update list here
                    refreshList(clickedFile);
                    isHomeFolder =false;
                }
                else{
                    if(MyApp.isLocked()){
                        Toast.makeText(context,"Music is Locked!",Toast.LENGTH_SHORT).show();
                        return ;
                    }
                    Play();
                }
                break;

            case R.id.menuPopup:
                PopupMenu popup = new PopupMenu(context, view);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_tracks_by_title, popup.getMenu());
                popup.getMenu().removeItem(R.id.action_delete);
                popup.getMenu().removeItem(R.id.action_edit_track_info);
                if(files.get(headers.get(position)).isDirectory())
                    popup.getMenu().removeItem(R.id.action_set_as_ringtone);
                popup.show();
                popup.setOnMenuItemClickListener(this);
                break;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if(clickedFile==null){
            return false;
        }
        switch (item.getItemId()){
            case R.id.action_play:
                if(MyApp.isLocked()){
                    Toast.makeText(context,"Music is Locked!",Toast.LENGTH_SHORT).show();
                    return true;
                }
                Play();
                break;

            case R.id.action_add_to_playlist:
                AddToPlaylist();
                break;

            case R.id.action_share:
                Share();
                break;

            case R.id.action_play_next:
                AddToQ(Constants.ADD_TO_Q.IMMEDIATE_NEXT);
                break;

            case R.id.action_add_to_q:
                AddToQ(Constants.ADD_TO_Q.AT_LAST);
                break;


            case R.id.action_set_as_ringtone:
                String abPath = files.get(headers.get(clickedItemPosition)).getAbsolutePath();
                UtilityFun.SetRingtone(context, abPath
                        ,MusicLibrary.getInstance().getTitleFromFilePath(abPath));
                break;

            case R.id.action_track_info:
                setTrackInfoDialog();
                break;

        }
        return true;
    }
    private void setTrackInfoDialog(){

        final AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle("Track Info");
        LinearLayout linear = new LinearLayout(context);
        linear.setOrientation(LinearLayout.VERTICAL);
        final TextView text = new TextView(context);

        if(clickedFile.isFile()){
            String title=MusicLibrary.getInstance().getTitleFromFilePath(clickedFile.getAbsolutePath());
            if(title!=null) {
                text.setText(UtilityFun.trackInfoBuild(title).toString());
            }else {
                text.setText("No info available at the moment!");
            }
        }else {
            String info = "File path : " + clickedFile.getAbsolutePath();
            text.setText(info);
        }

        text.setPadding(20, 20,20,10);
        text.setTextSize(15);
        //text.setGravity(Gravity.CENTER);

        linear.addView(text);
        alert.setView(linear);
        alert.setPositiveButton("Ok", null);
        alert.show();
    }
    private void Play(){
        if(clickedFile.isFile()) {
            String title=MusicLibrary.getInstance().getTitleFromFilePath(clickedFile.getAbsolutePath());
            if(title==null){
                return;
            }
            ArrayList<String> track=new ArrayList<>();
            track.add(title);
            playerService.setTrackList(track);
            playerService.playAtPosition(0);
            /*
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent()
                    .setAction(Constants.ACTION.PLAY_AT_POSITION)
                    .putExtra("position",0));*/
        }
        else {
            File[] fileList = clickedFile.listFiles();
            ArrayList<String> songTitles = new ArrayList<>();
            for(File f:fileList){
                if(f.getAbsolutePath().endsWith("mp3")) {
                    String title = MusicLibrary.getInstance().getTitleFromFilePath(f.getAbsolutePath());
                    if(title!=null) {
                        songTitles.add(title);
                    }
                }
            }
            if(songTitles.isEmpty()){
                Toast.makeText(context,"Nothing to play!",Toast.LENGTH_LONG).show();
                return;
            }
            playerService.setTrackList(songTitles);
            playerService.playAtPosition(0);
            /*
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent()
                    .setAction(Constants.ACTION.PLAY_AT_POSITION)
                    .putExtra("position",0));*/
        }
    }

    private void AddToPlaylist(){
        String[] song_titles;
        ArrayList<String> temp = new ArrayList<>();
        if(clickedFile.isFile()) {
            String title=MusicLibrary.getInstance().getTitleFromFilePath(clickedFile.getAbsolutePath());
            if(title==null){
                return;
            }
            song_titles=new String[]{title};
            UtilityFun.AddToPlaylist(context, song_titles);
        }else {
            File[] fileList = clickedFile.listFiles();
            for(File f:fileList){
                if(f.getAbsolutePath().endsWith("mp3")) {
                    String title = MusicLibrary.getInstance().getTitleFromFilePath(f.getAbsolutePath());
                    if(title!=null) {
                        temp.add(title);
                    }
                }
            }
            if(temp.isEmpty()){
                Toast.makeText(context,"Nothing to add!",Toast.LENGTH_LONG).show();
                return;
            }
            song_titles = temp.toArray(new String[temp.size()]);
            UtilityFun.AddToPlaylist(context, song_titles);
        }
    }

    private void Share(){
        ArrayList<Uri> files = new ArrayList<Uri>();  //for sending multiple files
        if(clickedFile.isFile()){
            files.add(Uri.fromFile(clickedFile));
        }else {
            File[] fileList = clickedFile.listFiles();
            for(File f:fileList){
                if(f.getAbsolutePath().endsWith("mp3")) {
                    files.add(Uri.fromFile(f));
                }
            }
        }
        UtilityFun.Share(context, files, "music");
    }

    private void AddToQ(int positionToAdd){
        //we are using same function for adding to q and playing next
        // toastString is to identify which string to disokay as toast
        String toastString=(positionToAdd==Constants.ADD_TO_Q.AT_LAST ? "added to  queue " : "playing next ") ;
        if(clickedFile.isFile()) {
            String title=MusicLibrary.getInstance().getTitleFromFilePath(clickedFile.getAbsolutePath());
            if(title==null){
                return;
            }
            playerService.addToQ(title, positionToAdd);
            Toast.makeText(context
                    ,toastString+title
                    ,Toast.LENGTH_SHORT).show();
        }else {
            File[] fileList = clickedFile.listFiles();
            for(File f:fileList){
                if(f.getAbsolutePath().endsWith("mp3")) {
                    String title = MusicLibrary.getInstance().getTitleFromFilePath(f.getAbsolutePath());
                    if(title!=null) {
                        playerService.addToQ(title, positionToAdd);
                    }
                }
            }
            Toast.makeText(context
                    ,toastString+clickedFile.getName()
                    ,Toast.LENGTH_SHORT).show();
        }


    }


    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView title;
        ImageView image;


        MyViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.header);
            title.setTypeface(FontFactory.getFont());

            image= (ImageView) stub.findViewById(R.id.imageViewForStub);
            itemView.setOnClickListener(this);
            itemView.findViewById(R.id.cardViewForAlbumGragment).setVisibility(View.GONE);
            itemView.findViewById(R.id.menuPopup).setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            FolderLibraryAdapter.this.onClick(v,getLayoutPosition());
        }
    }
}
