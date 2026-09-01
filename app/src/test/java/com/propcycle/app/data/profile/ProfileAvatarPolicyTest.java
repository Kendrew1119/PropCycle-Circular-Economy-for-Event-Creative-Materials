package com.propcycle.app.data.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ProfileAvatarPolicyTest {

    @Test
    public void allowlist_acceptsEveryDisplayedChoice() {
        assertEquals(ProfileAvatarPolicy.keys().size(), ProfileAvatarPolicy.labels().size());
        for (String key : ProfileAvatarPolicy.keys()) {
            assertTrue(ProfileAvatarPolicy.isValid(key));
        }
    }

    @Test
    public void normalized_rejectsUnknownOrMissingValues() {
        assertFalse(ProfileAvatarPolicy.isValid("custom-url"));
        assertEquals(ProfileAvatarPolicy.DEFAULT, ProfileAvatarPolicy.normalized("custom-url"));
        assertEquals(ProfileAvatarPolicy.DEFAULT, ProfileAvatarPolicy.normalized(null));
    }
}
