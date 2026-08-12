package com.propcycle.app.data.scanner;

/** Pure-Java size calculations shared by the Android image processor and JVM tests. */
final class ScannerImageLimits {

    static final int MAX_LONGEST_EDGE_PIXELS = 1_600;
    static final int MIN_EDGE_PIXELS = 32;
    static final int MAX_SOURCE_EDGE_PIXELS = 32_768;
    static final long MAX_SOURCE_PIXELS = 120_000_000L;
    static final long MAX_SOURCE_BYTES = 32L * 1024L * 1024L;
    static final int MAX_ENCODED_BYTES = 4 * 1024 * 1024;

    private ScannerImageLimits() {
    }

    static void validateSourceDimensions(int width, int height) {
        if (width < MIN_EDGE_PIXELS || height < MIN_EDGE_PIXELS) {
            throw new IllegalArgumentException("The selected image is too small.");
        }
        if (width > MAX_SOURCE_EDGE_PIXELS || height > MAX_SOURCE_EDGE_PIXELS) {
            throw new IllegalArgumentException("The selected image dimensions are too large.");
        }
        if ((long) width * (long) height > MAX_SOURCE_PIXELS) {
            throw new IllegalArgumentException("The selected image contains too many pixels.");
        }
    }

    static void validateKnownSourceBytes(long byteCount) {
        if (byteCount == 0L) {
            throw new IllegalArgumentException("The selected image is empty.");
        }
        if (byteCount > MAX_SOURCE_BYTES) {
            throw new IllegalArgumentException("Select an image smaller than 32 MB.");
        }
    }

    static void validateEncodedBytes(int byteCount) {
        if (byteCount <= 0) {
            throw new IllegalArgumentException("The processed image is empty.");
        }
        if (byteCount > MAX_ENCODED_BYTES) {
            throw new IllegalArgumentException("The processed image is larger than 4 MB.");
        }
    }

    static int calculateInSampleSize(int width, int height) {
        validateSourceDimensions(width, height);
        int sampleSize = 1;
        int longestEdge = Math.max(width, height);
        while ((longestEdge + sampleSize - 1) / sampleSize > MAX_LONGEST_EDGE_PIXELS) {
            sampleSize *= 2;
        }
        return sampleSize;
    }

    static int[] fitWithinLongestEdge(int width, int height, int maximumLongestEdge) {
        if (width <= 0 || height <= 0 || maximumLongestEdge <= 0) {
            throw new IllegalArgumentException("Image dimensions must be positive.");
        }
        int longestEdge = Math.max(width, height);
        if (longestEdge <= maximumLongestEdge) {
            return new int[]{width, height};
        }
        double scale = (double) maximumLongestEdge / (double) longestEdge;
        return new int[]{
                Math.max(1, (int) Math.round(width * scale)),
                Math.max(1, (int) Math.round(height * scale))};
    }
}
