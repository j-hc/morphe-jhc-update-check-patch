package app.morphe.extension.jhc;

import android.util.Pair;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.widget.LinearLayout;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.os.Build;
import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Arrays;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class JhcUpdateCheckPatch {
    private static final String TAG = "JhcUpdateCheckPatch";

    private static final String PREFERENCES_NAME = "jhc_release_update";
    private static final String LATEST_RELEASE_TAG_KEY = "jhc_last_update";
    private static final String LATEST_RELEASE_TAG_KEY_DISABLED = "-";
    private static final String LAST_UPDATE_CHECK_TIME_KEY = "jhc_last_update_check_time";
    private static final String PENDING_RELEASE_TAG_KEY = "jhc_pending_release_tag";
    private static final String PENDING_RELEASE_ASSET_URL_KEY = "jhc_pending_release_asset_url";
    private static final String PENDING_LATEST_RELEASE_TAG_KEY = "jhc_pending_latest_release_tag";
    private static final long UPDATE_CHECK_INTERVAL_MILLIS = 6 * 60 * 60 * 1000L; // A period of 6 hours is just finee
    private static String updateRepo = "j-hc/revanced-magisk-module";

    public static void checkUpdate(Context context) {
        SharedPreferences preferences = context.getApplicationContext().getSharedPreferences(
                PREFERENCES_NAME, Context.MODE_PRIVATE);
        String lastUpdate = preferences.getString(LATEST_RELEASE_TAG_KEY, "");

        if (LATEST_RELEASE_TAG_KEY_DISABLED.equals(lastUpdate)) {
            Log.d(TAG, "Update checks are disabled");
            return;
        }
        String packageName = context.getPackageName();
        updateRepo = getResourceString(context, packageName, "jhc_update_repo");

        if (lastUpdate.isBlank()) {
            lastUpdate = getResourceString(context, packageName, "jhc_current_tag");
            saveLastUpdateTag(preferences, lastUpdate);
        }
        
        LatestRelease pendingRelease = getPendingRelease(preferences);
        if (pendingRelease != null) {
            Log.d(TAG, "Showing pending update " + pendingRelease.tagName);
            new Handler(Looper.getMainLooper()).post(
                    () -> showReleaseDialog(context, preferences, pendingRelease)
            );
            return;
        }

        long now = System.currentTimeMillis();
        long lastCheckTime = preferences.getLong(LAST_UPDATE_CHECK_TIME_KEY, 0);
        if (now - lastCheckTime < UPDATE_CHECK_INTERVAL_MILLIS) {
            Log.d(TAG, "Next check in " + ((UPDATE_CHECK_INTERVAL_MILLIS - (now - lastCheckTime)) / 60000) + "m");
            return;
        }

        fetchLatestRelease(packageName, lastUpdate, latestRelease -> {
            savePendingRelease(preferences, latestRelease);
            new Handler(Looper.getMainLooper()).post(
                    () -> showReleaseDialog(context, preferences, latestRelease)
            );
        });

        preferences.edit().putLong(LAST_UPDATE_CHECK_TIME_KEY, now).apply();
    }

    private static String getResourceString(Context context, String packageName, String id) {
        int resourceId = context.getResources().getIdentifier(
                id,
                "string",
                packageName
        );
        if (resourceId == 0) {
            Log.e(TAG, "Missing " + id);
            return null;
        }

        String res = context.getString(resourceId);
        if (res.isBlank()) {
            Log.e(TAG, id + " is blank");
            return null;
        }
        Log.d(TAG, id + " : " + res);
        return res;
    }

    private static void showReleaseDialog(
            Context context,
            SharedPreferences preferences,
            LatestRelease latestRelease
    ) {
        Pair<Dialog, LinearLayout> dialogPair = CustomDialog.create(
                context,
                "Update available",
                "A new build on " + updateRepo + " is available.",
                null,
                "Download",
                () -> {
                    removePendingRelease(preferences);
                    saveLastUpdateTag(preferences, latestRelease.latestTagName);
                    openReleaseAsset(context, latestRelease.assetUrl);
                },
                () -> {},
                "Ignore",
                () -> {
                    removePendingRelease(preferences);
                    saveLastUpdateTag(preferences, latestRelease.latestTagName);
                },
                true,
                "Disable update checks",
                () -> {
                    removePendingRelease(preferences);
                    disableUpdateChecks(preferences);
                },
                true,
                true
        );
        dialogPair.first.show();
    }

    private static void saveLastUpdateTag(SharedPreferences preferences, String latestReleaseTag) {
        preferences.edit().putString(LATEST_RELEASE_TAG_KEY, latestReleaseTag).apply();
    }

    private static LatestRelease getPendingRelease(SharedPreferences preferences) {
        String tagName = preferences.getString(PENDING_RELEASE_TAG_KEY, "");
        String assetUrl = preferences.getString(PENDING_RELEASE_ASSET_URL_KEY, "");
        String latestTagName = preferences.getString(PENDING_LATEST_RELEASE_TAG_KEY, "");
        if (tagName.isBlank() || assetUrl.isBlank() || latestTagName.isBlank()) {
            return null;
        }
        return new LatestRelease(tagName, assetUrl, latestTagName);
    }

    private static void savePendingRelease(
            SharedPreferences preferences,
            LatestRelease latestRelease
    ) {
        preferences.edit()
                .putString(PENDING_RELEASE_TAG_KEY, latestRelease.tagName)
                .putString(PENDING_RELEASE_ASSET_URL_KEY, latestRelease.assetUrl)
                .putString(PENDING_LATEST_RELEASE_TAG_KEY, latestRelease.latestTagName)
                .apply();
    }

    private static void removePendingRelease(SharedPreferences preferences) {
        preferences.edit()
                .remove(PENDING_RELEASE_TAG_KEY)
                .remove(PENDING_RELEASE_ASSET_URL_KEY)
                .remove(PENDING_LATEST_RELEASE_TAG_KEY)
                .apply();
    }

    private static void disableUpdateChecks(SharedPreferences preferences) {
        preferences.edit().putString(LATEST_RELEASE_TAG_KEY, LATEST_RELEASE_TAG_KEY_DISABLED).apply();
    }

    private static void openReleaseAsset(Context context, String assetUrl) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(assetUrl));
        if (!(context instanceof android.app.Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Log.e(TAG, "No app can open the page", exception);
        }
    }

    private static void fetchLatestRelease(
            String packageName,
            String lastCheckedTag,
            Consumer<LatestRelease> onLatestRelease
    ) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("https://api.github.com/repos/" + updateRepo + "/releases?per_page=5");

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10_000);
                connection.setReadTimeout(10_000);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                // connection.setRequestProperty("User-Agent", "");

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "HTTP " + responseCode);
                    return;
                }

                String response = readResponse(connection.getInputStream());
                if (response == null) {
                    return;
                }

                JSONArray releases = new JSONArray(response);
                JSONObject newestRelease = releases.optJSONObject(0);
                String latestTagName = newestRelease == null ? "" : newestRelease.optString("tag_name");
                if (latestTagName.isBlank()) {
                    Log.e(TAG, "No latest release tag");
                    return;
                }
                if (latestTagName.equals(lastCheckedTag)) {
                    Log.d(TAG, "Is up-to-date " + latestTagName);
                    return;
                }

                LatestRelease latestRelease = findLatestMatchingRelease(releases, packageName);
                if (latestRelease == null) {
                    Log.i(TAG, "No matching release asset for " + packageName);
                    return;
                }

                Log.i(TAG, "Latest matching " + latestRelease.tagName);
                onLatestRelease.accept(new LatestRelease(
                        latestRelease.tagName,
                        latestRelease.assetUrl,
                        latestTagName
                ));
            } catch (Exception exception) {
                Log.e(TAG, "Failed to fetch the latest release: " + exception);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }, "github-latest-release").start();
    }

    private static String findAssetUrl(JSONArray assets, String packageName, String abi) {
        if (assets == null) {
            return null;
        }

        String[] packageNameParts = packageName.split("\\.");
        if (packageNameParts.length == 0) {
            Log.e(TAG, "?");
            return null;
        }

        for (int index = 0; index < assets.length(); index++) {
            JSONObject asset = assets.optJSONObject(index);
            if (asset == null) return null;
            String assetName = asset.optString("name");

            if (assetName.endsWith("-all.apk")) {}
            else if (assetName.endsWith("-arm64-v8a.apk") && abi.equals("arm64-v8a")) {}
            else if (assetName.endsWith("-arm-v7a.apk") && abi.equals("arm-v7a")) {}
            else continue;

            if (isCorrespondingAsset(assetName, packageNameParts)) {
                String assetUrl = asset.optString("browser_download_url");
                if (!assetUrl.isBlank()) {
                    return assetUrl;
                }
            }
        }
        return null;
    }

    private static LatestRelease findLatestMatchingRelease(JSONArray releases, String packageName) {
        String abi = getArmAbi();
        for (int index = 0; index < releases.length(); index++) {
            JSONObject release = releases.optJSONObject(index);
            if (release == null) continue;

            String tagName = release.optString("tag_name");
            if (tagName.isBlank()) continue;

            String assetUrl = findAssetUrl(release.optJSONArray("assets"), packageName, abi);
            if (assetUrl != null) {
                return new LatestRelease(tagName, assetUrl);
            }
        }
        return null;
    }

    private static boolean isCorrespondingAsset(String assetName, String[] packageNameParts) {
        String assetNameLower = assetName.toLowerCase();

        String l = packageNameParts[packageNameParts.length - 1];
        if (l.equals("youtube")) return assetNameLower.startsWith("youtube-");
        if (l.equals("music")) return assetNameLower.startsWith("music-");
        if (l.equals("photos")) return assetNameLower.startsWith("googlephotos-");

        for (String part : packageNameParts) {
            if (part.equals("twitter")) return assetNameLower.startsWith("twitter-");
            if (part.equals("reddit")) return assetNameLower.startsWith("reddit-");
        }

        return false;
    }

    private static String readResponse(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } catch (Exception exception) {
            Log.e(TAG, "Failed to read response", exception);
            return null;
        }
    }

    private static final class LatestRelease {
        private final String tagName;
        private final String assetUrl;
        private final String latestTagName;

        private LatestRelease(String tagName, String assetUrl) {
            this(tagName, assetUrl, tagName);
        }

        private LatestRelease(String tagName, String assetUrl, String latestTagName) {
            this.tagName = tagName;
            this.assetUrl = assetUrl;
            this.latestTagName = latestTagName;
        }
    }

    private static String getArmAbi() {
        String[] abis;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            abis = Build.SUPPORTED_ABIS;
        } else {
            abis = new String[]{Build.CPU_ABI};
        }

        for (String abi : abis) {
            if ("arm64-v8a".equals(abi)) return "arm64-v8a";
            if ("armeabi-v7a".equals(abi)) return "armeabi-v7a";
        }

        return null;
    }
}
