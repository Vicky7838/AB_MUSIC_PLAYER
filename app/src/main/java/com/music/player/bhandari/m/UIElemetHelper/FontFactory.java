package com.music.player.bhandari.m.UIElemetHelper;

import android.graphics.Typeface;
import android.widget.Switch;

import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.utils.MyApp;

/**
 * Created by Amit Bhandari on 3/18/2017.
 */

public class FontFactory {
    public static Typeface getFont(){
        Typeface t=Typeface.MONOSPACE;
        switch (MyApp.getPref().getInt(MyApp.getContext().getString(R.string.pref_text_font), Constants.FONT.MONOSPACE)){
            case Constants.FONT.NORMAL:
                t=Typeface.DEFAULT;
                break;

            case Constants.FONT.SANS:
                t=Typeface.SANS_SERIF;
                break;

            case Constants.FONT.SERIF:
                t= Typeface.SERIF;
                break;
        }
        return t;
    }


}
