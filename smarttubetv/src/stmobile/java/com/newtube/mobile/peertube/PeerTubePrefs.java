package com.newtube.mobile.peertube;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** Small, app-private list of PeerTube instances chosen by the user. */
final class PeerTubePrefs {
    private static final String PREFS = "singeiche_peertube";
    private static final String KEY_SERVERS = "servers";
    private static final int MAX_SERVERS = 5;

    static List<String> servers(Context context) {
        String raw = prefs(context).getString(KEY_SERVERS, "");
        List<String> result = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return result;
        for (String line : raw.split("\\n")) {
            try {
                String host = PeerTubeApiKt.normalizePeerTubeServer(line);
                if (!result.contains(host) && result.size() < MAX_SERVERS) result.add(host);
            } catch (Exception ignored) { }
        }
        return result;
    }

    static String primaryServer(Context context) {
        List<String> servers = servers(context);
        return servers.isEmpty() ? "" : servers.get(0);
    }

    static void addServer(Context context, String value) {
        String normalized = PeerTubeApiKt.normalizePeerTubeServer(value);
        List<String> servers = servers(context);
        servers.remove(normalized);
        servers.add(0, normalized);
        if (servers.size() > MAX_SERVERS) servers = new ArrayList<>(servers.subList(0, MAX_SERVERS));
        save(context, servers);
    }

    static void selectServer(Context context, String value) {
        addServer(context, value);
    }

    static void removeServer(Context context, String value) {
        List<String> servers = servers(context);
        servers.remove(value);
        save(context, servers);
    }

    private static void save(Context context, List<String> servers) {
        prefs(context).edit().putString(KEY_SERVERS, String.join("\n", servers)).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private PeerTubePrefs() { }
}
