package com.music.player.bhandari.m.customViews.nowplaying;

/**
 * Created by amit on 20/11/16.
 */

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.utils.DoubleClickListener;
import com.music.player.bhandari.m.utils.MyApp;


public class MusicPlayerView extends View implements OnPlayPauseToggleListener {

    /**
     * RectF for draw circle progress.
     */
    private RectF rectF;

    /**
     * Modified OnClickListener. We do not want all view click.
     * notify onClick() only button area touched.
     */
    private OnClickListener onClickListener;
    private DoubleClickListener onDoubleClickListener;
    /**
     * Button paint for play/pause control button
     */
    private static Paint mPaintButton;

    /**
     * Play/Pause button region for handle onTouch
     */
    private static Region mButtonRegion;

    /**
     * Paint to draw cover photo to canvas
     */
    private static Paint mPaintCover;

    /**
     * Bitmap for shader.
     */
    private static Bitmap mBitmapCover;

    /**
     * Shader for make drawable circle
     */
    private static BitmapShader mShader;

    /**
     * Image Height and Width values.
     */
    private int mHeight;
    private int mWidth;

    /**
     * Center values for cover image.
     */
    private float mCenterX;
    private float mCenterY;

    /**
     * Cover image is rotating. That is why we hold that value.
     */
    private int mRotateDegrees;

    /**
     * Handler for posting runnable object
     */
    private Handler mHandlerRotate;

    /**
     * Runnable for turning image (default velocity is 10)
     */
    private  final Runnable mRunnableRotate = new Runnable() {
        @Override
        public void run() {
            if (isRotating) {
                updateCoverRotate();
                mHandlerRotate.postDelayed(mRunnableRotate, ROTATE_DELAY);
            }
        }
    };

    /**
     * Handler for posting runnable object

    private Handler mHandlerProgress;

    /**
     * Runnable for turning image (default velocity is 10)

    private Runnable mRunnableProgress = new Runnable() {
        @Override
        public void run() {
            if (isRotating) {
                currentProgress++;
                mHandlerProgress.postDelayed(mRunnableProgress, PROGRESS_SECOND_MS);
            }
        }
    };

    /**
     * isRotating
     */
    private boolean isRotating;

    /**-
     * Handler will post runnable object every @ROTATE_DELAY seconds.
     */
    private static int ROTATE_DELAY = 10;


    /**
     * mRotateDegrees numberOfTracks increase 1 by 1 default.
     * I used that parameter as velocity.
     */
    private static float VELOCITY = 1;

    /**
     * Default color code for cover
     */
    private int mCoverColor = Color.GRAY;

    /**
     * Play/Pause button radius.(default = 120)
     */
    private float mButtonRadius = 120f;

    /**
     * Play/Pause button color(Default = dark gray)
     */
    private int mButtonColor = Color.DKGRAY;

    /**
     * Color code for progress left.
     */
    private int mProgressEmptyColor = 0x20FFFFFF;


    /**
     * play pause animation duration
     */
    private static final long PLAY_PAUSE_ANIMATION_DURATION = 200;

    /**
     * Play Pause drawable
     */
    private static PlayPauseDrawable mPlayPauseDrawable;

    /**
     * Animator set for play pause toggle
     */
    private static AnimatorSet mAnimatorSet;

    private boolean mFirstDraw = true;

    /**
     * Constructor
     */
    public MusicPlayerView(Context context) {
        super(context);
        init(context, null);
    }

    /**
     * Constructor
     */
    public MusicPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    /**
     * Constructor
     */
    public MusicPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    /**
     * Constructor
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MusicPlayerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    /**
     * Initializes resource values, create objects which we need them later.
     * Object creation must not called onDraw() method, otherwise it won't be
     * smooth.
     */
    private void init(Context context, AttributeSet attrs) {

        setWillNotDraw(false);
        mPlayPauseDrawable = new PlayPauseDrawable(context);
        mPlayPauseDrawable.setCallback(callback);
        mPlayPauseDrawable.setToggleListener(this);

        //Get Image resource from xml
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.playerview);
        Drawable mDrawableCover = a.getDrawable(R.styleable.playerview_cover);
        if (mDrawableCover != null) mBitmapCover = drawableToBitmap(mDrawableCover);

        //mButtonColor = a.getColor(R.styleable.playerview_buttonColor, mButtonColor);

        mButtonColor = getContext().getResources().getColor(R.color.blackTransparentLight);

        a.recycle();

        mRotateDegrees = 0;

        //Handler and Runnable object for turn cover image by updating rotation degrees
        mHandlerRotate = new Handler();

        //Handler and Runnable object for progressing.
        //mHandlerProgress = new Handler();

        //Play/Pause button circle paint
        mPaintButton = new Paint();
        mPaintButton.setAntiAlias(true);
        mPaintButton.setStyle(Paint.Style.FILL);
        mPaintButton.setColor(mButtonColor);

        //rectF and rect initializes
        rectF = new RectF();
    }

    /**
     * Calculate mWidth, mHeight, mCenterX, mCenterY values and
     * scale resource bitmap. Create shader. This is not called multiple times.
     */

    float radius;
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);

        mCenterX = mWidth / 2f;
        mCenterY = mHeight / 2f;

        float sizeOffset = MyApp.getPref().getFloat(getContext()
                .getString(R.string.pref_disc_size),Constants.DISC_SIZE.MEDIUM);

        //float sizeOffset = Constants.DISC_SIZE.MEDIUM;
        float px=Resources.getSystem().getDisplayMetrics().heightPixels/sizeOffset;

        Log.e(Constants.TAG,"Width: "+mWidth + " Height: "+mHeight);
        if(mWidth<mHeight){
            int offset = (mHeight-mWidth)/2;
            rectF.set(0, offset, mWidth, mWidth + offset);
            radius = px;
        }else if(mWidth>mHeight){
            int offset = (mWidth-mHeight)/2;
            rectF.set(offset, 0, mHeight+offset, mHeight);
            radius = px;
        }else {
            int minSide = Math.min(mWidth, mHeight);
            mWidth = minSide;
            mHeight = minSide;
            rectF.set(0f, 0f, mWidth , mHeight );
            radius = px;
        }

        this.setMeasuredDimension(mWidth, mHeight);

        //button size is about to 1/4 of image size then we divide it to 8.
        mButtonRadius = mWidth / 8.0f;

        //We resize play/pause drawable with button radius. button needs to be inside circle.
        mPlayPauseDrawable.resize((1.2f * mButtonRadius / 5.0f), (3.0f * mButtonRadius / 5.0f) + 10.0f,
                (mButtonRadius / 5.0f));

        mPlayPauseDrawable.setBounds(0, 0, mWidth, mHeight);

        mButtonRegion = new Region((int) (mCenterX - mButtonRadius), (int) (mCenterY - mButtonRadius),
                (int) (mCenterX + mButtonRadius), (int) (mCenterY + mButtonRadius));

        createShader();

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * This is where magic happens as you know.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mShader == null) return;

        //Draw cover image
        //float radius = (mHeight/2)-10;
       // float radius = mCenterX <= mCenterY ? mCenterX - 45.0f : mCenterY - 45.0f;
        canvas.drawColor(getResources().getColor(R.color.colorTransparent));
        canvas.rotate(mRotateDegrees, mCenterX, mCenterY);
        canvas.drawCircle(mCenterX, mCenterY, radius, mPaintCover);

        //Rotate back to make play/pause button stable(No turn)
        canvas.rotate(-mRotateDegrees, mCenterX, mCenterY);

        //Draw Play/Pause button
        canvas.drawCircle(mCenterX, mCenterY, mButtonRadius, mPaintButton);

        if (mFirstDraw) {
            toggle();
            mFirstDraw = false;
        }

        mPlayPauseDrawable.draw(canvas);
    }

    /**
     * We need to convert drawable (which we get from attributes) to bitmap
     * to prepare if for BitmapShader
     */
    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    /**
     * Create shader and set shader to mPaintCover
     */
    private void createShader() {

        if (mWidth == 0) return;

        //if mBitmapCover is null then create default colored cover
        if (mBitmapCover == null) {
            mBitmapCover = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
            mBitmapCover.eraseColor(mCoverColor);
        }

        /*
      Scale image to view width/height
     */
        float mCoverScale = ((float) mWidth) / (float) mBitmapCover.getWidth();

        Log.v("Bitmap size",mBitmapCover.getWidth()+"*"+mBitmapCover.getHeight());

        //mBitmapCover = ThumbnailUtils.extractThumbnail(mBitmapCover, 400, 400);

        try {

            if (mBitmapCover.getWidth() >= mBitmapCover.getHeight()){

                mBitmapCover = Bitmap.createBitmap(
                        mBitmapCover,
                        mBitmapCover.getWidth()/2 - mBitmapCover.getHeight()/2,
                        0,
                        mBitmapCover.getHeight(),
                        mBitmapCover.getHeight()
                );

            }else{

                mBitmapCover = Bitmap.createBitmap(
                        mBitmapCover,
                        0,
                        mBitmapCover.getHeight()/2 - mBitmapCover.getWidth()/2,
                        mBitmapCover.getWidth(),
                        mBitmapCover.getWidth()
                );
            }

        }catch (OutOfMemoryError ignored){

        }
        //Log.v(Constants.TAG,"dimentions" + mBitmapCover.getHeight() + mBitmapCover.getWidth());

        mBitmapCover =
                Bitmap.createScaledBitmap(mBitmapCover, (int) (mBitmapCover.getWidth() * mCoverScale),
                        (int) (mBitmapCover.getHeight() * mCoverScale), true);

        mShader = new BitmapShader(mBitmapCover, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        mPaintCover = new Paint();
        mPaintCover.setAntiAlias(true);
        mPaintCover.setShader(mShader);
    }

    /**
     * Update rotate degree of cover and invalide onDraw();
     */
    public void updateCoverRotate() {
        mRotateDegrees += VELOCITY;
        mRotateDegrees = mRotateDegrees % 360;
        postInvalidate();
    }

    /**
     * Checks is rotating
     */
    public boolean isRotating() {
        return isRotating;
    }

    /**
     * Start turning image
     */
    public void start() {
        isRotating = true;
        //mPlayPauseDrawable.setPlaying(false);     //added this to remove the infamous bug

        mHandlerRotate.removeCallbacksAndMessages(null);
        if(MyApp.getPref().getBoolean(getContext().getString(R.string.pref_disc_rotation),true)) {
            mHandlerRotate.postDelayed(mRunnableRotate, ROTATE_DELAY);
        }
        mPlayPauseDrawable.setPlaying(true);
        postInvalidate();
    }


    /**
     * Stop turning image and make play pause button to pause state
     */
    public void stop() {
        isRotating = false;
        mPlayPauseDrawable.setPlaying(isRotating);
        if(MyApp.getPref().getBoolean(getContext().getString(R.string.pref_disc_rotation),true)) {
            mHandlerRotate.removeCallbacksAndMessages(null);
        }
        postInvalidate();
    }

    /**
     * sets cover image
     * @param bitmap
     */
    public void setCoverBitmap(Bitmap bitmap) {
        mBitmapCover =bitmap;
        createShader();
        postInvalidate();
    }

    /**
     * This is detect when mButtonRegion is clicked. Which means
     * play/pause action happened.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                return true;
            }
            case MotionEvent.ACTION_UP: {
                if (mButtonRegion.contains((int) x, (int) y)) {
                    if (onClickListener != null) onClickListener.onClick(this);
                } else {
                    if (onDoubleClickListener != null) onDoubleClickListener.onClick(this);
                }
            }
            break;

            default: break;
        }

        return super.onTouchEvent(event);
    }

    /**
     * onClickListener.onClick will be called when button clicked.
     * We dont want all view click. We only want button area click.
     * That is why we override it.
     */
    @Override
    public void setOnClickListener(OnClickListener l) {
        onClickListener = l;
    }

    /**
     * Play pause drawable callback
     */
    Drawable.Callback callback = new Drawable.Callback() {
        @Override
        public void invalidateDrawable(Drawable who) {
            postInvalidate();
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {

        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {

        }
    };

    /**
     * Notified when button toggled
     */
    @Override
    public void onToggled() {
        toggle();
    }

    /**
     * Animate play/pause image
     */
    public void toggle() {
        if (mAnimatorSet != null) {
            mAnimatorSet.cancel();
        }

        mAnimatorSet = new AnimatorSet();
        final Animator pausePlayAnim = mPlayPauseDrawable.getPausePlayAnimator();
        mAnimatorSet.setInterpolator(new DecelerateInterpolator());
        mAnimatorSet.setDuration(PLAY_PAUSE_ANIMATION_DURATION);
        mAnimatorSet.playTogether(pausePlayAnim);
        mAnimatorSet.start();
    }

    //************************* class ends here *********************************











    //unused methods, might be needed in future
    public void setOnDoubleClickListener(DoubleClickListener d){onDoubleClickListener = d;}
    /**
     * Resize bitmap with @newHeight and @newWidth parameters
     */
    private Bitmap getResizedBitmap(Bitmap bm, float newHeight, float newWidth) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    /**
     * Sets button color
     */
    public void setButtonColor(int color) {
        mButtonColor = color;
        mPaintButton.setColor(mButtonColor);
        postInvalidate();
    }

    /**
     * remove handler callbacks and messages
     */
    public void removeHandlerCallback(){
        mHandlerRotate.removeCallbacksAndMessages(null);
    }

    /**
     * Set velocity.When updateCoverRotate() method called,
     * increase degree by velocity value.
     */
    public void setVelocity(float velocity) {
        if (velocity > 0) VELOCITY = velocity;
    }

    /**
     * set cover image resource
     */
    public void setCoverDrawable(int coverDrawable) {
        Drawable drawable = getContext().getResources().getDrawable(coverDrawable);
        mBitmapCover = drawableToBitmap(drawable);
        createShader();
        postInvalidate();
    }

    /**
     * sets cover image
     * @param drawable
     */
    public void setCoverDrawable(Drawable drawable) {
        mBitmapCover = drawableToBitmap(drawable);
        createShader();
        postInvalidate();
    }
}
