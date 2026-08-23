package com.propcycle.app.data.marketplace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class MarketplaceImagePolicyTest {

    @Test
    public void ownedVersionedPath_roundTripsAsPrivateGsUrl() {
        String path = MarketplaceImagePolicy.objectPath(
                "owner-user", "listing-one", "version_one");
        String url = MarketplaceImagePolicy.gsUrl(
                "propcycle-e5f14.firebasestorage.app", path);

        assertEquals(
                "marketplace/owner-user/listing-one/primary_version_one.jpg",
                path);
        assertTrue(MarketplaceImagePolicy.isOwnedListingGsUrl(
                url, "owner-user", "listing-one"));
        assertTrue(MarketplaceImagePolicy.isGsUrlForBucket(
                url, "propcycle-e5f14.firebasestorage.app"));
    }

    @Test
    public void foreignOrPublicUrls_areRejected() {
        String valid = "gs://bucket.firebasestorage.app/marketplace/owner-user/"
                + "listing-one/primary_version.jpg";
        assertFalse(MarketplaceImagePolicy.isOwnedListingGsUrl(
                valid, "another-owner", "listing-one"));
        assertFalse(MarketplaceImagePolicy.isOwnedListingGsUrl(
                valid, "owner-user", "another-listing"));
        assertFalse(MarketplaceImagePolicy.isOwnedListingGsUrl(
                "https://example.test/photo.jpg", "owner-user", "listing-one"));
        assertFalse(MarketplaceImagePolicy.isOwnedListingGsUrl(
                "gs://bucket/marketplace/owner-user/listing-one/primary.jpg",
                "owner-user", "listing-one"));
        assertFalse(MarketplaceImagePolicy.isGsUrlForBucket(
                valid, "another-project.firebasestorage.app"));
    }

    @Test
    public void unsafePathSegments_areRejectedBeforeUpload() {
        assertThrows(IllegalArgumentException.class, () ->
                MarketplaceImagePolicy.objectPath(
                        "owner/user", "listing-one", "version"));
        assertThrows(IllegalArgumentException.class, () ->
                MarketplaceImagePolicy.objectPath(
                        "owner-user", "../listing", "version"));
        assertThrows(IllegalArgumentException.class, () ->
                MarketplaceImagePolicy.gsUrl(
                        "bad/bucket",
                        "marketplace/owner-user/listing-one/primary_version.jpg"));
    }
}
