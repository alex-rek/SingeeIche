package com.liskovsoft.smartyoutubetv2.common.prefs;

import android.content.Context;

import com.google.mlkit.nl.translate.TranslateLanguage;
import com.liskovsoft.smartyoutubetv2.common.prefs.common.DataSaverBase;

/** Preferences for on-device translation of video titles and descriptions. */
public final class VideoTranslationData extends DataSaverBase {
    private static VideoTranslationData sInstance;
    private static volatile boolean sEnabled;
    private static volatile boolean sShowOriginal;
    private static volatile String sTargetLanguage;
    private final Context mContext;

    private VideoTranslationData(Context context) {
        super(context);
        mContext = context.getApplicationContext();
        refreshCachedValues(mContext);
    }

    public static synchronized VideoTranslationData instance(Context context) {
        if (sInstance == null) {
            sInstance = new VideoTranslationData(context.getApplicationContext());
        }
        return sInstance;
    }

    public boolean isEnabled() {
        return getBoolean(0, false);
    }

    public void setEnabled(boolean enabled) {
        setBoolean(0, enabled);
        sEnabled = enabled;
    }

    public boolean isShowOriginal() {
        return getBoolean(1, false);
    }

    public void setShowOriginal(boolean showOriginal) {
        setBoolean(1, showOriginal);
        sShowOriginal = showOriginal;
    }

    /** Empty language means follow the app's current interface language. */
    public String getLanguage() {
        return getString(2, "");
    }

    public void setLanguage(String language) {
        setString(2, language == null ? "" : language);
        refreshCachedValues(mContext);
    }

    public boolean isWifiOnly() {
        return getBoolean(3, true);
    }

    public void setWifiOnly(boolean wifiOnly) {
        setBoolean(3, wifiOnly);
    }

    public boolean isSubtitleTranslationEnabled() {
        return getBoolean(4, false);
    }

    public void setSubtitleTranslationEnabled(boolean enabled) {
        setBoolean(4, enabled);
    }

    public static boolean isTranslationEnabled() {
        return sEnabled;
    }

    public static boolean isShowingOriginal() {
        return sShowOriginal;
    }

    public static String getTargetLanguage(Context context) {
        VideoTranslationData data = instance(context);
        String code = data.getLanguage();
        if (code.isEmpty()) {
            code = context.getResources().getConfiguration().getLocales().get(0).getLanguage();
        }
        String supported = TranslateLanguage.fromLanguageTag(code);
        return supported != null ? supported : TranslateLanguage.ENGLISH;
    }

    private void refreshCachedValues(Context context) {
        sEnabled = getBoolean(0, false);
        sShowOriginal = getBoolean(1, false);
        String language = getString(2, "");
        if (language.isEmpty()) {
            language = context.getResources().getConfiguration().getLocales().get(0).getLanguage();
        }
        String supported = TranslateLanguage.fromLanguageTag(language);
        sTargetLanguage = supported != null ? supported : TranslateLanguage.ENGLISH;
    }
}
