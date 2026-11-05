package com.liskovsoft.smartyoutubetv2.common.misc;

import android.content.Context;
import android.text.TextUtils;
import android.util.LruCache;

import androidx.annotation.NonNull;

import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.liskovsoft.smartyoutubetv2.common.prefs.VideoTranslationData;

import java.util.HashMap;
import java.util.Map;

/** On-device title and description translation. Source text is never sent to a translation API. */
public final class VideoMetadataTranslation {
    public interface Callback {
        void onTranslated(@NonNull String text);
    }

    private static final Object LOCK = new Object();
    private static final Map<String, Translator> TRANSLATORS = new HashMap<>();
    private static final LruCache<String, String> CACHE = new LruCache<>(300);
    private static final LanguageIdentifier LANGUAGE_IDENTIFIER = LanguageIdentification.getClient();
    private static final String[] SUPPORTED_LANGUAGES = {
            "af", "ar", "be", "bg", "bn", "ca", "cs", "cy", "da", "de", "el", "en", "eo",
            "es", "et", "fa", "fi", "fr", "ga", "gl", "gu", "he", "hi", "hr", "ht", "hu",
            "id", "is", "it", "ja", "ka", "kn", "ko", "lt", "lv", "mk", "mr", "ms", "mt",
            "nl", "no", "pl", "pt", "ro", "ru", "sk", "sl", "sq", "sv", "sw", "ta", "te",
            "th", "tl", "tr", "uk", "ur", "vi", "zh"
    };

    private VideoMetadataTranslation() {
    }

    public static String[] supportedLanguageCodes() {
        return SUPPORTED_LANGUAGES.clone();
    }

    public static void translate(Context context, String text, Callback callback) {
        if (TextUtils.isEmpty(text) || callback == null) {
            return;
        }
        String target = VideoTranslationData.getTargetLanguage(context);
        String cacheKey = target + '\n' + text;
        synchronized (LOCK) {
            String cached = CACHE.get(cacheKey);
            if (cached != null) {
                callback.onTranslated(cached);
                return;
            }
        }

        LANGUAGE_IDENTIFIER.identifyLanguage(text)
                .addOnSuccessListener(sourceTag -> {
                    String source = TranslateLanguage.fromLanguageTag(sourceTag);
                    if (source == null || sourceTag.equals("und") || source.equals(target)) {
                        callback.onTranslated(text);
                        return;
                    }
                    Translator translator = getTranslator(source, target);
                    DownloadConditions conditions = new DownloadConditions.Builder()
                            .requireWifi()
                            .build();
                    translator.downloadModelIfNeeded(conditions)
                            .continueWithTask(task -> {
                                if (!task.isSuccessful()) {
                                    throw task.getException() != null ? task.getException()
                                            : new IllegalStateException("Translation model unavailable");
                                }
                                return translator.translate(text);
                            })
                            .addOnSuccessListener(result -> {
                                if (!TextUtils.isEmpty(result)) {
                                    synchronized (LOCK) {
                                        CACHE.put(cacheKey, result);
                                    }
                                    callback.onTranslated(result);
                                } else {
                                    callback.onTranslated(text);
                                }
                            })
                            .addOnFailureListener(error -> callback.onTranslated(text));
                })
                .addOnFailureListener(error -> callback.onTranslated(text));
    }

    private static Translator getTranslator(String source, String target) {
        String key = source + ':' + target;
        synchronized (LOCK) {
            Translator translator = TRANSLATORS.get(key);
            if (translator == null) {
                translator = com.google.mlkit.nl.translate.Translation.getClient(
                        new TranslatorOptions.Builder()
                                .setSourceLanguage(source)
                                .setTargetLanguage(target)
                                .build());
                TRANSLATORS.put(key, translator);
            }
            return translator;
        }
    }
}
