package com.propcycle.app.data.scanner;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public final class ScannerImageLimitsTest {

    @Test
    public void sourceDimensions_acceptSupportedBoundaries() {
        ScannerImageLimits.validateSourceDimensions(32, 32);
        ScannerImageLimits.validateSourceDimensions(12_000, 10_000);
    }

    @Test
    public void sourceDimensions_rejectTinyHugeOrExcessPixelImages() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ScannerImageLimits.validateSourceDimensions(31, 100));
        assertThrows(
                IllegalArgumentException.class,
                () -> ScannerImageLimits.validateSourceDimensions(32_769, 100));
        assertThrows(
                IllegalArgumentException.class,
                () -> ScannerImageLimits.validateSourceDimensions(12_001, 10_000));
    }

    @Test
    public void knownSourceBytes_allowUnknownAndBoundaryButRejectEmptyOrHuge() {
        ScannerImageLimits.validateKnownSourceBytes(-1L);
        ScannerImageLimits.validateKnownSourceBytes(ScannerImageLimits.MAX_SOURCE_BYTES);

        assertThrows(
                IllegalArgumentException.class,
                () -> ScannerImageLimits.validateKnownSourceBytes(0L));
        assertThrows(
                IllegalArgumentException.class,
                () -> ScannerImageLimits.validateKnownSourceBytes(
                        ScannerImageLimits.MAX_SOURCE_BYTES + 1L));
    }

    @Test
    public void encodedBytes_enforceFourMiBBound() {
        ScannerImageLimits.validateEncodedBytes(ScannerImageLimits.MAX_ENCODED_BYTES);

        assertThrows(
                IllegalArgumentException.class,
                () -> ScannerImageLimits.validateEncodedBytes(0));
        assertThrows(
                IllegalArgumentException.class,
                () -> ScannerImageLimits.validateEncodedBytes(
                        ScannerImageLimits.MAX_ENCODED_BYTES + 1));
    }

    @Test
    public void calculateInSampleSize_keepsDecodedLongestEdgeAtOrBelowLimit() {
        assertEquals(1, ScannerImageLimits.calculateInSampleSize(1_600, 900));
        assertEquals(2, ScannerImageLimits.calculateInSampleSize(1_601, 900));
        assertEquals(2, ScannerImageLimits.calculateInSampleSize(3_200, 900));
        assertEquals(4, ScannerImageLimits.calculateInSampleSize(3_201, 900));
    }

    @Test
    public void fitWithinLongestEdge_preservesAspectAndDoesNotUpscale() {
        assertArrayEquals(
                new int[]{1_600, 800},
                ScannerImageLimits.fitWithinLongestEdge(4_000, 2_000, 1_600));
        assertArrayEquals(
                new int[]{800, 1_600},
                ScannerImageLimits.fitWithinLongestEdge(2_000, 4_000, 1_600));
        assertArrayEquals(
                new int[]{700, 400},
                ScannerImageLimits.fitWithinLongestEdge(700, 400, 1_600));
    }
}
