package com.tysci.videoplayer.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.tysci.videoplayer.R;

import java.io.IOException;
import java.util.Map;

import tv.danmaku.ijk.media.player.AndroidMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Created by Administrator on 2016/2/3.
 */
public class VideoPlayerView extends FrameLayout implements SurfaceHolder.Callback,IMediaPlayer.OnPreparedListener,
IMediaPlayer.OnCompletionListener,IMediaPlayer.OnBufferingUpdateListener,IMediaPlayer.OnErrorListener,IMediaPlayer.OnVideoSizeChangedListener,
IMediaPlayer.OnInfoListener,IMediaPlayer.OnSeekCompleteListener{
    private final static String TAG = "VideoView";

    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    private Context context;
    private SeekBar sbPlayProgress;
    private SurfaceView svVideoView;
    private SurfaceHolder surfaceHolder;
    private ProgressBar loadingProgressBar;
    private TextView tvPlayerTimes;
    private TextView tvVideoTimes;
    private CheckBox cbPlayerState;
    private LinearLayout layoutPlayerControls;

    private Uri mUri;
    private IMediaPlayer mediaVideoPlayer;
    private int mCurrentBufferPercentage;
    private Map<String, String> mHeaders;
    private int mSeekWhenPrepared;  // recording the seek position while preparing

    // mCurrentState is a VideoView object's current state.
    // mTargetState is the state that a method caller intends to reach.
    // For instance, regardless the VideoView object's current state,
    // calling pause() intends to bring the object to a target state
    // of STATE_PAUSED.
    private int mCurrentState = STATE_IDLE;
    private int mTargetState = STATE_IDLE;



    public VideoPlayerView(Context context) {
        super(context);
        initViews(context);
    }

    public VideoPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViews(context);
    }

    public VideoPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViews(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public VideoPlayerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initViews(context);
    }

    private void initViews(Context context){
        this.context=context;
        LayoutInflater.from(context).inflate(R.layout.layout_player_video_view,this,true);
        sbPlayProgress = (SeekBar)this.findViewById(R.id.sb_play_progress);
        svVideoView=(SurfaceView)this.findViewById(R.id.sv_play_view);
        surfaceHolder=svVideoView.getHolder();
        loadingProgressBar= (ProgressBar) this.findViewById(R.id.pb_loading);
        layoutPlayerControls=(LinearLayout)this.findViewById(R.id.layout_player_control);
        tvPlayerTimes=(TextView)this.findViewById(R.id.tv_play_times);
        tvVideoTimes=(TextView)this.findViewById(R.id.tv_videos_times);
        cbPlayerState=(CheckBox)this.findViewById(R.id.cb_play_state);

        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
        surfaceHolder.addCallback(this);
    }

    private IMediaPlayer createMediaPlayer(){
       IjkMediaPlayer mediaPlayer= new IjkMediaPlayer();
        mediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);

        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);

        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 0);

        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_YV12);

        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);

        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);

        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
        //AndroidMediaPlayer androidMediaPlayer = new AndroidMediaPlayer();

        return mediaPlayer;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void openVideo() {
        if (mUri == null || surfaceHolder == null) {
            // not ready for playback just yet, will try again later
            return;
        }
        // we shouldn't clear the target state, because somebody might have
        // called start() previously
        release(false);

        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        try {
            mediaVideoPlayer = createMediaPlayer();
            mediaVideoPlayer.setDisplay(surfaceHolder);
            // TODO: create SubtitleController in MediaPlayer, but we need
            // a context for the subtitle renderers
            final Context context = getContext();
            // REMOVED: SubtitleController
            // REMOVED: mAudioSession
            mediaVideoPlayer.setOnPreparedListener(this);
            mediaVideoPlayer.setOnVideoSizeChangedListener(this);
            mediaVideoPlayer.setOnCompletionListener(this);
            mediaVideoPlayer.setOnErrorListener(this);
            mediaVideoPlayer.setOnBufferingUpdateListener(this);
            //mCurrentBufferPercentage = 0;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                mediaVideoPlayer.setDataSource(context, mUri, mHeaders);
            } else {
                mediaVideoPlayer.setDataSource(mUri.toString());
            }
            mediaVideoPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaVideoPlayer.setScreenOnWhilePlaying(true);
            mediaVideoPlayer.prepareAsync();
//            if (mHudViewHolder != null)
//                mHudViewHolder.setMediaPlayer(mMediaPlayer);

            // REMOVED: mPendingSubtitleTracks

            // we don't set the target state here either, but preserve the
            // target state that was there before.
            mCurrentState = STATE_PREPARING;
           // attachMediaController();
        } catch (IOException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            onError(mediaVideoPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
           onError(mediaVideoPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        } finally {
            // REMOVED: mPendingSubtitleTracks.clear();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e(TAG,"surfaceCreated...");
        surfaceHolder=holder;
        if(mediaVideoPlayer!=null){
            mediaVideoPlayer.setDisplay(holder);
        }else{
            openVideo();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e(TAG,"surfaceChanged...");
        if (mediaVideoPlayer != null ) {
//            if (mSeekWhenPrepared != 0) {
//                seekTo(mSeekWhenPrepared);
//            }
            start();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    /**
     * Sets video path.
     *
     * @param path the path of the video.
     */
    public void setVideoPath(String path) {
        setVideoURI(Uri.parse(path));
    }


    /**
     * Sets video URI.
     *
     * @param uri the URI of the video.
     */
    public void setVideoURI(Uri uri) {
        setVideoURI(uri, null);
    }

    /**
     * Sets video URI using specific headers.
     *
     * @param uri     the URI of the video.
     * @param headers the headers for the URI request.
     *                Note that the cross domain redirection is allowed by default, but that can be
     *                changed with key/value pairs through the headers parameter with
     *                "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value
     *                to disallow or allow cross domain redirection.
     */
    private void setVideoURI(Uri uri, Map<String, String> headers) {
        mUri = uri;
        mHeaders = headers;
        mSeekWhenPrepared = 0;
        openVideo();
    }

    public void start(){
        if (isInPlaybackState()) {
            mediaVideoPlayer.start();
            mCurrentState = STATE_PLAYING;
        }
        mTargetState = STATE_PLAYING;
    }

    private boolean isInPlaybackState() {
        return (mediaVideoPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    /*
     * release the media player in any state
     */
    public void release(boolean cleartargetstate) {
        if (mediaVideoPlayer != null) {
            mediaVideoPlayer.reset();
            mediaVideoPlayer.release();
            mediaVideoPlayer = null;
            // REMOVED: mPendingSubtitleTracks.clear();
            mCurrentState = STATE_IDLE;
            if (cleartargetstate) {
                mTargetState = STATE_IDLE;
            }
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(null);
        }
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer mp, int percent) {

    }

    @Override
    public void onCompletion(IMediaPlayer mp) {

    }

    @Override
    public boolean onError(IMediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public boolean onInfo(IMediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(IMediaPlayer mp) {
        Log.e(TAG,"onPrepared...");
        mCurrentState = STATE_PREPARED;

    }

    @Override
    public void onSeekComplete(IMediaPlayer mp) {

    }

    @Override
    public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den) {
        Log.e(TAG,"onVideoSizeChanged...");

    }
}
