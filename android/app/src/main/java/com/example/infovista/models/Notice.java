package com.example.infovista.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Notice {
    @SerializedName("_id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("priority")
    private String priority;

    @SerializedName("category")
    private String category;

    @SerializedName("mediaType")
    private String mediaType;

    @SerializedName("mediaUrl")
    private String mediaUrl;

    @SerializedName("scheduledAt")
    private String scheduledAt;

    @SerializedName("expiresAt")
    private String expiresAt;

    @SerializedName("isActive")
    private boolean isActive;

    @SerializedName("displayDuration")
    private int displayDuration;

    @SerializedName("targetDevices")
    private List<String> targetDevices;

    @SerializedName("createdBy")
    private User createdBy;

    @SerializedName("viewCount")
    private int viewCount;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    // Constructors
    public Notice() {
        this.priority = "medium";
        this.category = "general";
        this.mediaType = "text";
        this.isActive = true;
        this.displayDuration = 10;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public String getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(String scheduledAt) { this.scheduledAt = scheduledAt; }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getDisplayDuration() { return displayDuration; }
    public void setDisplayDuration(int displayDuration) { this.displayDuration = displayDuration; }

    public List<String> getTargetDevices() { return targetDevices; }
    public void setTargetDevices(List<String> targetDevices) { this.targetDevices = targetDevices; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
