package com.propcycle.app.data.chat;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class ChatValidatorTest {

    @Test
    public void message_rejectsNullBlankAndWhitespace() {
        assertNotNull(ChatValidator.messageError(null));
        assertNotNull(ChatValidator.messageError(""));
        assertNotNull(ChatValidator.messageError("  \n\t "));
    }

    @Test
    public void message_acceptsTrimmedTextAtLimit() {
        assertNull(ChatValidator.messageError("  hello  "));
        assertNull(ChatValidator.messageError("x".repeat(ChatValidator.MAX_MESSAGE_LENGTH)));
    }

    @Test
    public void message_rejectsTextOverLimit() {
        assertNotNull(ChatValidator.messageError(
                "x".repeat(ChatValidator.MAX_MESSAGE_LENGTH + 1)));
    }

    @Test
    public void marketplaceThread_rejectsOwnerChatAndUnsafeIds() {
        assertNotNull(ChatValidator.marketplaceThreadError(
                "listing", "same-user", "Backdrop", "same-user"));
        assertNotNull(ChatValidator.marketplaceThreadError(
                "listing/child", "owner", "Backdrop", "contact"));
        assertNotNull(ChatValidator.marketplaceThreadError(
                "listing", "owner", " ", "contact"));
    }

    @Test
    public void marketplaceThread_acceptsValidContext() {
        assertNull(ChatValidator.marketplaceThreadError(
                "listing123", "owner123", "Reusable Backdrop", "contact123"));
    }

    @Test
    public void marketplaceThreadId_isStableAndContextBound() {
        assertEquals(
                "marketplace_listing123_owner123_contact123",
                ChatValidator.marketplaceThreadId(
                        "listing123", "owner123", "contact123"));
    }

    @Test
    public void lendingThread_isValidatedAndUsesItsOwnNamespace() {
        assertNull(ChatValidator.lendingThreadError(
                "item123", "owner123", "Portable lights", "borrower123"));
        assertNotNull(ChatValidator.lendingThreadError(
                "item123", "same-user", "Portable lights", "same-user"));
        assertEquals(
                "lending_item123_owner123_borrower123",
                ChatValidator.lendingThreadId(
                        "item123", "owner123", "borrower123"));
    }

    @Test
    public void threadNavigation_requiresOneDocumentId() {
        assertNotNull(ChatValidator.threadIdError(""));
        assertNotNull(ChatValidator.threadIdError("threads/nested"));
        assertNull(ChatValidator.threadIdError("marketplace_listing_owner_contact"));
    }

    @Test
    public void operationId_matchesReciprocalRuleBounds() {
        assertFalse(ChatValidator.isValidOperationId("too-short"));
        assertTrue(ChatValidator.isValidOperationId(
                "123e4567-e89b-12d3-a456-426614174000"));
        assertFalse(ChatValidator.isValidOperationId("x".repeat(81)));
    }
}
