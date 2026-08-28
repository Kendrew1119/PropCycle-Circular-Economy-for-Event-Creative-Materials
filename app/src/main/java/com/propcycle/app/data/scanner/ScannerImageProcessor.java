package com.propcycle.app.data.scanner;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Converts one camera or Photo Picker image into a bounded, orientation-correct JPEG.
 *
 * <p>The output is written into app-private cache. Re-encoding intentionally removes EXIF, GPS,
 * XMP, and other source metadata before bytes are sent for analysis.</p>
 */
public final class ScannerImageProcessor implements Closeable {

    public static final String OUTPUT_MIME_TYPE = "image/jpeg";
    public static final int MAX_LONGEST_EDGE_PIXELS =
            ScannerImageLimits.MAX_LONGEST_EDGE_PIXELS;
    public static final int MAX_ENCODED_BYTES = ScannerImageLimits.MAX_ENCODED_BYTES;

    private static final String CACHE_DIRECTORY_NAME = "scanner";
    private static final String CAPTURE_PREFIX = "capture_";
    private static final String IMPORT_PREFIX = "import_";
    private static final String OUTPUT_PREFIX = "processed_";
    private static final String JPEG_SUFFIX = ".jpg";
    private static final long STALE_FILE_AGE_MILLIS = 24L * 60L * 60L * 1_000L;

    private final ContentResolver contentResolver;
    private final File scannerCacheDirectory;
    private final ExecutorService executor;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Set<CompletableFuture<ProcessedImage>> pendingFutures =
            Collections.synchronizedSet(new HashSet<>());

    public ScannerImageProcessor(@NonNull Context context) {
        Context applicationContext = Objects.requireNonNull(context, "context")
                .getApplicationContext();
        contentResolver = applicationContext.getContentResolver();
        scannerCacheDirectory = new File(
                applicationContext.getCacheDir(), CACHE_DIRECTORY_NAME);
        executor = Executors.newSingleThreadExecutor(new ScannerThreadFactory());
        executor.execute(this::deleteStaleFiles);
    }

    /** Creates the app-private temporary target that CameraX should write into. */
    @NonNull
    public File createCaptureFile() throws ImageProcessingException {
        ensureOpen();
        ensureCacheDirectory();
        try {
            return File.createTempFile(CAPTURE_PREFIX, JPEG_SUFFIX, scannerCacheDirectory);
        } catch (IOException error) {
            throw new ImageProcessingException(
                    ImageFailureKind.IO,
                    "A temporary camera file could not be created.",
                    error);
        }
    }

    /** Processes a Photo Picker URI. PropCycle never deletes or persists the provider's URI. */
    @NonNull
    public CompletableFuture<ProcessedImage> process(@NonNull Uri sourceUri) {
        Objects.requireNonNull(sourceUri, "sourceUri");
        return submit(new UriSource(sourceUri), null);
    }

    /**
     * Processes an app-private camera file and deletes that source file when processing ends.
     * The file must have been created inside this processor's scanner cache directory.
     */
    @NonNull
    public CompletableFuture<ProcessedImage> process(@NonNull File cameraFile) {
        Objects.requireNonNull(cameraFile, "cameraFile");
        try {
            requireOwnedScannerFile(cameraFile);
        } catch (ImageProcessingException error) {
            return failedFuture(error);
        }
        return submit(new FileSource(cameraFile), cameraFile);
    }

    @NonNull
    private CompletableFuture<ProcessedImage> submit(
            @NonNull Source source,
            @Nullable File ownedSourceToDelete) {
        if (closed.get()) {
            // A CameraX target is owned by this processor once process(File) is called. A stale
            // capture callback may arrive after the ViewModel has closed us, so it must not leave
            // that raw image in cache merely because no worker can accept the job.
            deleteQuietly(ownedSourceToDelete);
            return failedFuture(new ImageProcessingException(
                    ImageFailureKind.CANCELED,
                    "Image processing has already stopped."));
        }
        CompletableFuture<ProcessedImage> result = new CompletableFuture<>();
        AtomicReference<Future<?>> runnerReference = new AtomicReference<>();
        pendingFutures.add(result);
        result.whenComplete((ignoredImage, ignoredError) -> {
            pendingFutures.remove(result);
            if (result.isCancelled()) {
                Future<?> runner = runnerReference.get();
                if (runner != null) {
                    runner.cancel(true);
                }
            }
            if (ownedSourceToDelete != null) {
                deleteQuietly(ownedSourceToDelete);
            }
        });
        try {
            Future<?> runner = executor.submit(() -> {
                ProcessedImage processedImage = null;
                try {
                    processedImage = processOnWorker(source);
                    if (!result.complete(processedImage)) {
                        processedImage.delete();
                    }
                } catch (Throwable error) {
                    result.completeExceptionally(error);
                    if (processedImage != null) {
                        processedImage.delete();
                    }
                }
            });
            runnerReference.set(runner);
            // Cancellation may have won the race before submit() returned its Future.
            if (result.isCancelled()) {
                runner.cancel(true);
            }
        } catch (RejectedExecutionException rejected) {
            result.completeExceptionally(new ImageProcessingException(
                    ImageFailureKind.CANCELED,
                    "Image processing has already stopped.",
                    rejected));
        }
        return result;
    }

    @NonNull
    private ProcessedImage processOnWorker(@NonNull Source source)
            throws ImageProcessingException {
        throwIfInterrupted();
        validateSource(source);

        Source stableSource = source;
        File importedSource = null;
        if (source instanceof UriSource) {
            importedSource = copyUriIntoBoundedCache((UriSource) source);
            stableSource = new FileSource(importedSource, source.mimeType());
        }

        try {
            return decodeAndNormalize(stableSource);
        } finally {
            deleteQuietly(importedSource);
        }
    }

    @NonNull
    private ProcessedImage decodeAndNormalize(@NonNull Source source)
            throws ImageProcessingException {
        throwIfInterrupted();

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream stream = source.open()) {
            BitmapFactory.decodeStream(stream, null, bounds);
        } catch (IOException error) {
            throw ioFailure(error);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new ImageProcessingException(
                    ImageFailureKind.UNSUPPORTED,
                    "Select a valid JPEG, PNG, WebP, HEIF, or AVIF image.");
        }
        try {
            ScannerImageLimits.validateSourceDimensions(bounds.outWidth, bounds.outHeight);
        } catch (IllegalArgumentException error) {
            throw invalidImage(error.getMessage(), error);
        }

        int rotationDegrees = 0;
        boolean flipped = false;
        try (InputStream stream = source.open()) {
            ExifInterface exif = new ExifInterface(stream);
            rotationDegrees = exif.getRotationDegrees();
            flipped = exif.isFlipped();
        } catch (IOException | RuntimeException ignoredExif) {
            // Missing or unreadable EXIF is equivalent to normal orientation. Decode still
            // validates the image itself; no source metadata is copied to the output.
        }

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inSampleSize = ScannerImageLimits.calculateInSampleSize(
                bounds.outWidth, bounds.outHeight);
        decodeOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

        final Bitmap decoded;
        try (InputStream stream = source.open()) {
            decoded = BitmapFactory.decodeStream(stream, null, decodeOptions);
        } catch (IOException error) {
            throw ioFailure(error);
        } catch (OutOfMemoryError memoryError) {
            throw new ImageProcessingException(
                    ImageFailureKind.TOO_LARGE,
                    "This image is too large to prepare safely. Choose a smaller image.",
                    memoryError);
        }
        if (decoded == null) {
            throw new ImageProcessingException(
                    ImageFailureKind.UNSUPPORTED,
                    "This image format could not be decoded.");
        }

        Bitmap working = decoded;
        File outputFile = null;
        try {
            throwIfInterrupted();
            working = orientBitmap(working, flipped, rotationDegrees);
            working = scaleWithinLimit(working, MAX_LONGEST_EDGE_PIXELS);
            throwIfInterrupted();

            EncodedJpeg encoded = encodeBoundedJpeg(working);
            ScannerImageLimits.validateEncodedBytes(encoded.bytes.length);
            ensureCacheDirectory();
            outputFile = File.createTempFile(
                    OUTPUT_PREFIX, JPEG_SUFFIX, scannerCacheDirectory);
            try (FileOutputStream output = new FileOutputStream(outputFile)) {
                output.write(encoded.bytes);
                output.flush();
            }
            throwIfInterrupted();
            return new ProcessedImage(
                    outputFile,
                    encoded.bytes,
                    encoded.width,
                    encoded.height);
        } catch (ImageProcessingException error) {
            deleteQuietly(outputFile);
            throw error;
        } catch (IOException error) {
            deleteQuietly(outputFile);
            throw ioFailure(error);
        } catch (IllegalArgumentException error) {
            deleteQuietly(outputFile);
            throw invalidImage(error.getMessage(), error);
        } finally {
            if (!working.isRecycled()) {
                working.recycle();
            }
            if (working != decoded && !decoded.isRecycled()) {
                decoded.recycle();
            }
        }
    }

    @NonNull
    private File copyUriIntoBoundedCache(@NonNull UriSource source)
            throws ImageProcessingException {
        ensureCacheDirectory();
        File importedFile = null;
        try {
            importedFile = File.createTempFile(
                    IMPORT_PREFIX, ".tmp", scannerCacheDirectory);
            long totalBytes = 0L;
            byte[] buffer = new byte[64 * 1024];
            try (InputStream input = source.open();
                 FileOutputStream output = new FileOutputStream(importedFile)) {
                int read;
                while ((read = input.read(buffer)) != -1) {
                    throwIfInterrupted();
                    totalBytes += read;
                    if (totalBytes > ScannerImageLimits.MAX_SOURCE_BYTES) {
                        throw new ImageProcessingException(
                                ImageFailureKind.TOO_LARGE,
                                "Select an image smaller than 32 MB.");
                    }
                    output.write(buffer, 0, read);
                }
                output.flush();
            }
            try {
                ScannerImageLimits.validateKnownSourceBytes(totalBytes);
            } catch (IllegalArgumentException error) {
                throw invalidImage(error.getMessage(), error);
            }
            return importedFile;
        } catch (ImageProcessingException error) {
            deleteQuietly(importedFile);
            throw error;
        } catch (IOException | SecurityException error) {
            deleteQuietly(importedFile);
            throw ioFailure(error);
        }
    }

    private void validateSource(@NonNull Source source) throws ImageProcessingException {
        String mimeType = source.mimeType();
        if (mimeType != null && !mimeType.toLowerCase(java.util.Locale.ROOT)
                .startsWith("image/")) {
            throw new ImageProcessingException(
                    ImageFailureKind.UNSUPPORTED,
                    "Select an image file, not another type of file.");
        }
        try {
            ScannerImageLimits.validateKnownSourceBytes(source.knownLength());
        } catch (IllegalArgumentException error) {
            throw invalidImage(error.getMessage(), error);
        }
    }

    @NonNull
    private static Bitmap orientBitmap(
            @NonNull Bitmap source,
            boolean flipped,
            int clockwiseRotationDegrees) {
        if (!flipped && clockwiseRotationDegrees == 0) {
            return source;
        }
        Matrix matrix = new Matrix();
        // ExifInterface documents rotation as occurring after a horizontal flip.
        if (flipped) {
            matrix.postScale(-1f, 1f);
        }
        if (clockwiseRotationDegrees != 0) {
            matrix.postRotate(clockwiseRotationDegrees);
        }
        Bitmap oriented = Bitmap.createBitmap(
                source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        if (oriented != source) {
            source.recycle();
        }
        return oriented;
    }

    @NonNull
    private static Bitmap scaleWithinLimit(@NonNull Bitmap source, int longestEdge) {
        int[] dimensions = ScannerImageLimits.fitWithinLongestEdge(
                source.getWidth(), source.getHeight(), longestEdge);
        if (dimensions[0] == source.getWidth() && dimensions[1] == source.getHeight()) {
            return source;
        }
        Bitmap scaled = Bitmap.createScaledBitmap(
                source, dimensions[0], dimensions[1], true);
        if (scaled != source) {
            source.recycle();
        }
        return scaled;
    }

    @NonNull
    private static EncodedJpeg encodeBoundedJpeg(@NonNull Bitmap initialBitmap)
            throws ImageProcessingException {
        Bitmap working = initialBitmap;
        try {
            while (true) {
                throwIfInterrupted();
                for (int quality = 88; quality >= 48; quality -= 8) {
                    throwIfInterrupted();
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream(512 * 1024);
                    if (!working.compress(Bitmap.CompressFormat.JPEG, quality, bytes)) {
                        throw new ImageProcessingException(
                                ImageFailureKind.IO,
                                "The image could not be converted to JPEG.");
                    }
                    if (bytes.size() <= MAX_ENCODED_BYTES) {
                        return new EncodedJpeg(
                                bytes.toByteArray(), working.getWidth(), working.getHeight());
                    }
                }

                int longestEdge = Math.max(working.getWidth(), working.getHeight());
                if (longestEdge <= ScannerImageLimits.MIN_EDGE_PIXELS) {
                    throw new ImageProcessingException(
                            ImageFailureKind.TOO_LARGE,
                            "The prepared image is still larger than 4 MB.");
                }
                int nextLongestEdge = Math.max(
                        ScannerImageLimits.MIN_EDGE_PIXELS,
                        (int) Math.floor(longestEdge * 0.8d));
                Bitmap smaller = scaleWithinLimit(working, nextLongestEdge);
                if (smaller != working) {
                    if (working != initialBitmap && !working.isRecycled()) {
                        working.recycle();
                    }
                    working = smaller;
                }
            }
        } finally {
            if (working != initialBitmap && !working.isRecycled()) {
                working.recycle();
            }
        }
    }

    private static final class EncodedJpeg {
        private final byte[] bytes;
        private final int width;
        private final int height;

        private EncodedJpeg(@NonNull byte[] bytes, int width, int height) {
            this.bytes = bytes;
            this.width = width;
            this.height = height;
        }
    }

    private void requireOwnedScannerFile(@NonNull File file) throws ImageProcessingException {
        try {
            String cachePath = scannerCacheDirectory.getCanonicalPath() + File.separator;
            String filePath = file.getCanonicalPath();
            if (!filePath.startsWith(cachePath)) {
                throw new ImageProcessingException(
                        ImageFailureKind.INVALID,
                        "The camera file is outside PropCycle's private scanner cache.");
            }
        } catch (IOException error) {
            throw ioFailure(error);
        }
    }

    private void ensureCacheDirectory() throws ImageProcessingException {
        if ((!scannerCacheDirectory.exists() && !scannerCacheDirectory.mkdirs())
                || !scannerCacheDirectory.isDirectory()) {
            throw new ImageProcessingException(
                    ImageFailureKind.IO,
                    "PropCycle's temporary scanner folder is unavailable.");
        }
    }

    private void ensureOpen() throws ImageProcessingException {
        if (closed.get()) {
            throw new ImageProcessingException(
                    ImageFailureKind.CANCELED,
                    "Image processing has already stopped.");
        }
    }

    private static void throwIfInterrupted() throws ImageProcessingException {
        if (Thread.currentThread().isInterrupted()) {
            throw new ImageProcessingException(
                    ImageFailureKind.CANCELED,
                    "Image processing was canceled.");
        }
    }

    private void deleteStaleFiles() {
        File[] files = scannerCacheDirectory.listFiles();
        if (files == null) {
            return;
        }
        long cutoff = System.currentTimeMillis() - STALE_FILE_AGE_MILLIS;
        for (File file : files) {
            if (file.isFile()
                    && file.lastModified() < cutoff
                    && (file.getName().startsWith(CAPTURE_PREFIX)
                    || file.getName().startsWith(IMPORT_PREFIX)
                    || file.getName().startsWith(OUTPUT_PREFIX))) {
                deleteQuietly(file);
            }
        }
    }

    public static void deleteQuietly(@Nullable File file) {
        if (file != null && file.exists()) {
            // App-private cache deletion is best effort. Android may also clear cache itself.
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    public static void deleteQuietly(@Nullable ProcessedImage image) {
        if (image != null) {
            image.delete();
        }
    }

    /** Resolves only a processed image inside PropCycle's private scanner cache. */
    @Nullable
    public static File resolveTransferredImage(
            @NonNull Context context,
            @Nullable String absolutePath) {
        if (absolutePath == null || absolutePath.trim().isEmpty()) {
            return null;
        }
        File scannerDirectory = new File(
                context.getApplicationContext().getCacheDir(), CACHE_DIRECTORY_NAME);
        File candidate = new File(absolutePath);
        try {
            String expectedParent = scannerDirectory.getCanonicalPath() + File.separator;
            String candidatePath = candidate.getCanonicalPath();
            return candidatePath.startsWith(expectedParent)
                    && candidate.getName().startsWith(OUTPUT_PREFIX)
                    && candidate.getName().endsWith(JPEG_SUFFIX)
                    && candidate.isFile()
                    ? candidate
                    : null;
        } catch (IOException ignored) {
            return null;
        }
    }

    /** Deletes an unconsumed scanner handoff without accepting an arbitrary file path. */
    public static void deleteTransferredImage(
            @NonNull Context context,
            @Nullable String absolutePath) {
        deleteQuietly(resolveTransferredImage(context, absolutePath));
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            List<CompletableFuture<ProcessedImage>> futures;
            synchronized (pendingFutures) {
                futures = new ArrayList<>(pendingFutures);
            }
            for (CompletableFuture<ProcessedImage> future : futures) {
                future.cancel(true);
            }
            executor.shutdownNow();
        }
    }

    @NonNull
    private static <T> CompletableFuture<T> failedFuture(@NonNull Throwable error) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(error);
        return future;
    }

    @NonNull
    private static ImageProcessingException ioFailure(@NonNull Throwable error) {
        return new ImageProcessingException(
                ImageFailureKind.IO,
                "The selected image could not be read. Choose it again.",
                error);
    }

    @NonNull
    private static ImageProcessingException invalidImage(
            @Nullable String message,
            @NonNull Throwable error) {
        return new ImageProcessingException(
                ImageFailureKind.TOO_LARGE,
                message == null ? "The selected image is not supported." : message,
                error);
    }

    private interface Source {
        @NonNull
        InputStream open() throws IOException;

        @Nullable
        String mimeType();

        /** Returns -1 when the provider cannot report a size. */
        long knownLength();
    }

    private final class UriSource implements Source {
        private final Uri uri;

        UriSource(@NonNull Uri uri) {
            this.uri = uri;
        }

        @NonNull
        @Override
        public InputStream open() throws IOException {
            InputStream stream = contentResolver.openInputStream(uri);
            if (stream == null) {
                throw new IOException("The content provider returned no image stream.");
            }
            return stream;
        }

        @Nullable
        @Override
        public String mimeType() {
            return contentResolver.getType(uri);
        }

        @Override
        public long knownLength() {
            try (AssetFileDescriptor descriptor =
                         contentResolver.openAssetFileDescriptor(uri, "r")) {
                return descriptor == null ? -1L : descriptor.getLength();
            } catch (IOException | SecurityException ignored) {
                return -1L;
            }
        }
    }

    private static final class FileSource implements Source {
        private final File file;

        FileSource(@NonNull File file) {
            this(file, OUTPUT_MIME_TYPE);
        }

        FileSource(@NonNull File file, @Nullable String mimeType) {
            this.file = file;
            this.mimeType = mimeType;
        }

        @Nullable private final String mimeType;

        @NonNull
        @Override
        public InputStream open() throws IOException {
            return new FileInputStream(file);
        }

        @Nullable
        @Override
        public String mimeType() {
            return mimeType;
        }

        @Override
        public long knownLength() {
            return file.exists() ? file.length() : 0L;
        }
    }

    /** Owned scanner bytes and file. Call {@link #delete()} as soon as the request ends. */
    public static final class ProcessedImage implements Closeable {
        private final File file;
        private final int width;
        private final int height;
        private byte[] encodedBytes;
        private boolean deleted;

        private ProcessedImage(
                @NonNull File file,
                @NonNull byte[] encodedBytes,
                int width,
                int height) {
            this.file = file;
            this.encodedBytes = encodedBytes;
            this.width = width;
            this.height = height;
        }

        @NonNull
        public File getFile() {
            return file;
        }

        @NonNull
        public synchronized byte[] copyBytes() {
            if (deleted) {
                throw new IllegalStateException("The temporary scanner image was deleted.");
            }
            return Arrays.copyOf(encodedBytes, encodedBytes.length);
        }

        @NonNull
        public String getMimeType() {
            return OUTPUT_MIME_TYPE;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public synchronized int getByteCount() {
            return deleted ? 0 : encodedBytes.length;
        }

        public synchronized boolean isDeleted() {
            return deleted;
        }

        /**
         * Transfers the cache file to the next screen and clears this object's in-memory copy.
         * The receiving screen must pass the file back through {@link ScannerImageProcessor#process(File)}
         * or delete it when the journey is abandoned.
         */
        @NonNull
        public synchronized File transferFileOwnership() {
            if (deleted || !file.isFile()) {
                throw new IllegalStateException("The temporary scanner image is unavailable.");
            }
            Arrays.fill(encodedBytes, (byte) 0);
            encodedBytes = new byte[0];
            deleted = true;
            return file;
        }

        /** Deletes the cache file and clears the in-memory copy. Safe to call repeatedly. */
        public synchronized void delete() {
            if (deleted) {
                return;
            }
            Arrays.fill(encodedBytes, (byte) 0);
            encodedBytes = new byte[0];
            deleteQuietly(file);
            deleted = true;
        }

        @Override
        public void close() {
            delete();
        }
    }

    public enum ImageFailureKind {
        INVALID,
        UNSUPPORTED,
        TOO_LARGE,
        IO,
        CANCELED
    }

    public static final class ImageProcessingException extends RuntimeException {
        private final ImageFailureKind kind;

        public ImageProcessingException(
                @NonNull ImageFailureKind kind,
                @NonNull String message) {
            super(message);
            this.kind = kind;
        }

        public ImageProcessingException(
                @NonNull ImageFailureKind kind,
                @NonNull String message,
                @NonNull Throwable cause) {
            super(message, cause);
            this.kind = kind;
        }

        @NonNull
        public ImageFailureKind getKind() {
            return kind;
        }
    }

    private static final class ScannerThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(@NonNull Runnable runnable) {
            Thread thread = new Thread(runnable, "PropCycle-image-processor");
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        }
    }
}
