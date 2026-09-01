package com.propcycle.app.data.chat;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class ChatParticipantPolicyTest {

    private static final ChatThread THREAD = new ChatThread(
            "thread", "marketplace", "item", "Reusable item",
            "owner", "contact", "", "", 0L, 0L);

    @Test
    public void owner_seesContactAsOtherUser() {
        assertEquals("contact", ChatParticipantPolicy.otherUserId(THREAD, "owner"));
    }

    @Test
    public void contact_seesOwnerAsOtherUser() {
        assertEquals("owner", ChatParticipantPolicy.otherUserId(THREAD, "contact"));
    }

    @Test
    public void outsider_cannotResolveAProfileLink() {
        assertEquals("", ChatParticipantPolicy.otherUserId(THREAD, "outsider"));
    }
}
