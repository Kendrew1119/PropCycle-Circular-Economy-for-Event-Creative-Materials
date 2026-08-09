package com.propcycle.app.data.marketplace;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

/** Firestore representation of one document in {@code marketplaceListings}. */
public final class MarketplaceListing {

    private String id;
    private String ownerId;
    private String title;
    private String titleNormalized;
    private String description;
    private String category;
    private String condition;
    private String transactionIntent;
    private String fulfilmentMethod;
    private Long priceMinor;
    private String exchangeTerms;
    private String status;
    private String imageUrl;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /** Required by Firestore's Java object mapper. */
    public MarketplaceListing() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleNormalized() {
        return titleNormalized;
    }

    public void setTitleNormalized(String titleNormalized) {
        this.titleNormalized = titleNormalized;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getTransactionIntent() {
        return transactionIntent;
    }

    public void setTransactionIntent(String transactionIntent) {
        this.transactionIntent = transactionIntent;
    }

    public String getFulfilmentMethod() {
        return fulfilmentMethod;
    }

    public void setFulfilmentMethod(String fulfilmentMethod) {
        this.fulfilmentMethod = fulfilmentMethod;
    }

    @Nullable
    public Long getPriceMinor() {
        return priceMinor;
    }

    public void setPriceMinor(@Nullable Long priceMinor) {
        this.priceMinor = priceMinor;
    }

    @Nullable
    public String getExchangeTerms() {
        return exchangeTerms;
    }

    public void setExchangeTerms(@Nullable String exchangeTerms) {
        this.exchangeTerms = exchangeTerms;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(@Nullable String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Nullable
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@Nullable Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Nullable
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(@Nullable Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
