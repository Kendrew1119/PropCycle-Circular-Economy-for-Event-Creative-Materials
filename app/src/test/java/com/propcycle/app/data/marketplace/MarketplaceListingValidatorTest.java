package com.propcycle.app.data.marketplace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class MarketplaceListingValidatorTest {

    @Test
    public void validate_trimsAndMapsProposalLabelsToStableIds() {
        MarketplaceListingValidator.ValidationResult result =
                MarketplaceListingValidator.validate(
                        "  Reusable Backdrop  ",
                        "Decor",
                        "Like new",
                        "Donate",
                        "Pick-up",
                        "",
                        "",
                        "  Ready for another event.  ");

        assertTrue(result.isValid());
        NewMarketplaceListing listing = result.getListing();
        assertNotNull(listing);
        assertEquals("Reusable Backdrop", listing.getTitle());
        assertEquals("reusable backdrop", listing.getTitleNormalized());
        assertEquals("decoration", listing.getCategory());
        assertEquals("like_new", listing.getCondition());
        assertEquals("donation", listing.getTransactionIntent());
        assertEquals("pickup", listing.getFulfilmentMethod());
        assertEquals(0L, listing.getPriceMinor());
        assertEquals("", listing.getExchangeTerms());
        assertEquals("Ready for another event.", listing.getDescription());
    }

    @Test
    public void validate_rejectsShortTitle() {
        MarketplaceListingValidator.ValidationResult result =
                MarketplaceListingValidator.validate(
                        "ab", "Decoration", "Good", "Donation", "Pickup", "", "",
                        "Reusable item");

        assertFalse(result.isValid());
        assertEquals(
                "Enter an item name with at least 3 characters.",
                result.getErrorMessage());
    }

    @Test
    public void validate_rejectsUnknownCategory() {
        MarketplaceListingValidator.ValidationResult result =
                MarketplaceListingValidator.validate(
                        "Display panel", "Mystery", "Good", "Donation", "Pickup", "", "",
                        "Reusable item");

        assertFalse(result.isValid());
        assertEquals("Choose a valid category.", result.getErrorMessage());
    }

    @Test
    public void validate_requiresDescription() {
        MarketplaceListingValidator.ValidationResult result =
                MarketplaceListingValidator.validate(
                        "Display panel", "Decoration", "Good", "Donation", "Pickup", "", "",
                        "  ");

        assertFalse(result.isValid());
        assertEquals("Add a short description.", result.getErrorMessage());
    }

    @Test
    public void normalizeSearchText_isLocaleStableAndCollapsesWhitespace() {
        assertEquals(
                "fabric bundle",
                MarketplaceListingValidator.normalizeSearchText("  FABRIC   BUNDLE "));
    }

    @Test
    public void validate_saleConvertsRinggitToIntegerMinorUnits() {
        MarketplaceListingValidator.ValidationResult result =
                MarketplaceListingValidator.validate(
                        "Wooden stand", "Wood", "Fair", "Sale", "Meet-up", "12.50", "",
                        "Collection-ready stand");

        assertTrue(result.isValid());
        assertNotNull(result.getListing());
        assertEquals(1250L, result.getListing().getPriceMinor());
        assertEquals("meetup", result.getListing().getFulfilmentMethod());
        assertEquals("", result.getListing().getExchangeTerms());
    }

    @Test
    public void validate_saleRequiresPositivePrice() {
        MarketplaceListingValidator.ValidationResult result =
                MarketplaceListingValidator.validate(
                        "Wooden stand", "Wood", "Fair", "Sale", "Pickup", "0", "",
                        "Collection-ready stand");

        assertFalse(result.isValid());
        assertEquals("Enter a sale price greater than RM 0.00.", result.getErrorMessage());
    }

    @Test
    public void validate_exchangeRequiresTermsAndForcesZeroPrice() {
        MarketplaceListingValidator.ValidationResult missingTerms =
                MarketplaceListingValidator.validate(
                        "Fabric bundle", "Fabric", "Good", "Exchange", "Pickup", "", "",
                        "Clean fabric bundle");
        assertFalse(missingTerms.isValid());

        MarketplaceListingValidator.ValidationResult valid =
                MarketplaceListingValidator.validate(
                        "Fabric bundle", "Fabric", "Good", "Exchange", "Pickup", "99",
                        "A roll of craft paper", "Clean fabric bundle");
        assertTrue(valid.isValid());
        assertNotNull(valid.getListing());
        assertEquals(0L, valid.getListing().getPriceMinor());
        assertEquals("A roll of craft paper", valid.getListing().getExchangeTerms());
    }

    @Test
    public void validate_rejectsUnknownFulfilmentMethod() {
        MarketplaceListingValidator.ValidationResult result =
                MarketplaceListingValidator.validate(
                        "Fabric bundle", "Fabric", "Good", "Donation", "Delivery", "", "",
                        "Clean fabric bundle");

        assertFalse(result.isValid());
        assertEquals("Choose pickup or meet-up.", result.getErrorMessage());
    }
}
