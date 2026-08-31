package com.propcycle.app.data.marketplace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class MarketplaceListingStatusPolicyTest {

    @Test
    public void availableOwner_canEditWithdrawAndMarkSoldButNotRelistOrChat() {
        assertTrue(MarketplaceListingStatusPolicy.canEdit(true, "available"));
        assertTrue(MarketplaceListingStatusPolicy.canWithdraw(true, "available"));
        assertTrue(MarketplaceListingStatusPolicy.canMarkSold(true, "available"));
        assertFalse(MarketplaceListingStatusPolicy.canRelist(true, "available"));
        assertFalse(MarketplaceListingStatusPolicy.canContactSeller(true, "available"));
        assertEquals(
                "Withdraw",
                MarketplaceListingStatusPolicy.ownerStatusActionLabel("available"));
    }

    @Test
    public void withdrawnOwner_canEditAndRelistButNotWithdrawOrChat() {
        assertTrue(MarketplaceListingStatusPolicy.canEdit(true, "withdrawn"));
        assertFalse(MarketplaceListingStatusPolicy.canWithdraw(true, "withdrawn"));
        assertTrue(MarketplaceListingStatusPolicy.canRelist(true, "withdrawn"));
        assertFalse(MarketplaceListingStatusPolicy.canMarkSold(true, "withdrawn"));
        assertFalse(MarketplaceListingStatusPolicy.canContactSeller(true, "withdrawn"));
        assertEquals(
                "Relist",
                MarketplaceListingStatusPolicy.ownerStatusActionLabel("withdrawn"));
    }

    @Test
    public void soldListing_isTerminalAndCannotBeEditedOrRelisted() {
        assertTrue(MarketplaceListingStatusPolicy.isSupportedStatus("sold"));
        assertFalse(MarketplaceListingStatusPolicy.canEdit(true, "sold"));
        assertFalse(MarketplaceListingStatusPolicy.canWithdraw(true, "sold"));
        assertFalse(MarketplaceListingStatusPolicy.canRelist(true, "sold"));
        assertFalse(MarketplaceListingStatusPolicy.canMarkSold(true, "sold"));
        assertFalse(MarketplaceListingStatusPolicy.canContactSeller(false, "sold"));
        assertEquals("Sold", MarketplaceListingStatusPolicy.ownerStatusActionLabel("sold"));
    }

    @Test
    public void nonOwner_canContactOnlyAvailableListing() {
        assertTrue(MarketplaceListingStatusPolicy.canContactSeller(false, "available"));
        assertFalse(MarketplaceListingStatusPolicy.canContactSeller(false, "withdrawn"));
        assertFalse(MarketplaceListingStatusPolicy.canEdit(false, "available"));
        assertFalse(MarketplaceListingStatusPolicy.canWithdraw(false, "available"));
        assertFalse(MarketplaceListingStatusPolicy.canRelist(false, "withdrawn"));
        assertFalse(MarketplaceListingStatusPolicy.canMarkSold(false, "available"));
    }

    @Test
    public void unknownOrMissingStatus_neverEnablesActions() {
        assertFalse(MarketplaceListingStatusPolicy.isSupportedStatus(null));
        assertFalse(MarketplaceListingStatusPolicy.isSupportedStatus("completed"));
        assertFalse(MarketplaceListingStatusPolicy.canEdit(true, "completed"));
        assertFalse(MarketplaceListingStatusPolicy.canWithdraw(true, "completed"));
        assertFalse(MarketplaceListingStatusPolicy.canRelist(true, "completed"));
        assertFalse(MarketplaceListingStatusPolicy.canMarkSold(true, "completed"));
        assertFalse(MarketplaceListingStatusPolicy.canContactSeller(false, "completed"));
    }
}
