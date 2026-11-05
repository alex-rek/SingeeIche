package com.newtube.mobile.peertube;

import android.app.PictureInPictureParams;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Rational;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.google.android.material.button.MaterialButton;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.newtube.mobile.ui.common.MobileActivity;

/** Picture-in-picture surface for PeerTube; the foreground service keeps its audio alive. */
public final class PeerTubePlayerActivity extends MobileActivity {
    public static final String EXTRA_URL = "singeiche:peertube_url";
    public static final String EXTRA_TITLE = "singeiche:peertube_title";
    public static final String EXTRA_PAGE_URL = "singeiche:peertube_page_url";
    public static final String EXTRA_SERVER = "singeiche:peertube_server";

    private PlayerView mPlayerView;
    private PeerTubePlaybackService mService;
    private boolean mBound;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            mService = ((PeerTubePlaybackService.LocalBinder) binder).getService();
            ExoPlayer player = mService.getPlayer();
            if (mPlayerView != null && player != null) mPlayerView.setPlayer(player);
            updatePipParams();
        }

        @Override public void onServiceDisconnected(ComponentName name) {
            if (mPlayerView != null) mPlayerView.setPlayer(null);
            mService = null;
        }
    };

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String url = getIntent().getStringExtra(EXTRA_URL);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (url == null || url.trim().isEmpty()) {
            finish();
            return;
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        setContentView(createContent(title));
        Intent playback = playbackIntent(url, title,
                getIntent().getStringExtra(EXTRA_PAGE_URL), getIntent().getStringExtra(EXTRA_SERVER));
        ContextCompat.startForegroundService(this, playback);
        bindService(playback, mConnection, BIND_AUTO_CREATE);
        mBound = true;
        updatePipParams();
    }

    private View createContent(String title) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(android.graphics.Color.BLACK);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        MaterialButton stop = new MaterialButton(this);
        stop.setText(R.string.mobile_peertube_stop);
        stop.setOnClickListener(v -> {
            PeerTubePlaybackService.stop(this);
            finish();
        });
        toolbar.addView(stop, new LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView name = new TextView(this);
        name.setText(title == null ? getString(R.string.mobile_peertube_player) : title);
        name.setTextColor(android.graphics.Color.WHITE);
        name.setTextSize(16);
        name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setPadding(12, 0, 12, 0);
        toolbar.addView(name, new LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(toolbar);

        mPlayerView = new PlayerView(this);
        mPlayerView.setUseController(true);
        mPlayerView.setControllerAutoShow(true);
        mPlayerView.setKeepContentOnPlayerReset(true);
        root.addView(mPlayerView, new LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        return root;
    }

    private Intent playbackIntent(String url, String title, String pageUrl, String server) {
        return new Intent(this, PeerTubePlaybackService.class)
                .setAction(PeerTubePlaybackService.ACTION_PLAY)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_PAGE_URL, pageUrl)
                .putExtra(EXTRA_SERVER, server);
    }

    private void updatePipParams() {
        if (Build.VERSION.SDK_INT < 26) return;
        try {
            setPictureInPictureParams(new PictureInPictureParams.Builder()
                    .setAspectRatio(new Rational(16, 9)).build());
        } catch (IllegalArgumentException ignored) { }
    }

    @Override protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= 26 && !isInPictureInPictureMode()) {
            try {
                enterPictureInPictureMode(new PictureInPictureParams.Builder()
                        .setAspectRatio(new Rational(16, 9)).build());
            } catch (IllegalArgumentException ignored) { }
        }
    }

    @Override public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode,
                                                          android.content.res.Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (mPlayerView != null) mPlayerView.setUseController(!isInPictureInPictureMode);
    }

    @Override protected void onDestroy() {
        if (mPlayerView != null) mPlayerView.setPlayer(null);
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        super.onDestroy();
    }
}
