package com.propcycle.app.data.lending;

import androidx.annotation.Nullable;

/** Validated input for one new or edited lending item. */
public final class NewLendingItem {

    private final String title;
    private final String titleNormalized;
    private final String description;
    private final String category;
    private final String condition;
    private final String pickupMethod;
    private final String areaLabel;
    private final int maxBorrowDays;
    private final long depositMinor;
    @Nullable private final Double latitude;
    @Nullable private final Double longitude;
    private final String demoImageKey;

    public NewLendingItem(
            String title,
            String titleNormalized,
            String description,
            String category,
            String condition,
            String pickupMethod,
            String areaLabel,
            int maxBorrowDays,
            long depositMinor,
            @Nullable Double latitude,
            @Nullable Double longitude) {
        this(title, titleNormalized, description, category, condition, pickupMethod, areaLabel,
                maxBorrowDays, depositMinor, latitude, longitude, "");
    }

    public NewLendingItem(
            String title,
            String titleNormalized,
            String description,
            String category,
            String condition,
            String pickupMethod,
            String areaLabel,
            int maxBorrowDays,
            long depositMinor,
            @Nullable Double latitude,
            @Nullable Double longitude,
            String demoImageKey) {
        this.title = title;
        this.titleNormalized = titleNormalized;
        this.description = description;
        this.category = category;
        this.condition = condition;
        this.pickupMethod = pickupMethod;
        this.areaLabel = areaLabel;
        this.maxBorrowDays = maxBorrowDays;
        this.depositMinor = depositMinor;
        this.latitude = latitude;
        this.longitude = longitude;
        this.demoImageKey = demoImageKey;
    }

    public String getTitle() { return title; }
    public String getTitleNormalized() { return titleNormalized; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getCondition() { return condition; }
    public String getPickupMethod() { return pickupMethod; }
    public String getAreaLabel() { return areaLabel; }
    public int getMaxBorrowDays() { return maxBorrowDays; }
    public long getDepositMinor() { return depositMinor; }
    @Nullable public Double getLatitude() { return latitude; }
    @Nullable public Double getLongitude() { return longitude; }
    public String getDemoImageKey() { return demoImageKey; }
}
