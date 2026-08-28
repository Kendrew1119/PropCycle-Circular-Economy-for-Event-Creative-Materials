package com.propcycle.app.data.activity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** One account-scoped, truthful action that can be shown offline on this device. */
@Entity(
        tableName = "activity_records",
        indices = {
                @Index(value = {"ownerUid", "occurredAt"}),
                @Index(value = {"ownerUid", "type"})
        })
public final class ActivityRecord {

    @PrimaryKey
    @NonNull
    private String id;
    @NonNull
    private String ownerUid;
    @NonNull
    private String type;
    @NonNull
    private String title;
    @NonNull
    private String detail;
    @NonNull
    private String destination;
    @NonNull
    private String payload;
    private long occurredAt;

    public ActivityRecord(
            @NonNull String id,
            @NonNull String ownerUid,
            @NonNull String type,
            @NonNull String title,
            @NonNull String detail,
            @NonNull String destination,
            @NonNull String payload,
            long occurredAt) {
        this.id = id;
        this.ownerUid = ownerUid;
        this.type = type;
        this.title = title;
        this.detail = detail;
        this.destination = destination;
        this.payload = payload;
        this.occurredAt = occurredAt;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    @NonNull public String getOwnerUid() { return ownerUid; }
    public void setOwnerUid(@NonNull String ownerUid) { this.ownerUid = ownerUid; }
    @NonNull public String getType() { return type; }
    public void setType(@NonNull String type) { this.type = type; }
    @NonNull public String getTitle() { return title; }
    public void setTitle(@NonNull String title) { this.title = title; }
    @NonNull public String getDetail() { return detail; }
    public void setDetail(@NonNull String detail) { this.detail = detail; }
    @NonNull public String getDestination() { return destination; }
    public void setDestination(@NonNull String destination) { this.destination = destination; }
    @NonNull public String getPayload() { return payload; }
    public void setPayload(@NonNull String payload) { this.payload = payload; }
    public long getOccurredAt() { return occurredAt; }
    public void setOccurredAt(long occurredAt) { this.occurredAt = occurredAt; }
}
