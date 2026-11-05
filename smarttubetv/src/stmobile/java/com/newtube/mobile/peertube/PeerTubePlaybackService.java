package com.newtube.mobile.peertube;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import com.liskovsoft.smartyoutubetv2.tv.R;

/** Owns a PeerTube Media3 player so playback can survive the browser Activity going away. */
public final class PeerTubePlaybackService extends Service {
    static final String ACTION_PLAY = "com.newtube.mobile.peertube.PLAY";
    static final String ACTION_TOGGLE = "com.newtube.mobile.peertube.TOGGLE";
    static final String ACTION_STOP = "com.newtube.mobile.peertube.STOP";
    static final int NOTIFICATION_ID = 41421;
    static final String CHANNEL_ID = "singeiche_peertube_playback";

    private final IBinder mBinder = new LocalBinder();
    private ExoPlayer mPlayer;
    private MediaSessionCompat mSession;
    private String mTitle = "";
    private String mUrl = "";
    private String mPageUrl = "";
    private String mServer = "";

    final class LocalBinder extends Binder {
        PeerTubePlaybackService getService() { return PeerTubePlaybackService.this; }
    }

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                .setUserAgent("SingeeIche PeerTube")
                .setConnectTimeoutMs(8_000)
                .setReadTimeoutMs(20_000)
                .setAllowCrossProtocolRedirects(true);
        mPlayer = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(new DefaultDataSource.Factory(this, http)))
                .build();
        mPlayer.setHandleAudioBecomingNoisy(true);
        mPlayer.setWakeMode(C.WAKE_MODE_NETWORK);
        mPlayer.addListener(new Player.Listener() {
            @Override public void onIsPlayingChanged(boolean isPlaying) { publishState(); }
            @Override public void onPlaybackStateChanged(int state) { publishState(); }
            @Override public void onPlayerError(PlaybackException error) { publishState(); }
        });

        mSession = new MediaSessionCompat(this, "SingeeIche PeerTube");
        mSession.setCallback(new MediaSessionCompat.Callback() {
            @Override public void onPlay() { if (mPlayer != null) mPlayer.play(); }
            @Override public void onPause() { if (mPlayer != null) mPlayer.pause(); }
            @Override public void onSeekTo(long position) { if (mPlayer != null) mPlayer.seekTo(position); }
            @Override public void onStop() { stopPlayback(); }
        });
        mSession.setActive(true);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_PLAY : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopPlayback();
            return START_NOT_STICKY;
        }
        if (ACTION_TOGGLE.equals(action)) {
            if (mPlayer != null) {
                if (mPlayer.isPlaying()) mPlayer.pause(); else mPlayer.play();
            }
            publishState();
            return START_NOT_STICKY;
        }
        if (intent != null) {
            String url = intent.getStringExtra(PeerTubePlayerActivity.EXTRA_URL);
            if (url != null && !url.equals(mUrl)) {
                mUrl = url;
                mTitle = intent.getStringExtra(PeerTubePlayerActivity.EXTRA_TITLE);
                mPageUrl = intent.getStringExtra(PeerTubePlayerActivity.EXTRA_PAGE_URL);
                mServer = intent.getStringExtra(PeerTubePlayerActivity.EXTRA_SERVER);
                setPeerTubeMedia(url);
            } else if (url != null && mPlayer != null && !mPlayer.isPlaying()) {
                mPlayer.play();
            }
        }
        startForegroundCompat(buildNotification());
        if (mPlayer != null && mPlayer.getPlaybackState() == Player.STATE_IDLE) {
            mPlayer.prepare();
            mPlayer.play();
        }
        publishState();
        return START_NOT_STICKY;
    }

    private void setPeerTubeMedia(String url) {
        Uri uri = Uri.parse(url);
        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                .setUserAgent("SingeeIche PeerTube")
                .setConnectTimeoutMs(8_000)
                .setReadTimeoutMs(20_000)
                .setAllowCrossProtocolRedirects(true);
        if (mServer != null && !mServer.isEmpty()) {
            java.util.Map<String, String> headers = new java.util.HashMap<>();
            headers.put("Origin", mServer);
            if (mPageUrl != null && !mPageUrl.isEmpty()) headers.put("Referer", mPageUrl);
            http.setDefaultRequestProperties(headers);
        }
        DefaultDataSource.Factory source = new DefaultDataSource.Factory(this, http);
        MediaItem item = new MediaItem.Builder()
                .setMediaId(url)
                .setUri(uri)
                .setMediaMetadata(new MediaMetadata.Builder().setTitle(mTitle).setArtist(mServer).build())
                .build();
        mPlayer.setMediaSource(new DefaultMediaSourceFactory(source).createMediaSource(item));
        updateSessionMetadata();
        mPlayer.prepare();
        mPlayer.play();
    }

    private void updateSessionMetadata() {
        if (mSession == null) return;
        mSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mTitle)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mServer)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mPlayer == null ? 0 : mPlayer.getDuration())
                .build());
    }

    private void publishState() {
        if (mSession == null || mPlayer == null) return;
        int state;
        if (mPlayer.isPlaying()) state = PlaybackStateCompat.STATE_PLAYING;
        else if (mPlayer.getPlaybackState() == Player.STATE_BUFFERING) state = PlaybackStateCompat.STATE_BUFFERING;
        else if (mPlayer.getPlaybackState() == Player.STATE_ENDED) state = PlaybackStateCompat.STATE_STOPPED;
        else state = PlaybackStateCompat.STATE_PAUSED;
        long actions = PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE
                | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SEEK_TO
                | PlaybackStateCompat.ACTION_STOP;
        mSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(actions).setState(state, mPlayer.getCurrentPosition(), mPlayer.getPlaybackParameters().speed)
                .build());
        updateSessionMetadata();
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null && !mTitle.isEmpty()) manager.notify(NOTIFICATION_ID, buildNotification());
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, PeerTubePlayerActivity.class)
                .putExtra(PeerTubePlayerActivity.EXTRA_URL, mUrl)
                .putExtra(PeerTubePlayerActivity.EXTRA_TITLE, mTitle)
                .putExtra(PeerTubePlayerActivity.EXTRA_PAGE_URL, mPageUrl)
                .putExtra(PeerTubePlayerActivity.EXTRA_SERVER, mServer)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(this, 14, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent toggle = new Intent(this, PeerTubePlaybackService.class).setAction(ACTION_TOGGLE);
        PendingIntent toggleIntent = PendingIntent.getService(this, 15, toggle,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        int icon = mPlayer != null && mPlayer.isPlaying()
                ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        NotificationCompat.Action action = new NotificationCompat.Action(icon,
                mPlayer != null && mPlayer.isPlaying() ? getString(R.string.mobile_player_pause)
                        : getString(R.string.mobile_player_play), toggleIntent);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(mTitle.isEmpty() ? getString(R.string.mobile_peertube_player) : mTitle)
                .setContentText(mServer)
                .setContentIntent(content)
                .setOnlyAlertOnce(true)
                .setOngoing(mPlayer != null && mPlayer.isPlaying())
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .addAction(action)
                .setStyle(new MediaStyle().setMediaSession(mSession == null ? null : mSession.getSessionToken())
                        .setShowActionsInCompactView(0))
                .build();
    }

    private void startForegroundCompat(Notification notification) {
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                getString(R.string.mobile_peertube_player), NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(getString(R.string.mobile_playback_channel_desc));
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }

    private void stopPlayback() {
        if (mPlayer != null) mPlayer.stop();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override public IBinder onBind(Intent intent) { return mBinder; }

    @Override public void onDestroy() {
        if (mSession != null) {
            mSession.setActive(false);
            mSession.release();
            mSession = null;
        }
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        super.onDestroy();
    }

    @Nullable ExoPlayer getPlayer() { return mPlayer; }

    static void stop(Context context) {
        context.startService(new Intent(context, PeerTubePlaybackService.class).setAction(ACTION_STOP));
    }
}
