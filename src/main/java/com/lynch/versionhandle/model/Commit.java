package com.lynch.versionhandle.model;

import java.util.Map;

public class Commit {

    private String id;
    private String message;
    private String timestamp;
    private String parent;
    Map<String, String> snapshot;

    public Commit(String id, String message, String timestamp, String parent, Map<String, String> snapshot) {
        this.id = id;
        this.message = message;
        this.timestamp = timestamp;
        this.parent = parent;
        this.snapshot = snapshot;
    }
}