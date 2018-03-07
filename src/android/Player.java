/*
 The MIT License (MIT)

 Copyright (c) 2017 Nedim Cholich

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */
package co.pejonic.cordova.plugin.exoplayer;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.DebugTextViewHelper;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.google.android.gms.cast.framework.CastContext;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONObject;

public class Player {
    private CastContext castContext;
    private ImageButton btn_lock;
    public static final String TAG = "ExoPlayerPlugin";
    private Activity activity;
    private final CallbackContext callbackContext;
    private final Configuration config;
    private Dialog dialog;
    private SimpleExoPlayer exoPlayer;
    private SimpleExoPlayerView exoView;
    private CordovaWebView webView;
    private int controllerVisibility;
    private boolean paused = false;
    private SimpleExoPlayerView castControlView;
    private AudioManager audioManager;
    private ProgressBar progressBar = null;
    //private FrameLayout mainLayout;
    private RelativeLayout loadingPanel;
    private Handler mainHandler;
    private LinearLayout debugRootView;
    private TextView debugTextView;
    private ImageButton btn_settings ;
    private DataSource.Factory mediaDataSourceFactory;
    private SimpleExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private TrackSelectionHelper trackSelectionHelper;
    private DebugTextViewHelper debugViewHelper;
    private boolean inErrorState;
    private TrackGroupArray lastSeenTrackGroupArray;
    private MappedTrackInfo mappedTrackInfo = null;
    private FrameLayout mainLayout = null;
    private Activity dialogActivity = null;
    private TrackGroupArray trackGroups;


    public Player(Configuration config, Activity activity, CallbackContext callbackContext, CordovaWebView webView) {
        this.config = config;
        this.activity = activity;
        this.callbackContext = callbackContext;
        this.webView = webView;

        this.audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);


    }

    private ExoPlayer.EventListener playerEventListener = new ExoPlayer.EventListener() {

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            Log.i(TAG, "Playback parameters changed");

        }



        @Override
        public void onPlayerError(ExoPlaybackException error) {
            JSONObject payload = Payload.playerErrorEvent(Player.this.exoPlayer, error, null);
            new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.ERROR, payload, true);
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            JSONObject payload = Payload.loadingEvent(Player.this.exoPlayer, isLoading);
            new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.OK, payload, true);
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {


            if (playbackState == ExoPlayer.STATE_BUFFERING) {
                Log.d("myFilter", "Song finished");
                
                progressBar.setVisibility(View.VISIBLE);

                //loading = (ProgressBar) findViewById();
            }
            else if(playbackState == ExoPlayer.STATE_READY){
                progressBar.setVisibility(View.GONE);

            }
            else if(playbackState == ExoPlayer.STATE_ENDED){
                progressBar.setVisibility(View.GONE);
            }
            JSONObject payload = Payload.stateEvent(Player.this.exoPlayer, playbackState, Player.this.controllerVisibility == View.VISIBLE);
            new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.OK, payload, true);
        }

        @Override
        public void onPositionDiscontinuity() {
            JSONObject payload = Payload.positionDiscontinuityEvent(Player.this.exoPlayer);
            new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.OK, payload, true);
        }

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {
            JSONObject payload = Payload.timelineChangedEvent(Player.this.exoPlayer, timeline, manifest);
            new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.OK, payload, true);
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            // Need to see if we want to send this to Cordova.
            if (trackGroups != lastSeenTrackGroupArray) {
                MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                if (mappedTrackInfo != null) {
                    if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_VIDEO)
                            == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                       // showToast(R.string.error_unsupported_video);
                    }
                    if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_AUDIO)
                            == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                       // showToast(R.string.error_unsupported_audio);

                    }
                }
                lastSeenTrackGroupArray = trackGroups;
            }
        }

        @Override
        public void onRepeatModeChanged(int newRepeatMode) {
            // Need to see if we want to send this to Cordova.
        }
    };

    private DialogInterface.OnDismissListener dismissListener = new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialog) {
            if (exoPlayer != null) {
                exoPlayer.release();
            }
            exoPlayer = null;
            JSONObject payload = Payload.stopEvent(exoPlayer);
            new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.OK, payload, true);
        }
    };


    private DialogInterface.OnKeyListener onKeyListener = new DialogInterface.OnKeyListener() {
        @Override
        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            int action = event.getAction();
            String key = KeyEvent.keyCodeToString(event.getKeyCode());
            // We need android to handle these key events
            if (key.equals("KEYCODE_VOLUME_UP") ||
                    key.equals("KEYCODE_VOLUME_DOWN") ||
                    key.equals("KEYCODE_VOLUME_MUTE")) {
                return false;
            }
            else {
                JSONObject payload = Payload.keyEvent(event);
                new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.OK, payload, true);
                return true;
            }
        }
    };


    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        int previousAction = -1;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int eventAction = event.getAction();
            if (previousAction != eventAction) {
                previousAction = eventAction;
                JSONObject payload = Payload.touchEvent(event);
                new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.OK, payload, true);
            }
            return true;
        }
    };

    private PlaybackControlView.VisibilityListener playbackControlVisibilityListener = new PlaybackControlView.VisibilityListener() {
        @Override
        public void onVisibilityChange(int visibility) {
            Player.this.controllerVisibility = visibility;
        }
    };

    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                JSONObject payload = Payload.audioFocusEvent(Player.this.exoPlayer, "AUDIOFOCUS_LOSS_TRANSIENT");
                new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.OK, payload, true);
            }
            else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                JSONObject payload = Payload.audioFocusEvent(Player.this.exoPlayer, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.OK, payload, true);
            }
            else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                JSONObject payload = Payload.audioFocusEvent(Player.this.exoPlayer, "AUDIOFOCUS_GAIN");
                new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.OK, payload, true);
            }
            else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                JSONObject payload = Payload.audioFocusEvent(Player.this.exoPlayer, "AUDIOFOCUS_LOSS");
                new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.OK, payload, true);
            }
        }
    };



    public void createPlayer() {
        if (!config.isAudioOnly()) {
            createDialog();
        }
        preparePlayer(config.getUri());
    }

    public void createDialog() {
        dialogActivity = this.activity;
        dialog = new Dialog(dialogActivity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setOnKeyListener(onKeyListener);
        dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View decorView = dialog.getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
        dialog.setCancelable(true);
        dialog.setOnDismissListener(dismissListener);

        mainLayout = LayoutProvider.getMainLayout(this.activity);
        exoView = LayoutProvider.getExoPlayerView(this.activity, config);
        exoView.setControllerVisibilityListener(playbackControlVisibilityListener);



        mainLayout.addView(exoView);


        progressBar = new ProgressBar(this.activity);
        progressBar.getIndeterminateDrawable().setColorFilter(0xffffffff,android.graphics.PorterDuff.Mode.MULTIPLY);
        progressBar.setIndeterminate(true);

        // Center the progress bar
        /*
        Adding progressBar to view and manage trouble..... :D
        */
        progressBar.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        // Add progress throbber to view
        mainLayout.addView(progressBar);
        progressBar.bringToFront();
        progressBar.setVisibility(View.VISIBLE);
        dialog.setContentView(mainLayout);


        /// settings imagebutton stored to memory







        dialog.show();
        btn_settings = (ImageButton) new ImageButton(this.activity);
        btn_settings.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP));
        btn_settings.setImageResource(R.drawable.hplib_ic_settings);
        btn_settings.setOnClickListener(ClickListener);
        String id = "btn_settings";
        btn_settings.setId(R.id.exo_settings);
        btn_settings.setTag(3);
        btn_settings.setBackgroundColor(Color.TRANSPARENT);
        btn_settings.bringToFront();
        LinearLayout exo_buttons = mainLayout.findViewById(R.id.exo_buttons);
        exo_buttons.addView(btn_settings);
        btn_settings.setVisibility(View.VISIBLE);

        dialog.getWindow().setAttributes(LayoutProvider.getDialogLayoutParams(activity, config, dialog));
        exoView.requestFocus();
            exoView.setOnTouchListener(onTouchListener);
            exoView.getSubtitleView();
        LayoutProvider.setupController(exoView, activity, config.getController());
    }

    private int setupAudio() {
        activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        return audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    // OnClickListener methods



    private void preparePlayer(Uri uri) {
        int audioFocusResult = setupAudio();
        String audioFocusString = audioFocusResult == AudioManager.AUDIOFOCUS_REQUEST_FAILED ?
                "AUDIOFOCUS_REQUEST_FAILED" :
                "AUDIOFOCUS_REQUEST_GRANTED";
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();

        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        LoadControl loadControl = new DefaultLoadControl();

        trackSelectionHelper = new TrackSelectionHelper(trackSelector, videoTrackSelectionFactory);
        mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        exoPlayer = ExoPlayerFactory.newSimpleInstance(this.activity, trackSelector, loadControl);
        exoPlayer.addListener(playerEventListener);
        if (null != exoView) {
            exoView.setPlayer(exoPlayer);
        }

        MediaSource mediaSource = getMediaSource(uri, bandwidthMeter);
        if (mediaSource != null) {
            long offset = config.getSeekTo();
            boolean autoPlay = config.autoPlay();
            if (offset > -1) {
                exoPlayer.seekTo(offset);
            }
            exoPlayer.prepare(mediaSource);

            exoPlayer.setPlayWhenReady(autoPlay);
            paused = !autoPlay;

            JSONObject payload = Payload.startEvent(exoPlayer, audioFocusString);
            new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.OK, payload, true);
        }
        else {
            sendError("Failed to construct mediaSource for " + uri);
        }
    }

    private MediaSource getMediaSource(Uri uri, DefaultBandwidthMeter bandwidthMeter) {
        String userAgent = Util.getUserAgent(this.activity, config.getUserAgent());
        Handler mainHandler = new Handler();
        int connectTimeout = config.getConnectTimeout();
        int readTimeout = config.getReadTimeout();
        int retryCount = config.getRetryCount();

        HttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSourceFactory(userAgent, bandwidthMeter, connectTimeout, readTimeout, true);
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this.activity, bandwidthMeter, httpDataSourceFactory);
        MediaSource mediaSource;
        int type = Util.inferContentType(uri);
        switch (type) {
            case C.TYPE_DASH:
                long livePresentationDelayMs = DashMediaSource.DEFAULT_LIVE_PRESENTATION_DELAY_PREFER_MANIFEST_MS;
                DefaultDashChunkSource.Factory dashChunkSourceFactory = new DefaultDashChunkSource.Factory(dataSourceFactory);
                // Last param is AdaptiveMediaSourceEventListener
                mediaSource = new DashMediaSource(uri, dataSourceFactory, dashChunkSourceFactory, retryCount, livePresentationDelayMs, mainHandler, null);
                break;
            case C.TYPE_HLS:
                // Last param is AdaptiveMediaSourceEventListener
                mediaSource = new HlsMediaSource(uri, dataSourceFactory, retryCount, mainHandler, null);
                break;
            case C.TYPE_SS:
                DefaultSsChunkSource.Factory ssChunkSourceFactory = new DefaultSsChunkSource.Factory(dataSourceFactory);
                // Last param is AdaptiveMediaSourceEventListener
                mediaSource = new SsMediaSource(uri, dataSourceFactory, ssChunkSourceFactory, mainHandler, null);
                break;
            default:
                ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
                mediaSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, mainHandler, null);
                break;
        }

        String subtitleUrl = config.getSubtitleUrl();
        if (null != subtitleUrl) {
            Uri subtitleUri = Uri.parse(subtitleUrl);
            String subtitleType = inferSubtitleType(subtitleUri);
            Log.i(TAG, "Subtitle present: " + subtitleUri + ", type=" + subtitleType);
            com.google.android.exoplayer2.Format textFormat = com.google.android.exoplayer2.Format.createTextSampleFormat(null, subtitleType, null, com.google.android.exoplayer2.Format.NO_VALUE, com.google.android.exoplayer2.Format.NO_VALUE, "en", null, 0);
            MediaSource subtitleSource = new SingleSampleMediaSource(subtitleUri, httpDataSourceFactory, textFormat, C.TIME_UNSET);
            return new MergingMediaSource(mediaSource, subtitleSource);
        }
        else {
            return mediaSource;
        }
    }

    private static String inferSubtitleType(Uri uri) {
        String fileName = uri.getPath().toLowerCase();

        if (fileName.endsWith(".vtt")) {
            return MimeTypes.TEXT_VTT;
        }
        else {
            // Assume it's srt.
            return MimeTypes.APPLICATION_SUBRIP;
        }
    }

    public void close() {
        audioManager.abandonAudioFocus(audioFocusChangeListener);
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        if (this.dialog != null) {
            dialog.dismiss();
        }
    }

    public void setStream(Uri uri, JSONObject controller) {
        if (null != uri) {
            DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
            MediaSource mediaSource = getMediaSource(uri, bandwidthMeter);
            exoPlayer.prepare(mediaSource);
            play();
        }
        setController(controller);
    }

    private View.OnClickListener ClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //log action here
            int btn = v.getId();
            if(btn == R.id.exo_settings){
                Log.d("myFilter", "values: "+v.getId());
                mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
               // LinearLayout layoutMenu = mainLayout.findViewById(R.id.quality_control);

                PopupMenu popup = new PopupMenu(dialogActivity, v);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                        if (mappedTrackInfo != null) {
                            trackSelectionHelper.showSelectionDialog(
                                    dialogActivity, item.getTitle(), mappedTrackInfo, (item.getItemId() -1 ));
                        }

                        return false;
                    }
                });

                Menu menu = popup.getMenu();
                for (int i = 0; i < mappedTrackInfo.length; i++) {
                    trackGroups = mappedTrackInfo.getTrackGroups(i);
                    if (trackGroups.length != 0) {
                        Button button = new Button(dialogActivity);
                        int label;
                        switch (exoPlayer.getRendererType(i)) {
                            case C.TRACK_TYPE_AUDIO:

                                menu.add(1, (i + 1), (i + 1), "Language");
                                break;
                            case C.TRACK_TYPE_VIDEO:
                                menu.add(1, (i + 1), (i + 1), "Quality");
                                break;
                            case C.TRACK_TYPE_TEXT:
                                menu.add(1, (i + 1), (i + 1), "Subtitle");
                                break;
                        }
                    }
                }

                popup.show();
                //mainLayout.addView(layoutMenu); //adding layout to the main view port
            }


        }
    };
    public void playPause() {
        if (this.paused) {
            play();
        }
        else {
            pause();
        }
    }


    private void pause() {
        paused = true;
        exoPlayer.setPlayWhenReady(false);
    }

    private void play() {
        paused = false;

        exoPlayer.setPlayWhenReady(true);
    }

    public void stop() {
        paused = false;
        exoPlayer.stop();
    }

    public void settings(){

    }
    private long normalizeOffset(long newTime) {
        long duration = exoPlayer.getDuration();
        return duration == 0 ? 0 : Math.min(Math.max(0, newTime), duration);
    }

    public JSONObject seekTo(long timeMillis) {
        long newTime = normalizeOffset(timeMillis);
        exoPlayer.seekTo(newTime);
        JSONObject payload = Payload.seekEvent(Player.this.exoPlayer, newTime);
        return payload;
    }

    public JSONObject seekBy(long timeMillis) {
        long newTime = normalizeOffset(exoPlayer.getCurrentPosition() + timeMillis);
        exoPlayer.seekTo(newTime);
        JSONObject payload = Payload.seekEvent(Player.this.exoPlayer, newTime);
        return payload;
    }

    public JSONObject getPlayerState() {
        return Payload.stateEvent(exoPlayer,
                null != exoPlayer ? exoPlayer.getPlaybackState() : SimpleExoPlayer.STATE_ENDED,
                Player.this.controllerVisibility == View.VISIBLE);
    }


    public void showController() {
        if (null != exoView) {
            exoView.showController();
        }
    }



    public void hideController() {
        if (null != exoView) {
            exoView.hideController();
        }
    }

    public void setController(JSONObject controller) {
        if (null != exoView) {
            LayoutProvider.setupController(exoView, activity, controller);
        }
    }

    private void sendError(String msg) {
        Log.e(TAG, msg);
        JSONObject payload = Payload.playerErrorEvent(Player.this.exoPlayer, null, msg);
        new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.ERROR, payload, true);
    }




}
