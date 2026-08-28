package com.propcycle.app.data.lending;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class LendingImagePolicyTest {

    @Test
    public void ownerPath_roundTripsAsProtectedGsUrl() {
        String path = LendingImagePolicy.objectPath(
                "owner-user", "item-one", "version_one");
        String url = LendingImagePolicy.gsUrl(
                "propcycle.firebasestorage.app", path);
        assertEquals(
                "lending/owner-user/item-one/primary_version_one.jpg",
                path);
        assertTrue(LendingImagePolicy.isOwnedItemUrl(
                url, "owner-user", "item-one"));
    }

    @Test
    public void foreignAndUnsafePaths_areRejected() {
        String valid = "gs://bucket.firebasestorage.app/lending/owner/item/primary_v1.jpg";
        assertFalse(LendingImagePolicy.isOwnedItemUrl(valid, "other", "item"));
        assertFalse(LendingImagePolicy.isOwnedItemUrl(
                "https://example.test/image.jpg", "owner", "item"));
        assertThrows(IllegalArgumentException.class, () ->
                LendingImagePolicy.objectPath("owner/path", "item", "v1"));
        assertThrows(IllegalArgumentException.class, () ->
                LendingImagePolicy.gsUrl("bad/bucket", "lending/owner/item/primary_v1.jpg"));
    }
}
