package com.newtube.mobile.peertube;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.newtube.mobile.ui.common.MobileActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Touch-first PeerTube browser. Search spans the federation; instance feeds stay server-local. */
public final class PeerTubeActivity extends MobileActivity {
    private final ExecutorService mWorker = Executors.newSingleThreadExecutor();
    private final List<PeerTubeVideo> mVideos = new ArrayList<>();
    private VideoAdapter mAdapter;
    private EditText mSearchInput;
    private TextView mServerSummary;
    private TextView mEmpty;
    private ProgressBar mProgress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createContent());
        updateServerSummary();
        showMessage(getString(R.string.mobile_peertube_no_servers));
    }

    private View createContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(themeColor(android.R.attr.colorBackground, Color.BLACK));
        root.setPadding(dp(12), dp(8), dp(12), dp(8));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText(R.string.mobile_peertube_title);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(themeColor(android.R.attr.textColorPrimary, Color.WHITE));
        toolbar.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        MaterialButton servers = new MaterialButton(this);
        servers.setText(R.string.mobile_peertube_server);
        servers.setOnClickListener(v -> showServers());
        toolbar.addView(servers);
        root.addView(toolbar);

        mServerSummary = new TextView(this);
        mServerSummary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        mServerSummary.setTextColor(themeColor(android.R.attr.textColorSecondary, Color.LTGRAY));
        mServerSummary.setPadding(dp(4), dp(2), dp(4), dp(8));
        root.addView(mServerSummary);

        LinearLayout search = new LinearLayout(this);
        search.setGravity(Gravity.CENTER_VERTICAL);
        mSearchInput = new EditText(this);
        mSearchInput.setSingleLine(true);
        mSearchInput.setHint(R.string.mobile_peertube_search_hint);
        search.addView(mSearchInput, new LinearLayout.LayoutParams(0, dp(52), 1));
        MaterialButton searchButton = new MaterialButton(this);
        searchButton.setText(R.string.mobile_peertube_search);
        searchButton.setOnClickListener(v -> search());
        search.addView(searchButton);
        root.addView(search);

        MaterialButton home = new MaterialButton(this);
        home.setText(R.string.mobile_peertube_home);
        home.setOnClickListener(v -> loadServerHome());
        root.addView(home, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        FrameLayout results = new FrameLayout(this);
        mAdapter = new VideoAdapter();
        RecyclerView list = new RecyclerView(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(mAdapter);
        results.addView(list, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mProgress = new ProgressBar(this);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(dp(44), dp(44), Gravity.CENTER);
        results.addView(mProgress, progressParams);
        mProgress.setVisibility(View.GONE);
        mEmpty = new TextView(this);
        mEmpty.setGravity(Gravity.CENTER);
        mEmpty.setTextColor(themeColor(android.R.attr.textColorSecondary, Color.LTGRAY));
        mEmpty.setPadding(dp(24), dp(16), dp(24), dp(16));
        results.addView(mEmpty, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(results, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        return root;
    }

    private void search() {
        String query = mSearchInput.getText() == null ? "" : mSearchInput.getText().toString().trim();
        if (query.isEmpty()) {
            mSearchInput.setError(getString(R.string.mobile_peertube_search_hint));
            return;
        }
        List<String> servers = PeerTubePrefs.servers(this);
        runLoad(() -> PeerTubeClients.searchBlocking(query, servers));
    }

    private void loadServerHome() {
        String server = PeerTubePrefs.primaryServer(this);
        if (server.isEmpty()) {
            showServers();
            return;
        }
        runLoad(() -> PeerTubeClients.homeBlocking(server));
    }

    private void runLoad(VideoRequest request) {
        mProgress.setVisibility(View.VISIBLE);
        mEmpty.setVisibility(View.GONE);
        mWorker.execute(() -> {
            try {
                List<PeerTubeVideo> result = request.run();
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    mProgress.setVisibility(View.GONE);
                    mVideos.clear();
                    mVideos.addAll(result);
                    mAdapter.notifyDataSetChanged();
                    showMessage(result.isEmpty() ? getString(R.string.mobile_peertube_empty) : "");
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    mProgress.setVisibility(View.GONE);
                    showMessage(getString(R.string.mobile_peertube_search_error));
                    Toast.makeText(this, error.getMessage() == null
                            ? getString(R.string.mobile_peertube_search_error) : error.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showServers() {
        List<String> current = PeerTubePrefs.servers(this);
        String[] labels = new String[current.size()];
        for (int i = 0; i < current.size(); i++) labels[i] = current.get(i);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.mobile_peertube_choose_server)
                .setItems(labels, (d, which) -> {
                    PeerTubePrefs.selectServer(this, current.get(which));
                    updateServerSummary();
                })
                .setPositiveButton(R.string.mobile_peertube_add_server, (d, which) -> addServerManually())
                .setNeutralButton(R.string.mobile_peertube_discover, (d, which) -> discoverServers())
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();
    }

    private void addServerManually() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("https://video.example.org");
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.mobile_peertube_add_server)
                .setView(input)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    try {
                        PeerTubePrefs.addServer(this, input.getText().toString());
                        updateServerSummary();
                        Toast.makeText(this, R.string.mobile_peertube_server_saved, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } catch (Exception error) {
                        input.setError(error.getMessage() == null ? "Invalid server" : error.getMessage());
                    }
                }));
        dialog.show();
    }

    private void discoverServers() {
        mProgress.setVisibility(View.VISIBLE);
        mWorker.execute(() -> {
            try {
                List<PeerTubeInstance> instances = PeerTubeClients.discoverInstancesBlocking("");
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    mProgress.setVisibility(View.GONE);
                    List<PeerTubeInstance> safeInstances = new ArrayList<>();
                    for (PeerTubeInstance instance : instances) {
                        try {
                            PeerTubeApiKt.normalizePeerTubeServer(instance.getHost());
                            safeInstances.add(instance);
                        } catch (Exception ignored) { }
                    }
                    if (safeInstances.isEmpty()) {
                        Toast.makeText(this, R.string.mobile_peertube_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String[] labels = new String[Math.min(30, safeInstances.size())];
                    for (int i = 0; i < labels.length; i++) {
                        PeerTubeInstance item = safeInstances.get(i);
                        String displayName = TextUtils.isEmpty(item.getName()) ? item.getHost() : item.getName();
                        long videos = item.getTotalVideos() == null ? 0 : item.getTotalVideos();
                        labels[i] = getString(R.string.mobile_peertube_instance_label, displayName, videos);
                    }
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.mobile_peertube_choose_server)
                            .setItems(labels, (dialog, which) -> {
                                PeerTubePrefs.addServer(this, safeInstances.get(which).getHost());
                                updateServerSummary();
                                Toast.makeText(this, R.string.mobile_peertube_server_saved, Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    mProgress.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.mobile_peertube_search_error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void updateServerSummary() {
        if (mServerSummary == null) return;
        List<String> servers = PeerTubePrefs.servers(this);
        mServerSummary.setText(servers.isEmpty()
                ? getString(R.string.mobile_peertube_no_servers)
                : TextUtils.join(" · ", servers));
    }

    private void showMessage(String message) {
        mEmpty.setText(message);
        mEmpty.setVisibility(TextUtils.isEmpty(message) ? View.GONE : View.VISIBLE);
    }

    private void openVideo(PeerTubeVideo item) {
        mProgress.setVisibility(View.VISIBLE);
        mWorker.execute(() -> {
            try {
                String host = item.sourceServer(PeerTubePrefs.primaryServer(this));
                PeerTubeVideo detail = PeerTubeClients.videoBlocking(host, item.apiId());
                String stream = detail.playableUrl(0);
                if (TextUtils.isEmpty(stream)) throw new IllegalStateException(getString(R.string.mobile_peertube_video_unavailable));
                PeerTubeVideo playItem = detail;
                runOnUiThread(() -> {
                    mProgress.setVisibility(View.GONE);
                    Intent intent = new Intent(this, PeerTubePlayerActivity.class)
                            .putExtra(PeerTubePlayerActivity.EXTRA_URL, stream)
                            .putExtra(PeerTubePlayerActivity.EXTRA_TITLE, playItem.getName())
                            .putExtra(PeerTubePlayerActivity.EXTRA_PAGE_URL, playItem.getUrl())
                            .putExtra(PeerTubePlayerActivity.EXTRA_SERVER, host);
                    startActivity(intent);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    mProgress.setVisibility(View.GONE);
                    Toast.makeText(this, error.getMessage() == null
                            ? getString(R.string.mobile_peertube_video_unavailable) : error.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int themeColor(int attribute, int fallback) {
        TypedValue value = new TypedValue();
        return getTheme().resolveAttribute(attribute, value, true) ? value.data : fallback;
    }

    @Override
    protected void onDestroy() {
        mWorker.shutdownNow();
        super.onDestroy();
    }

    private interface VideoRequest { List<PeerTubeVideo> run() throws Exception; }

    private final class VideoAdapter extends RecyclerView.Adapter<VideoHolder> {
        @Override public VideoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            MaterialCardView card = new MaterialCardView(PeerTubeActivity.this);
            card.setRadius(dp(12));
            card.setCardElevation(dp(2));
            card.setUseCompatPadding(true);
            LinearLayout content = new LinearLayout(PeerTubeActivity.this);
            content.setOrientation(LinearLayout.VERTICAL);
            ImageView thumb = new ImageView(PeerTubeActivity.this);
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            content.addView(thumb, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(190)));
            TextView title = new TextView(PeerTubeActivity.this);
            title.setTextColor(themeColor(android.R.attr.textColorPrimary, Color.WHITE));
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            title.setMaxLines(2);
            title.setEllipsize(TextUtils.TruncateAt.END);
            title.setPadding(dp(12), dp(8), dp(12), dp(2));
            content.addView(title);
            TextView meta = new TextView(PeerTubeActivity.this);
            meta.setTextColor(themeColor(android.R.attr.textColorSecondary, Color.LTGRAY));
            meta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            meta.setPadding(dp(12), 0, dp(12), dp(10));
            content.addView(meta);
            card.addView(content);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, dp(6), 0, dp(6));
            card.setLayoutParams(params);
            return new VideoHolder(card, thumb, title, meta);
        }

        @Override public void onBindViewHolder(VideoHolder holder, int position) {
            PeerTubeVideo video = mVideos.get(position);
            holder.title.setText(video.getName());
            String host = video.sourceServer(PeerTubePrefs.primaryServer(PeerTubeActivity.this));
            holder.meta.setText(host + (video.getViews() > 0 ? " · " + video.getViews() + " views" : ""));
            List<String> thumbs = video.thumbnailCandidates(host);
            Glide.with(PeerTubeActivity.this).load(thumbs.isEmpty() ? video.getThumbnailUrl() : thumbs.get(0))
                    .centerCrop().into(holder.thumbnail);
            holder.itemView.setOnClickListener(v -> openVideo(video));
        }

        @Override public int getItemCount() { return mVideos.size(); }
    }

    private static final class VideoHolder extends RecyclerView.ViewHolder {
        final ImageView thumbnail;
        final TextView title;
        final TextView meta;
        VideoHolder(View itemView, ImageView thumbnail, TextView title, TextView meta) {
            super(itemView);
            this.thumbnail = thumbnail;
            this.title = title;
            this.meta = meta;
        }
    }
}
