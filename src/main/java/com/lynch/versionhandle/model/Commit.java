package com.lynch.versionhandle.model;

import java.util.Map;

public class Commit {

    private String id;
    private String message;
    private String timestamp;
    private String parentId;
    Map<String, String> snapshot;

    public Commit(String id, String message, String timestamp, String parentId, Map<String, String> snapshot) {
        this.id = id;
        this.message = message;
        this.timestamp = timestamp;
        this.parentId = parentId;
        this.snapshot = snapshot;
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getParentId() {
        return parentId;
    }

    public Map<String, String> getSnapshot() {
        return snapshot;
    }
}