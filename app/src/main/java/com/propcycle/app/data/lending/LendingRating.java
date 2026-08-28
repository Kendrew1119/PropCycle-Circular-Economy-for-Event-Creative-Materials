package com.propcycle.app.data.lending;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

/** Immutable participant rating created after a confirmed return. */
public final class LendingRating {

    private String id;
    private String requestId;
    private String itemId;
    private String raterUid;
    private String recipientUid;
    private Long score;
    private String comment;
    private Timestamp createdAt;

    public LendingRating() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getRaterUid() { return raterUid; }
    public void setRaterUid(String raterUid) { this.raterUid = raterUid; }
    public String getRecipientUid() { return recipientUid; }
    public void setRecipientUid(String recipientUid) { this.recipientUid = recipientUid; }
    @Nullable public Long getScore() { return score; }
    public void setScore(@Nullable Long score) { this.score = score; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    @Nullable public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(@Nullable Timestamp createdAt) { this.createdAt = createdAt; }
}
