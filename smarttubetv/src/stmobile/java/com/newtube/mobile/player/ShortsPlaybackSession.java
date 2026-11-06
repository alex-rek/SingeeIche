package com.newtube.mobile.player;

import androidx.annotation.Nullable;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;

import java.util.ArrayList;
import java.util.List;

/** In-process queue handed from the Shorts feed to the full-screen player. */
public final class ShortsPlaybackSession {
    public interface RefreshListener {
        void onRefreshed(@Nullable Video firstVideo);
    }
    private static final List<Video> VIDEOS = new ArrayList<>();
    private static int sCurrentIndex = -1;
    private static boolean sActive;
    private static boolean sContinuationRequested;
    private static boolean sRefreshRequested;
    @Nullable private static RefreshListener sRefreshListener;

    private ShortsPlaybackSession() {
    }

    public static synchronized void start(List<Video> videos, Video selected) {
        VIDEOS.clear();
        addUnique(videos);
        sCurrentIndex = indexOf(selected);
        sActive = sCurrentIndex >= 0;
        sContinuationRequested = false;
    }

    public static synchronized void append(List<Video> videos) {
        int oldSize = VIDEOS.size();
        addUnique(videos);
        if (VIDEOS.size() > oldSize) {
            sContinuationRequested = false;
        }
    }

    public static synchronized void setRefreshListener(@Nullable RefreshListener listener) {
        sRefreshListener = listener;
    }

    public static synchronized boolean requestRefresh() {
        if (!sActive || sRefreshRequested) return false;
        sRefreshRequested = true;
        return true;
    }

    /** Replace the swipe queue with the refreshed Shorts feed and open its first playable item. */
    public static synchronized void onFeedRefreshed(@Nullable List<Video> videos) {
        if (!sRefreshRequested || videos == null || videos.isEmpty()) return;
        List<Video> fresh = new ArrayList<>();
        for (Video video : videos) {
            if (video != null && video.hasVideo()) fresh.add(video);
        }
        if (fresh.isEmpty()) return;
        VIDEOS.clear();
        VIDEOS.addAll(fresh);
        sCurrentIndex = 0;
        sActive = true;
        sContinuationRequested = false;
        sRefreshRequested = false;
        RefreshListener listener = sRefreshListener;
        if (listener != null) listener.onRefreshed(VIDEOS.get(0));
    }

    public static synchronized boolean contains(Video video) {
        return sActive && indexOf(video) >= 0;
    }

    public static synchronized boolean isActive() {
        return sActive;
    }

    @Nullable
    public static synchronized Video move(int direction) {
        int next = sCurrentIndex + (direction < 0 ? -1 : 1);
        if (!sActive || next < 0 || next >= VIDEOS.size()) {
            return null;
        }
        sCurrentIndex = next;
        return VIDEOS.get(sCurrentIndex);
    }

    @Nullable
    public static synchronized Video current() {
        return sCurrentIndex >= 0 && sCurrentIndex < VIDEOS.size() ? VIDEOS.get(sCurrentIndex) : null;
    }

    @Nullable
    public static synchronized Video peekNext() {
        int next = sCurrentIndex + 1;
        return sActive && next >= 0 && next < VIDEOS.size() ? VIDEOS.get(next) : null;
    }

    @Nullable
    public static synchronized Video last() {
        return VIDEOS.isEmpty() ? null : VIDEOS.get(VIDEOS.size() - 1);
    }

    public static synchronized boolean requestContinuation() {
        if (!sActive || VIDEOS.isEmpty() || sContinuationRequested
                || sCurrentIndex < VIDEOS.size() - 4) {
            return false;
        }
        sContinuationRequested = true;
        return true;
    }

    public static synchronized void clear() {
        VIDEOS.clear();
        sCurrentIndex = -1;
        sActive = false;
        sContinuationRequested = false;
        sRefreshRequested = false;
        sRefreshListener = null;
    }

    private static void addUnique(@Nullable List<Video> videos) {
        if (videos == null) {
            return;
        }
        for (Video video : videos) {
            if (video != null && video.hasVideo() && indexOf(video) < 0) {
                VIDEOS.add(video);
            }
        }
    }

    private static int indexOf(@Nullable Video video) {
        if (video == null || video.videoId == null) {
            return -1;
        }
        for (int i = 0; i < VIDEOS.size(); i++) {
            Video candidate = VIDEOS.get(i);
            if (candidate != null && video.videoId.equals(candidate.videoId)) {
                return i;
            }
        }
        return -1;
    }
}
