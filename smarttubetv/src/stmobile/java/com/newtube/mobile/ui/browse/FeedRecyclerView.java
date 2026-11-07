package com.newtube.mobile.ui.browse;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/** RecyclerView for browse feeds with a bounded fling, so a short flick stays controllable. */
public final class FeedRecyclerView extends RecyclerView {
    /** Maximum fling speed in dp/s; keeps quick corrective swipes from skipping several cards. */
    private static final int MAX_FLING_DP_PER_SECOND = 1_500;

    public FeedRecyclerView(@NonNull Context context) {
        super(context);
        init();
    }

    public FeedRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FeedRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Keep pull-to-refresh on the parent SwipeRefreshLayout, without a second elastic edge
        // effect on the grid itself.
        setOverScrollMode(OVER_SCROLL_NEVER);
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        int maxVelocity = Math.round(MAX_FLING_DP_PER_SECOND * getResources()
                .getDisplayMetrics().density);
        return super.fling(limit(velocityX, maxVelocity), limit(velocityY, maxVelocity));
    }

    private static int limit(int velocity, int maxVelocity) {
        if (velocity > maxVelocity) return maxVelocity;
        if (velocity < -maxVelocity) return -maxVelocity;
        return velocity;
    }
}
