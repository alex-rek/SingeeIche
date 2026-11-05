package com.newtube.mobile.ui.browse;

import android.app.Activity;
import android.content.res.Configuration;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Browse's top bar. On a bottom-nav tab it is the SingeeIche wordmark; a
 * section opened from a You row (Sports, Playlists, ...) is a sub-screen of You, so it gets a back
 * arrow and the section's name - like YouTube's You sub-pages - instead of a wordmark that gave
 * no hint where the user was or that back returns to You.
 */
final class BrowseTopBar {
    private static final float SUBSCREEN_TITLE_SP = 20f;

    private final TextView mTitle;
    private final ImageView mDarkLogo;
    private final ImageView mLightLogo;
    private final View mBack;
    private final int mWordmarkInset;
    private final int mBesideBackInset;

    BrowseTopBar(Activity activity, Runnable onBack) {
        mTitle = activity.findViewById(R.id.mobile_title_text);
        mDarkLogo = activity.findViewById(R.id.mobile_title_logo_dark);
        mLightLogo = activity.findViewById(R.id.mobile_title_logo_light);
        mBack = activity.findViewById(R.id.mobile_title_back);
        mBack.setOnClickListener(v -> onBack.run());
        mWordmarkInset = mTitle.getPaddingStart();
        mBesideBackInset = activity.getResources()
                .getDimensionPixelSize(R.dimen.mobile_dialog_title_inset_with_back);
    }

    /**
     * A tab shows the wordmark; a sub-screen shows the back arrow and {@code title} (the wordmark
     * text if the section came without one - the arrow is what matters).
     */
    void show(boolean subScreen, CharSequence title) {
        mBack.setVisibility(subScreen ? View.VISIBLE : View.GONE);
        mTitle.setVisibility(subScreen ? View.VISIBLE : View.GONE);
        boolean darkTheme = (mTitle.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        mDarkLogo.setVisibility(!subScreen && darkTheme ? View.VISIBLE : View.GONE);
        mLightLogo.setVisibility(!subScreen && !darkTheme ? View.VISIBLE : View.GONE);
        if (subScreen) {
            mTitle.setText(title != null && title.length() > 0
                    ? title : mTitle.getContext().getString(R.string.app_name));
        }
        mTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, SUBSCREEN_TITLE_SP);
        mTitle.setPaddingRelative(subScreen ? mBesideBackInset : mWordmarkInset,
                mTitle.getPaddingTop(), mTitle.getPaddingEnd(), mTitle.getPaddingBottom());
    }
}
