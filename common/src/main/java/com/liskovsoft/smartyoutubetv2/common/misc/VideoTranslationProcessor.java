package com.liskovsoft.smartyoutubetv2.common.misc;

import android.content.Context;
import android.text.TextUtils;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.prefs.VideoTranslationData;
import com.liskovsoft.smartyoutubetv2.common.prefs.common.DataChangeBase.OnDataChange;

/** Translates visible feed titles and available descriptions after the card data has loaded. */
public final class VideoTranslationProcessor implements BrowseProcessor, OnDataChange {
    private final Context mContext;
    private final OnItemReady mOnItemReady;
    private final VideoTranslationData mPreferences;

    public VideoTranslationProcessor(Context context, OnItemReady onItemReady) {
        mContext = context.getApplicationContext();
        mOnItemReady = onItemReady;
        mPreferences = VideoTranslationData.instance(context);
        mPreferences.setOnChange(this);
    }

    @Override
    public void process(VideoGroup group) {
        if (!mPreferences.isEnabled() || group == null || group.isEmpty()) {
            return;
        }
        String target = VideoTranslationData.getTargetLanguage(mContext);
        for (Video video : group.getVideos()) {
            if (video == null) {
                continue;
            }
            translateTitle(video, target);
            translateDescription(video, target);
        }
    }

    private void translateTitle(Video video, String target) {
        String source = video.getOriginalTitleForTranslation();
        if (TextUtils.isEmpty(source)) {
            return;
        }
        String key = target + '\n' + source;
        if (key.equals(video.translationTitleKey)) {
            return;
        }
        video.translationTitleKey = key;
        VideoMetadataTranslation.translate(mContext, source, translated -> {
            if (!mPreferences.isEnabled()
                    || !target.equals(VideoTranslationData.getTargetLanguage(mContext))) {
                return;
            }
            video.translatedTitle = translated;
            video.translatedTitleSource = source;
            mOnItemReady.onItemReady(video);
        });
    }

    private void translateDescription(Video video, String target) {
        String source = video.description;
        if (TextUtils.isEmpty(source)) {
            return;
        }
        String key = target + '\n' + source;
        if (key.equals(video.translationDescriptionKey)) {
            return;
        }
        video.translationDescriptionKey = key;
        VideoMetadataTranslation.translate(mContext, source, translated -> {
            if (!mPreferences.isEnabled()
                    || !target.equals(VideoTranslationData.getTargetLanguage(mContext))) {
                return;
            }
            video.translatedDescription = translated;
            mOnItemReady.onItemReady(video);
        });
    }

    @Override
    public void onDataChange() {
        // The current feed is refreshed by the Settings page after a preference is changed.
    }

    @Override
    public void dispose() {
        mPreferences.removeOnChange(this);
    }
}
