package com.propcycle.app.data.lending;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

/** Firestore representation of one public lending item. */
public final class LendingItem {

    private String id;
    private String ownerId;
    private String title;
    private String titleNormalized;
    private String description;
    private String category;
    private String condition;
    private String pickupMethod;
    private String areaLabel;
    private Long maxBorrowDays;
    private Long depositMinor;
    private Double latitude;
    private Double longitude;
    private String imageUrl;
    private String demoImageKey;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /** Required by Firestore's Java object mapper. */
    public LendingItem() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTitleNormalized() { return titleNormalized; }
    public void setTitleNormalized(String titleNormalized) { this.titleNormalized = titleNormalized; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getPickupMethod() { return pickupMethod; }
    public void setPickupMethod(String pickupMethod) { this.pickupMethod = pickupMethod; }
    public String getAreaLabel() { return areaLabel; }
    public void setAreaLabel(String areaLabel) { this.areaLabel = areaLabel; }
    @Nullable public Long getMaxBorrowDays() { return maxBorrowDays; }
    public void setMaxBorrowDays(@Nullable Long maxBorrowDays) { this.maxBorrowDays = maxBorrowDays; }
    @Nullable public Long getDepositMinor() { return depositMinor; }
    public void setDepositMinor(@Nullable Long depositMinor) { this.depositMinor = depositMinor; }
    @Nullable public Double getLatitude() { return latitude; }
    public void setLatitude(@Nullable Double latitude) { this.latitude = latitude; }
    @Nullable public Double getLongitude() { return longitude; }
    public void setLongitude(@Nullable Double longitude) { this.longitude = longitude; }
    @Nullable public String getImageUrl() { return imageUrl; }
    public void setImageUrl(@Nullable String imageUrl) { this.imageUrl = imageUrl; }
    @Nullable public String getDemoImageKey() { return demoImageKey; }
    public void setDemoImageKey(@Nullable String demoImageKey) { this.demoImageKey = demoImageKey; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    @Nullable public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(@Nullable Timestamp createdAt) { this.createdAt = createdAt; }
    @Nullable public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(@Nullable Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public boolean hasApproximateLocation() {
        return latitude != null && longitude != null;
    }
}
