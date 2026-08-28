package com.propcycle.app.data.lending;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** Pure path and ownership checks for one protected lending JPEG. */
public final class LendingImagePolicy {

    public static final int MAX_ENCODED_BYTES = 4 * 1024 * 1024;
    public static final String CONTENT_TYPE = "image/jpeg";
    public static final String METADATA_KIND = "lending-primary";
    private static final Pattern SAFE_SEGMENT = Pattern.compile("[A-Za-z0-9_-]{1,128}");
    private static final Pattern SAFE_BUCKET = Pattern.compile("[A-Za-z0-9._-]{1,255}");
    private static final Pattern VERSIONED_FILE =
            Pattern.compile("primary_[A-Za-z0-9_-]{1,64}\\.jpg");

    private LendingImagePolicy() {
    }

    @NonNull
    public static String newItemId() {
        return UUID.randomUUID().toString();
    }

    @NonNull
    public static String newVersionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @NonNull
    public static String objectPath(
            @NonNull String ownerId,
            @NonNull String itemId,
            @NonNull String versionId) {
        requireSafe(ownerId);
        requireSafe(itemId);
        requireSafe(versionId);
        return String.format(
                Locale.ROOT,
                "lending/%s/%s/primary_%s.jpg",
                ownerId,
                itemId,
                versionId);
    }

    @NonNull
    public static String gsUrl(@NonNull String bucket, @NonNull String path) {
        if (!SAFE_BUCKET.matcher(bucket).matches() || !isObjectPath(path)) {
            throw new IllegalArgumentException("Lending image path is invalid.");
        }
        return "gs://" + bucket + "/" + path;
    }

    public static boolean isOwnedItemUrl(
            @Nullable String url,
            @Nullable String ownerId,
            @Nullable String itemId) {
        if (url == null || ownerId == null || itemId == null
                || !SAFE_SEGMENT.matcher(ownerId).matches()
                || !SAFE_SEGMENT.matcher(itemId).matches()
                || !url.startsWith("gs://")) {
            return false;
        }
        int pathStart = url.indexOf('/', 5);
        if (pathStart < 0 || !SAFE_BUCKET.matcher(url.substring(5, pathStart)).matches()) {
            return false;
        }
        String prefix = "lending/" + ownerId + "/" + itemId + "/";
        String path = url.substring(pathStart + 1);
        return path.startsWith(prefix)
                && VERSIONED_FILE.matcher(path.substring(prefix.length())).matches();
    }

    private static boolean isObjectPath(@NonNull String path) {
        String[] parts = path.split("/", -1);
        return parts.length == 4
                && "lending".equals(parts[0])
                && SAFE_SEGMENT.matcher(parts[1]).matches()
                && SAFE_SEGMENT.matcher(parts[2]).matches()
                && VERSIONED_FILE.matcher(parts[3]).matches();
    }

    private static void requireSafe(@NonNull String value) {
        if (!SAFE_SEGMENT.matcher(value).matches()) {
            throw new IllegalArgumentException("Lending image segment is invalid.");
        }
    }
}
