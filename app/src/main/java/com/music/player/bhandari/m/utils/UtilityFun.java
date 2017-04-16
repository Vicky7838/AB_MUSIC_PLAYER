package com.music.player.bhandari.m.utils;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.model.TrackItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.Executors;

/**
 * Created by amit on 8/1/17.
 */

public class UtilityFun {

    public static String secondsToString(int pTime) {
        return String.format("%02d:%02d", pTime / 60, pTime % 60);
    }

    public static int getProgressPercentage(int currentDuration, int totalDuration){
        Double percentage = (double) 0;

        long currentSeconds = (int) (currentDuration);
        long totalSeconds = (int) (totalDuration);

        // calculating percentage
        percentage =(((double)currentSeconds)/totalSeconds)*100;

        // return percentage
        return percentage.intValue();
    }

    public static int progressToTimer(int progress, int totalDuration) {
        int currentDuration = 0;
        totalDuration = (int) (totalDuration / 1000);
        currentDuration = (int) ((((double)progress) / 100) * totalDuration);

        // return current duration in milliseconds
        return currentDuration * 1000;
    }

    public static String msToString(long pTime) {
        return String.format("%02d:%02d", (pTime/1000) / 60, (pTime/1000) % 60);
    }

    public static int GetDominantColor(Bitmap bitmap){
        int redBucket = 0;
        int greenBucket = 0;
        int blueBucket = 0;
        int alphaBucket = 0;
        bitmap = Bitmap.createScaledBitmap(bitmap,100,100,false);
        boolean hasAlpha = bitmap.hasAlpha();
        int pixelCount = bitmap.getWidth() * bitmap.getHeight();
        int[] pixels = new int[pixelCount];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int y = 0, h = bitmap.getHeight(); y < h; y++)
        {
            for (int x = 0, w = bitmap.getWidth(); x < w; x++)
            {
                int color = pixels[x + y * w]; // x + y * width
                redBucket += (color >> 16) & 0xFF; // Color.red
                greenBucket += (color >> 8) & 0xFF; // Color.greed
                blueBucket += (color & 0xFF); // Color.blue
                if (hasAlpha) alphaBucket += (color >>> 24); // Color.alpha
            }
        }

        int color = Color.argb(
                (hasAlpha) ? (alphaBucket / pixelCount) : 255,
                redBucket/ pixelCount,
                greenBucket / pixelCount,
                blueBucket / pixelCount);
        if(UtilityFun.isWhiteColor(color)){
            color = UtilityFun.getDarkColor(color);
        }
        if(!UtilityFun.isColorDark(color)){
            color = UtilityFun.getDarkColor(color);
        }
        if(UtilityFun.isColorDark(color)){
            color = UtilityFun.getBrightColor(color);
        }


        return color;
    }

    public static boolean isWhiteColor(int color) {
        if (android.R.color.transparent == color)
            return true;
        boolean rtnValue = false;
        int[] rgb = { Color.red(color), Color.green(color), Color.blue(color) };
        int Y = (int)(0.2126*rgb[0] + 0.7152*rgb[1] + 0.0722*rgb[2]);
        if (Y >= 200) {
            rtnValue = true;
        }
        return rtnValue;
    }

    public static boolean isColorDark(int color){
        double darkness = 1-(0.299*Color.red(color) + 0.587*Color.green(color) + 0.114*Color.blue(color))/255;
        if(darkness<0.5){
            return false; // It's a light color
        }else{
            return true; // It's a dark color
        }
    }

    public static int getDarkColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f; // value component
        return Color.HSVToColor(hsv);
    }

    public static int getBrightColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = 1.0f - 0.8f * (1.0f - hsv[2]);; // value component
        return Color.HSVToColor(hsv);
    }

    static String escapeDoubleQuotes(String title){
        //escape all the quotes
        ArrayList<Integer> indexList = new ArrayList<>();
        StringBuffer stringBuffer = new StringBuffer(title);
        int index = stringBuffer.indexOf("\"");
        while(index >= 0) {
            indexList.add(index);
            index = stringBuffer.indexOf("\"", index+1);
        }
        int i=0;
        for(int tempIndex:indexList){
            stringBuffer.insert(tempIndex+i,"\\");
            i++;
        }
        return stringBuffer.toString();
    }

    public static boolean DeleteFiles(ArrayList<File> files){
        boolean result=false;
        for(File f:files){
            if(f.delete()){
                result = true;
            }else {
                result = false;
                break;
            }
        }
        return  result;
    }

    public static boolean DeleteFromContentProvider(ArrayList<Integer> ids, Context context){
        boolean result =false;
        for(int id:ids){ // NOTE: You would normally obtain this from the content provider!
            Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            Uri itemUri = ContentUris.withAppendedId(contentUri, id);
            int rows = context.getContentResolver().delete(itemUri, null, null);
            String path = itemUri.getEncodedPath();
            if(rows == 0)
            {
                Log.e("Example Code:","Could not delete "+path+" :(");
                result = false;
                break;
            }
            else {
                Log.d("Example Code:", "Deleted " + path + " ^_^");
                result = true;
            }
        }
        return  result;
    }

    public static void AddToPlaylist(final Context context, final String[] song_titles){
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);
        builderSingle.setTitle("Select Playlist");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.select_dialog_item);
        arrayAdapter.addAll(PlaylistManager.getInstance(context).GetPlaylistList(true));

        builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String playlist_name = arrayAdapter.getItem(which);
                PlaylistManager.getInstance(context).AddSongToPlaylist(playlist_name,song_titles);
            }
        });
        builderSingle.show();
    }

    public static void Share(Context context, ArrayList<Uri> files, String title){
        if(files.size()==1) {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("audio/*");
            share.putExtra(Intent.EXTRA_STREAM, files.get(0));
            context.startActivity(Intent.createChooser(share, title));
        }else {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("*/*");
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
            context.startActivity(Intent.createChooser(intent, title));
        }
    }

    public static void Delete (Context context,ArrayList<Integer> ids, ArrayList<File> files, String[] titles, int status ){
        if(DeleteFiles(files)){
            if(DeleteFromContentProvider(ids,context)){
                PlaylistManager.getInstance(context).RemoveEntryFromUserMusicDb(titles);
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent()
                        .setAction(Constants.ACTION.DELETE_RESULT)
                        .putExtra("error",Constants.ERROR_CODE.SUCCESS)
                        .putExtra("status",status));
            }else {
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent()
                        .setAction(Constants.ACTION.DELETE_RESULT)
                        .putExtra("error",Constants.ERROR_CODE.FAIL)
                        .putExtra("status",status));
            }
        }else {
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent()
                    .setAction(Constants.ACTION.DELETE_RESULT)
                    .putExtra("error",Constants.ERROR_CODE.FAIL)
                    .putExtra("status",status));
            Log.v(Constants.TAG,"Status"+status);
        }
    }

    public static void SetRingtone(final Context context, final String filePath , final String songTitle){
        if(!checkSystemWritePermission(context)){
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            openAndroidPermissionsMenu(context);
                            //Yes button clicked
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("Please provide write setting permission for application to change Ringtone")
                    .setPositiveButton("Sure", dialogClickListener)
                    .setNegativeButton("Not now", dialogClickListener).show();

        }else {

            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    File k = new File(filePath);

                    File newFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES)
                            .getAbsolutePath()
                            + "/AB_Music_tone.mp3");
                    try
                    {
                        newFile.createNewFile();
                        copy(k,newFile);
                    }
                    catch (IOException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }


                    if(!k.canRead()){
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Unable to set ringtone: " + songTitle, Toast.LENGTH_SHORT).show();
                            }
                        });

                        return;
                    }
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DATA, newFile.getAbsolutePath());
                    values.put(MediaStore.MediaColumns.TITLE, songTitle+" Tone");
                    values.put(MediaStore.MediaColumns.SIZE, k.length());
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3");
                    values.put(MediaStore.Audio.Media.DURATION, 230);
                    values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
                    values.put(MediaStore.Audio.Media.IS_NOTIFICATION, true);
                    values.put(MediaStore.Audio.Media.IS_ALARM, true);
                    values.put(MediaStore.Audio.Media.IS_MUSIC, false);

//Insert it into the database
                    Uri uri1 = MediaStore.Audio.Media.getContentUriForPath(newFile.getAbsolutePath());
                    context.getContentResolver().delete(uri1, MediaStore.MediaColumns.DATA + "=\"" + newFile.getAbsolutePath() + "\"",
                            null);
                    Uri newUri = context.getContentResolver().insert(uri1, values);

                    RingtoneManager.setActualDefaultRingtoneUri(
                            context,
                            RingtoneManager.TYPE_RINGTONE,
                            newUri
                    );

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Ringtone set: " + songTitle, Toast.LENGTH_SHORT).show();
                        }
                    });

                }
            });



        }
    }



    private static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    private static boolean checkSystemWritePermission(Context context) {
        boolean retVal = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            retVal = Settings.System.canWrite(context);
            Log.d(Constants.TAG, "Can Write Settings: " + retVal);
        }
        return retVal;
    }

    private static void openAndroidPermissionsMenu(Context context) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        context.startActivity(intent);
    }

    public static boolean isConnectedToInternet(){
        ConnectivityManager
                cm = (ConnectivityManager) MyApp.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null
                && activeNetwork.isConnectedOrConnecting()) {
            return true;
        }else {
            //Toast.makeText(MyApp.getContext(), "No internet connection!", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    public static StringBuilder trackInfoBuild(String title){
        StringBuilder trackInfo = new StringBuilder();
        TrackItem item = MusicLibrary.getInstance().getTrackItemFromTitle(title);
        if(item==null){
            return trackInfo;
        }

        trackInfo.append("Title : ")
                .append(item.getTitle())
                .append("\n\n")
                .append("Artist : ")
                .append(item.getArtist())
                .append("\n\n").append("Album : ")
                .append(item.getAlbum()).append("\n\n")
                .append("Duration : ")
                .append(item.getDurStr()).append("\n\n")
                .append("File Path : ")
                .append(item.getFilePath()).append("\n\n")
                .append("File Size : ")
                .append(android.text.format.Formatter.formatFileSize(MyApp.getContext(), new File(item.getFilePath()).length()));
                //.append(new File(item.getFilePath()).length()/(1024*1024)).append(" MB");

        return trackInfo;
    }

    public static Bitmap decodeUri(Context c, Uri uri, final int requiredSize)
            throws FileNotFoundException {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(c.getContentResolver().openInputStream(uri), null, o);

        int width_tmp = o.outWidth
                , height_tmp = o.outHeight;
        int scale = 1;

        while(true) {
            if(width_tmp / 2 < requiredSize || height_tmp / 2 < requiredSize)
                break;
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(c.getContentResolver().openInputStream(uri), null, o2);
    }
}
