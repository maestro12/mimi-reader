package com.emogoth.android.phone.mimi.util;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.view.Surface;
import android.view.TextureView;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.io.IOException;
import java.util.ArrayList;

public class ExoPlayer2Helper implements ExoPlayer.EventListener, TransferListener, SimpleExoPlayer.VideoListener, AudioRendererEventListener, VideoRendererEventListener, ExtractorMediaSource.EventListener {

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private static final int ALLOCATION_SIZE = 65_535;

    private final FileDataSourceFactory dataSourceFactory;
    private final DefaultExtractorsFactory extractorsFactory;

    private final SimpleExoPlayer player;

    private final Handler handler = new Handler();
    private final ArrayList<Listener> listeners = new ArrayList<>();

    public ExoPlayer2Helper(Context context) {
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        dataSourceFactory = new FileDataSourceFactory(this);
        extractorsFactory = new DefaultExtractorsFactory();

        DefaultTrackSelector trackSelector = new DefaultTrackSelector(bandwidthMeter);
        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context);
        EventLogger eventLogger = new EventLogger(trackSelector);

        player = ExoPlayerFactory.newSimpleInstance(context, renderersFactory, trackSelector);
        player.addListener(this);
        player.addListener(eventLogger);
        player.setAudioDebugListener(eventLogger);
        player.setVideoDebugListener(eventLogger);
        player.setMetadataOutput(eventLogger);
        player.setVideoListener(this);
    }

    public void initVideo(Uri videoUrl) {
        ExtractorMediaSource mediaSource = new ExtractorMediaSource(videoUrl, dataSourceFactory, extractorsFactory, handler, this);
        LoopingMediaSource loopingMediaSource = new LoopingMediaSource(mediaSource);

        player.prepare(loopingMediaSource);
    }

    public void setTextureView(TextureView view) {
        if (player != null) {
            player.setVideoTextureView(view);
        }
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        player.setPlayWhenReady(playWhenReady);
    }

    public void seekTo(long positionMs) {
        player.seekTo(positionMs);
    }

    public void release() {
        player.release();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void clearListeners() {
        listeners.clear();
    }

    public void start() {
        player.setPlayWhenReady(true);
    }

    public void pause() {
        player.setPlayWhenReady(false);
    }

    public boolean isPlaying() {
        return player.getPlayWhenReady();
    }

    public void mute(boolean muted) {
        player.setVolume(muted ? 0 : 1);
    }

    public long getDuration() {
        return player.getDuration();
    }

    public long getCurrentPosition() {
        return player.getCurrentPosition();
    }

    public boolean isMuted() {
        return player.getVolume() == 0;
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        for (Listener listener : listeners) {
            listener.onStateChanged(playWhenReady, playbackState);
        }
    }

    @Override
    public void onRepeatModeChanged(int i) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        for (Listener listener : listeners) {
            listener.onError(error);
        }
    }

    @Override
    public void onPositionDiscontinuity(int reason) {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }

    @Override
    public void onVideoEnabled(DecoderCounters counters) {

    }

    @Override
    public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {

    }

    @Override
    public void onVideoInputFormatChanged(Format format) {

    }

    @Override
    public void onDroppedFrames(int count, long elapsedMs) {

    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        for (Listener listener : listeners) {
            listener.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio);
        }
    }

    @Override
    public void onRenderedFirstFrame(Surface surface) {
        for (Listener listener : listeners) {
            listener.onRenderedFirstFrame(surface);
        }
    }

    @Override
    public void onVideoDisabled(DecoderCounters counters) {

    }

    @Override
    public void onRenderedFirstFrame() {

    }

    @Override
    public void onAudioEnabled(DecoderCounters counters) {

    }

    @Override
    public void onAudioSessionId(int audioSessionId) {

    }

    @Override
    public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {

    }

    @Override
    public void onAudioInputFormatChanged(Format format) {

    }

    @Override
    public void onAudioSinkUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {

    }

    @Override
    public void onAudioDisabled(DecoderCounters counters) {

    }

    @Override
    public void onLoadError(IOException error) {

    }

    @Override
    public void onTransferInitializing(DataSource source, DataSpec dataSpec, boolean isNetwork) {

    }

    @Override
    public void onTransferStart(DataSource source, DataSpec dataSpec, boolean isNetwork) {

    }

    @Override
    public void onBytesTransferred(DataSource source, DataSpec dataSpec, boolean isNetwork, int bytesTransferred) {

    }

    @Override
    public void onTransferEnd(DataSource source, DataSpec dataSpec, boolean isNetwork) {

    }

    /**
     * A listener for core events.
     */
    public interface Listener {
        void onStateChanged(boolean playWhenReady, int playbackState);

        void onError(Exception e);

        void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                float pixelWidthHeightRatio);

        void onDrawnToSurface(Surface surface);

        void onRenderedFirstFrame(Surface surface);
    }
}
