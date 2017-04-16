package com.music.player.bhandari.m.UIElemetHelper;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;


import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.utils.MyApp;

import java.util.Random;

/**
 * Created by Amit Bhandari on 2/8/2017.
 */

public class ColorHelper {

    public static int getAccentColor(){
        if(MyApp.getPref().getInt(MyApp.getContext().getResources()
                .getString(R.string.pref_theme_color),Constants.THEME.BLACK) == Constants.THEME.BLACK) {
            return Constants.THEME.PINK_CERISE;
        }else {
            return getBrightPrimaryColor();
        }
    }

    public static int getPrimaryColor(){
        return MyApp.getPref().getInt(MyApp.getContext().getResources()
                .getString(R.string.pref_theme_color),Constants.THEME.DARK_SLATE_GRAY);
    }

    public static int getDarkPrimaryColor(){
        int color = getPrimaryColor();
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f; // value component
        return  Color.HSVToColor(hsv);
    }

    public static int getBrightPrimaryColor(){
        int color = getPrimaryColor();
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 1.4f; // value component
        return  Color.HSVToColor(hsv);
    }

    public static Drawable getColoredThemeDrawable(){
        //int color = getLightThemebackgroundColor();
        return new GradientDrawable(
                GradientDrawable.Orientation.BR_TL,
                new int[] {getDarkPrimaryColor(),0xFF131313});
    }

    public static Drawable getBaseThemeDrawable(){
        Drawable d = new ColorDrawable(MyApp.getContext().getResources().getColor(R.color.light_gray2));
        int pref = MyApp.getPref().getInt(MyApp.getContext().getResources()
                .getString(R.string.pref_theme),Constants.THEME.LIGHT);
        switch (pref){
            case Constants.THEME.LIGHT:
                break;
            case Constants.THEME.DARK:
                d = new ColorDrawable(MyApp.getContext().getResources().getColor(R.color.dark_gray2));
                break;

            case Constants.THEME.GLOSSY:
                d = new GradientDrawable(
                        GradientDrawable.Orientation.BR_TL,
                        new int[] {getDarkPrimaryColor(),0xFF131313});
                break;
        }
        return d;
    }

    public static int getBaseThemeTextColor(){
        int color = MyApp.getContext().getResources().getColor(R.color.light_text);
        int pref = MyApp.getPref().getInt(MyApp.getContext().getResources()
                .getString(R.string.pref_theme),Constants.THEME.LIGHT);
        switch (pref){
            case Constants.THEME.LIGHT:
                break;

            case Constants.THEME.DARK:
                color=MyApp.getContext().getResources().getColor(R.color.dark_text);
                break;

            case Constants.THEME.GLOSSY:
                color=MyApp.getContext().getResources().getColor(R.color.dark_text);
                break;

        }
        return color;
    }

    public static int getColor(int id){
        return MyApp.getContext().getResources().getColor(id);
    }

    public static int getNowPlayingControlsColor(){
        return Color.YELLOW;
    }

}
