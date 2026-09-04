package com.propcycle.app.data.chat;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ChatMessageTest {

    @Test
    public void oldMessageWithoutTypeRemainsText() {
        ChatMessage message = new ChatMessage(
                "message-operation-0001", "user", "Hello", 1L, false);

        assertFalse(message.isMarketplaceItem());
    }

    @Test
    public void marketplaceMessageRequiresTypeAndItemId() {
        ChatMessage card = new ChatMessage(
                "marketplace_item_card",
                "buyer",
                "Marketplace item shared",
                ChatMessage.TYPE_MARKETPLACE_ITEM,
                "listing-one",
                1L,
                false);
        ChatMessage missingItem = new ChatMessage(
                "marketplace_item_card",
                "buyer",
                "Marketplace item shared",
                ChatMessage.TYPE_MARKETPLACE_ITEM,
                "",
                1L,
                false);

        assertTrue(card.isMarketplaceItem());
        assertFalse(missingItem.isMarketplaceItem());
    }

    @Test
    public void lendingMessageRequiresTypeItemAndRequestIds() {
        ChatMessage card = new ChatMessage(
                "lending_request_request-one",
                "borrower",
                "Lending request sent",
                ChatMessage.TYPE_LENDING_REQUEST,
                "item-one",
                "request-one",
                1L,
                false);

        assertTrue(card.isLendingRequest());
        assertFalse(card.isMarketplaceItem());
    }
}
