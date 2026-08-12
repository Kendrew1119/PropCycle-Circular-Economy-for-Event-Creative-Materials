package com.propcycle.app.data.scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ScanAnalysisTest {

    private static final String VALID_JSON = "{"
            + "\"itemName\":\"PET plastic bottle\","
            + "\"material\":\"PET plastic (code 1)\","
            + "\"category\":\"RECYCLABLE\","
            + "\"isRecyclable\":true,"
            + "\"uncalibratedModelEstimatePercent\":87,"
            + "\"recyclingGuidance\":\"Empty and rinse it, then check local acceptance.\","
            + "\"upcyclingIdeas\":[\"Use it as a small planter.\",\"Make a watering tool.\"],"
            + "\"environmentalNote\":\"Reuse can keep a usable item in service longer.\","
            + "\"safetyNote\":\"Do not reuse it for hot food or drink.\""
            + "}";

    @Test
    public void fromJson_validObject_parsesAllBoundedFields() {
        ScanAnalysis result = ScanAnalysis.fromJson(VALID_JSON);

        assertEquals("PET plastic bottle", result.getItemName());
        assertEquals("PET plastic (code 1)", result.getMaterial());
        assertEquals(ScanAnalysis.Category.RECYCLABLE, result.getCategory());
        assertTrue(result.isRecyclable());
        assertEquals(87, result.getUncalibratedModelEstimatePercent());
        assertEquals(2, result.getUpcyclingIdeas().size());
        assertTrue(ScanAnalysis.MALAYSIA_DISCLAIMER.contains("Malaysia"));
        assertTrue(ScanAnalysis.MALAYSIA_DISCLAIMER.contains("local council"));
    }

    @Test
    public void toJson_roundTrip_preservesValidatedResult() {
        ScanAnalysis first = ScanAnalysis.fromJson(VALID_JSON);

        ScanAnalysis roundTrip = ScanAnalysis.fromJson(first.toJson());

        assertEquals(first, roundTrip);
        assertEquals(first.hashCode(), roundTrip.hashCode());
    }

    @Test
    public void constructor_normalizesWhitespaceAndCopiesIdeas() {
        List<String> ideas = new ArrayList<>(Arrays.asList("  Make\tstorage.  "));
        ScanAnalysis result = new ScanAnalysis(
                "  Glass\n jar ",
                " Glass ",
                ScanAnalysis.Category.REUSABLE,
                true,
                60,
                "  Check   the local collector. ",
                ideas,
                " Reuse may extend its useful life. ",
                " Inspect for cracks. ");
        ideas.set(0, "Changed later");

        assertEquals("Glass jar", result.getItemName());
        assertEquals("Check the local collector.", result.getRecyclingGuidance());
        assertEquals("Make storage.", result.getUpcyclingIdeas().get(0));
        assertThrows(
                UnsupportedOperationException.class,
                () -> result.getUpcyclingIdeas().add("Not allowed"));
    }

    @Test
    public void fromJson_rejectsMissingAndUnexpectedFields() {
        assertInvalid(VALID_JSON.replace(
                ",\"safetyNote\":\"Do not reuse it for hot food or drink.\"", ""));
        assertInvalid(VALID_JSON.replaceFirst(
                "\\{", "{\"unexpected\":\"value\","));
    }

    @Test
    public void fromJson_rejectsWrongPrimitiveTypesAndDecimalEstimate() {
        assertInvalid(VALID_JSON.replace("\"isRecyclable\":true", "\"isRecyclable\":\"yes\""));
        assertInvalid(VALID_JSON.replace(
                "\"uncalibratedModelEstimatePercent\":87",
                "\"uncalibratedModelEstimatePercent\":87.5"));
        assertInvalid(VALID_JSON.replace(
                "\"upcyclingIdeas\":[\"Use it as a small planter.\",\"Make a watering tool.\"]",
                "\"upcyclingIdeas\":[4]"));
    }

    @Test
    public void fromJson_acceptsExactFirebaseSchemaBooleanEnumStrings() {
        ScanAnalysis recyclable = ScanAnalysis.fromJson(
                VALID_JSON.replace("\"isRecyclable\":true", "\"isRecyclable\":\"true\""));
        ScanAnalysis notRecyclable = ScanAnalysis.fromJson(
                VALID_JSON.replace("\"isRecyclable\":true", "\"isRecyclable\":\"false\""));

        assertTrue(recyclable.isRecyclable());
        assertFalse(notRecyclable.isRecyclable());
    }

    @Test
    public void fromJson_rejectsOutOfRangeEstimateAndUnknownCategory() {
        assertInvalid(VALID_JSON.replace(
                "\"uncalibratedModelEstimatePercent\":87",
                "\"uncalibratedModelEstimatePercent\":101"));
        assertInvalid(VALID_JSON.replace("RECYCLABLE", "MAGIC_WASTE"));
    }

    @Test
    public void fromJson_rejectsEmptyAndOverlongText() {
        assertInvalid(VALID_JSON.replace("PET plastic bottle", "   "));
        assertInvalid(VALID_JSON.replace(
                "PET plastic bottle",
                repeat('x', ScanAnalysis.MAX_ITEM_NAME_CHARACTERS + 1)));
    }

    @Test
    public void fromJson_rejectsWebLinksInEveryFreeTextShape() {
        assertInvalid(VALID_JSON.replace(
                "PET plastic bottle", "See HTTPS://example.test/item"));
        assertInvalid(VALID_JSON.replace(
                "Empty and rinse it, then check local acceptance.",
                "Open http://example.test for disposal instructions."));
        assertInvalid(VALID_JSON.replace(
                "Use it as a small planter.", "Visit www.example.test first."));
        assertInvalid(VALID_JSON.replace(
                "Reuse can keep a usable item in service longer.",
                "Details are at https://example.test."));
        assertInvalid(VALID_JSON.replace(
                "Do not reuse it for hot food or drink.",
                "Follow HTTP://example.test/safety."));
    }

    @Test
    public void fromJson_rejectsIdeaCountOutsideBounds() {
        assertInvalid(VALID_JSON.replace(
                "[\"Use it as a small planter.\",\"Make a watering tool.\"]",
                "[]"));
        assertInvalid(VALID_JSON.replace(
                "[\"Use it as a small planter.\",\"Make a watering tool.\"]",
                "[\"One\",\"Two\",\"Three\",\"Four\"]"));
    }

    @Test
    public void fromJson_rejectsNonObjectTrailingOrOversizedPayload() {
        assertInvalid("[]");
        assertInvalid(VALID_JSON + " garbage");
        assertInvalid(repeat('x', ScanAnalysis.MAX_JSON_CHARACTERS + 1));
    }

    @Test
    public void categoryLabels_areReviewFriendly() {
        assertEquals("E-waste", ScanAnalysis.Category.E_WASTE.getDisplayName());
        assertEquals("Unknown", ScanAnalysis.Category.UNKNOWN.getDisplayName());
        assertFalse(ScanAnalysis.MALAYSIA_DISCLAIMER.isEmpty());
    }

    private static void assertInvalid(String json) {
        assertThrows(ScanAnalysis.ValidationException.class, () -> ScanAnalysis.fromJson(json));
    }

    private static String repeat(char value, int count) {
        char[] characters = new char[count];
        Arrays.fill(characters, value);
        return new String(characters);
    }
}
