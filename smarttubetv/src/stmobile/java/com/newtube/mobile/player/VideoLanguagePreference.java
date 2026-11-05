package com.newtube.mobile.player;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Preferred audio/dub language for new videos. Empty follows the interface language when available. */
public final class VideoLanguagePreference {
    private static final String PREFS = "video_language";
    private static final String KEY_LANGUAGE = "preferred_audio_language";

    private VideoLanguagePreference() {
    }

    @NonNull
    public static String get(Context context) {
        return preferences(context).getString(KEY_LANGUAGE, "");
    }

    @NonNull
    public static String getEffective(Context context) {
        String preferred = get(context);
        if (!preferred.isEmpty()) {
            return preferred;
        }
        return context.getResources().getConfiguration().getLocales().get(0).getLanguage();
    }

    public static void set(Context context, @NonNull String language) {
        preferences(context).edit().putString(KEY_LANGUAGE, language).apply();
    }

    public static List<Locale> supportedLanguages(Context context) {
        Locale displayLocale = context.getResources().getConfiguration().getLocales().get(0);
        List<Locale> result = new ArrayList<>();
        for (String language : Locale.getISOLanguages()) {
            Locale locale = new Locale(language);
            if (!locale.getDisplayLanguage(displayLocale).isEmpty()) {
                result.add(locale);
            }
        }
        Collator collator = Collator.getInstance(displayLocale);
        result.sort((left, right) -> collator.compare(
                left.getDisplayLanguage(displayLocale), right.getDisplayLanguage(displayLocale)));
        return result;
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
