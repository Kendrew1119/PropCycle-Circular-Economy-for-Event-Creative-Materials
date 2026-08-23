package com.propcycle.app.data.marketplace;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** Pure validation and path construction for one versioned marketplace JPEG. */
public final class MarketplaceImagePolicy {

    public static final int MAX_ENCODED_BYTES = 4 * 1024 * 1024;
    public static final int MAX_LONGEST_EDGE_PIXELS = 1_600;
    public static final String CONTENT_TYPE = "image/jpeg";
    public static final String METADATA_KIND = "marketplace-primary";

    private static final Pattern SAFE_SEGMENT =
            Pattern.compile("[A-Za-z0-9_-]{1,128}");
    private static final Pattern SAFE_BUCKET =
            Pattern.compile("[A-Za-z0-9._-]{1,255}");
    private static final Pattern VERSIONED_FILE =
            Pattern.compile("primary_[A-Za-z0-9_-]{1,64}\\.jpg");

    private MarketplaceImagePolicy() {
    }

    @NonNull
    public static String newListingId() {
        return UUID.randomUUID().toString();
    }

    @NonNull
    public static String newVersionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @NonNull
    public static String objectPath(
            @NonNull String ownerId,
            @NonNull String listingId,
            @NonNull String versionId) {
        requireSafeSegment("owner ID", ownerId);
        requireSafeSegment("listing ID", listingId);
        requireSafeSegment("image version", versionId);
        return String.format(
                Locale.ROOT,
                "marketplace/%s/%s/primary_%s.jpg",
                ownerId,
                listingId,
                versionId);
    }

    @NonNull
    public static String gsUrl(@NonNull String bucket, @NonNull String objectPath) {
        if (!SAFE_BUCKET.matcher(bucket).matches()) {
            throw new IllegalArgumentException("Storage bucket name is invalid.");
        }
        if (!isMarketplaceObjectPath(objectPath)) {
            throw new IllegalArgumentException("Marketplace image path is invalid.");
        }
        return "gs://" + bucket + "/" + objectPath;
    }

    public static boolean isOwnedListingGsUrl(
            @Nullable String url,
            @Nullable String ownerId,
            @Nullable String listingId) {
        if (url == null || ownerId == null || listingId == null
                || !SAFE_SEGMENT.matcher(ownerId).matches()
                || !SAFE_SEGMENT.matcher(listingId).matches()) {
            return false;
        }
        String prefix = "gs://";
        if (!url.startsWith(prefix)) {
            return false;
        }
        int pathStart = url.indexOf('/', prefix.length());
        if (pathStart < 0) {
            return false;
        }
        String bucket = url.substring(prefix.length(), pathStart);
        String objectPath = url.substring(pathStart + 1);
        if (!SAFE_BUCKET.matcher(bucket).matches()) {
            return false;
        }
        String expectedPrefix = "marketplace/" + ownerId + "/" + listingId + "/";
        if (!objectPath.startsWith(expectedPrefix)) {
            return false;
        }
        String fileName = objectPath.substring(expectedPrefix.length());
        return VERSIONED_FILE.matcher(fileName).matches();
    }

    public static boolean isGsUrlForBucket(
            @Nullable String url,
            @Nullable String expectedBucket) {
        if (url == null || expectedBucket == null
                || !SAFE_BUCKET.matcher(expectedBucket).matches()) {
            return false;
        }
        String prefix = "gs://" + expectedBucket + "/";
        return url.startsWith(prefix);
    }

    private static boolean isMarketplaceObjectPath(@NonNull String objectPath) {
        String[] parts = objectPath.split("/", -1);
        return parts.length == 4
                && "marketplace".equals(parts[0])
                && SAFE_SEGMENT.matcher(parts[1]).matches()
                && SAFE_SEGMENT.matcher(parts[2]).matches()
                && VERSIONED_FILE.matcher(parts[3]).matches();
    }

    private static void requireSafeSegment(@NonNull String label, @NonNull String value) {
        if (!SAFE_SEGMENT.matcher(value).matches()) {
            throw new IllegalArgumentException("The " + label + " is invalid.");
        }
    }
}
