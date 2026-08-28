package com.propcycle.app.data.lending;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.util.List;

/** Participant-private borrow request and lifecycle state. */
public final class LendingRequest {

    private String id;
    private String itemId;
    private String itemTitle;
    private String ownerUid;
    private String borrowerUid;
    private List<String> participantIds;
    private String startDate;
    private String endDate;
    private List<String> dayKeys;
    private String status;
    private String lockToken;
    private Boolean returnReported;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public LendingRequest() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getItemTitle() { return itemTitle; }
    public void setItemTitle(String itemTitle) { this.itemTitle = itemTitle; }
    public String getOwnerUid() { return ownerUid; }
    public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }
    public String getBorrowerUid() { return borrowerUid; }
    public void setBorrowerUid(String borrowerUid) { this.borrowerUid = borrowerUid; }
    public List<String> getParticipantIds() { return participantIds; }
    public void setParticipantIds(List<String> participantIds) { this.participantIds = participantIds; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public List<String> getDayKeys() { return dayKeys; }
    public void setDayKeys(List<String> dayKeys) { this.dayKeys = dayKeys; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLockToken() { return lockToken; }
    public void setLockToken(String lockToken) { this.lockToken = lockToken; }
    @Nullable public Boolean getReturnReported() { return returnReported; }
    public void setReturnReported(@Nullable Boolean returnReported) { this.returnReported = returnReported; }
    @Nullable public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(@Nullable Timestamp createdAt) { this.createdAt = createdAt; }
    @Nullable public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(@Nullable Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public boolean isReturnReported() {
        return Boolean.TRUE.equals(returnReported);
    }
}
