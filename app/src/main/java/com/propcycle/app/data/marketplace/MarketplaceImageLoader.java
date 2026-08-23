package com.propcycle.app.data.marketplace;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.firebase.storage.FirebaseStorage;
import com.propcycle.app.core.firebase.FirebaseEnvironment;

import java.io.Closeable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/** Bounded authenticated Storage download and background decode for marketplace cards. */
public final class MarketplaceImageLoader implements Closeable {

    public interface Callback {
        void onLoaded(@NonNull Bitmap bitmap);

        void onError();
    }

    public interface LoadHandle {
        void cancel();
    }

    private static final int TARGET_LONGEST_EDGE = 900;
    private static final int MEMORY_KIB = (int) Math.min(
            Integer.MAX_VALUE,
            Runtime.getRuntime().maxMemory() / 1024L);
    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<>(Math.max(4 * 1024, MEMORY_KIB / 12)) {
                @Override
                protected int sizeOf(@NonNull String key, @NonNull Bitmap bitmap) {
                    return Math.max(1, bitmap.getAllocationByteCount() / 1024);
                }
            };

    private final Context applicationContext;
    private final FirebaseStorage storage;
    private final ExecutorService decodeExecutor = Executors.newFixedThreadPool(2);
    private final Set<Request> requests = Collections.synchronizedSet(new HashSet<>());
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public MarketplaceImageLoader(@NonNull Context context) {
        applicationContext = context.getApplicationContext();
        storage = FirebaseEnvironment.storage(applicationContext);
    }

    @NonNull
    public LoadHandle load(@NonNull String gsUrl, @NonNull Callback callback) {
        if (closed.get() || storage == null
                || !MarketplaceImagePolicy.isGsUrlForBucket(
                        gsUrl, storage.getReference().getBucket())) {
            callback.onError();
            return () -> {
            };
        }
        Bitmap cached = CACHE.get(gsUrl);
        if (cached != null && !cached.isRecycled()) {
            callback.onLoaded(cached);
            return () -> {
            };
        }

        Request request = new Request();
        requests.add(request);
        try {
            storage.getReferenceFromUrl(gsUrl)
                    .getBytes(MarketplaceImagePolicy.MAX_ENCODED_BYTES)
                    .addOnSuccessListener(bytes -> decode(gsUrl, bytes, request, callback))
                    .addOnFailureListener(error -> finishError(request, callback));
        } catch (IllegalArgumentException error) {
            finishError(request, callback);
        }
        return request::cancel;
    }

    private void decode(
            @NonNull String gsUrl,
            @NonNull byte[] bytes,
            @NonNull Request request,
            @NonNull Callback callback) {
        if (closed.get() || request.canceled.get()) {
            requests.remove(request);
            return;
        }
        try {
            decodeExecutor.execute(() -> {
                Bitmap bitmap = decodeBounded(bytes);
                if (bitmap != null) {
                    CACHE.put(gsUrl, bitmap);
                }
                ContextCompat.getMainExecutor(applicationContext).execute(() -> {
                    requests.remove(request);
                    if (request.canceled.get() || closed.get()) {
                        return;
                    }
                    if (bitmap == null) {
                        callback.onError();
                    } else {
                        callback.onLoaded(bitmap);
                    }
                });
            });
        } catch (RejectedExecutionException error) {
            finishError(request, callback);
        }
    }

    private void finishError(@NonNull Request request, @NonNull Callback callback) {
        requests.remove(request);
        if (!request.canceled.get() && !closed.get()) {
            callback.onError();
        }
    }

    private static Bitmap decodeBounded(@NonNull byte[] bytes) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }
        int longest = Math.max(bounds.outWidth, bounds.outHeight);
        int sample = 1;
        while ((longest + sample - 1) / sample > TARGET_LONGEST_EDGE) {
            sample *= 2;
        }
        BitmapFactory.Options decode = new BitmapFactory.Options();
        decode.inSampleSize = sample;
        decode.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, decode);
        } catch (OutOfMemoryError error) {
            return null;
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        synchronized (requests) {
            for (Request request : requests) {
                request.cancel();
            }
            requests.clear();
        }
        decodeExecutor.shutdownNow();
    }

    private static final class Request {
        private final AtomicBoolean canceled = new AtomicBoolean(false);

        private void cancel() {
            canceled.set(true);
        }
    }
}
