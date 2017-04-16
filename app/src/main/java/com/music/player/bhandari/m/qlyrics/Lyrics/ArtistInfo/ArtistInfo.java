package com.music.player.bhandari.m.qlyrics.Lyrics.ArtistInfo;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by Amit Bhandari on 3/27/2017.
 */

public class ArtistInfo implements Serializable, Parcelable{

    private String originalArtist;
    private String mArtist;
    private String artistContent;
    private String imageUrl;
    private String artistUrl;
    private int flag = NEGATIVE;
    public static int POSITIVE = 0;
    public static int NEGATIVE = 1;


    public static final Creator<ArtistInfo> CREATOR = new Creator<ArtistInfo>()     {
        @Override
        public ArtistInfo createFromParcel(Parcel in) {
            return new ArtistInfo(in);
        }

        @Override
        public ArtistInfo[] newArray(int size) {
            return new ArtistInfo[size];
        }
    };

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    protected ArtistInfo(Parcel in) {
        originalArtist = in.readString();
        mArtist = in.readString();
        artistContent = in.readString();
        imageUrl = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(originalArtist);
        dest.writeString(mArtist);
        dest.writeString(artistContent);
        dest.writeString(imageUrl);
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public String getArtistUrl() {
        return artistUrl;
    }

    public void setArtistUrl(String artistUrl) {
        this.artistUrl = artistUrl;
    }

    public interface Callback {
        void onArtInfoDownloaded(ArtistInfo artistInfo);
    }

    public ArtistInfo(String artist){
        originalArtist = artist;
    }

    public void setOriginalArtist(String artist){
        originalArtist=artist;
    }

    public String getOriginalArtist(){
        return originalArtist;
    }

    public void setCorrectedArtist(String artist){
        this.mArtist=artist;
    }

    public String getCorrectedArtist(){
        return mArtist;
    }

    public void setArtistContent(String artistContent){
        this.artistContent = artistContent;
    }

    public String getArtistContent(){
        return artistContent;
    }
}
