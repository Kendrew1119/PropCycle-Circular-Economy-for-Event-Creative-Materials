package com.propcycle.app.data.marketplace;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

/** Firestore representation of one reviewer's marketplace rating for one seller. */
public final class MarketplaceSellerRating {

    private String raterUid;
    private String recipientUid;
    private String contextListingId;
    private Long score;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /** Required by Firestore's Java object mapper. */
    public MarketplaceSellerRating() {
    }

    @Nullable public String getRaterUid() { return raterUid; }
    public void setRaterUid(@Nullable String raterUid) { this.raterUid = raterUid; }
    @Nullable public String getRecipientUid() { return recipientUid; }
    public void setRecipientUid(@Nullable String recipientUid) {
        this.recipientUid = recipientUid;
    }
    @Nullable public String getContextListingId() { return contextListingId; }
    public void setContextListingId(@Nullable String contextListingId) {
        this.contextListingId = contextListingId;
    }
    @Nullable public Long getScore() { return score; }
    public void setScore(@Nullable Long score) { this.score = score; }
    @Nullable public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(@Nullable Timestamp createdAt) { this.createdAt = createdAt; }
    @Nullable public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(@Nullable Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
