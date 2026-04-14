package com.lynch.versionhandle.service;

import com.lynch.versionhandle.model.Commit;
import com.lynch.versionhandle.util.HashUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CommitService {

    /**
     * Checks conditions for a snapshot to be commited then commits if valid
     * @param message user provided message describing their commit
     */
    public void commit(Path repoPath, String message) {

        Path vhPath = repoPath.resolve(".versionhandle");

        // Check project is initialised
        if(!Files.exists(vhPath)) {
            System.out.println("Not a versionhandle repository. Initialise project first.");
            return;
        }

        Map<String, String> index = new IndexService().loadIndex(repoPath);

        // Check that there are any files staged ready for a commit
        if(index.isEmpty()) {
            System.out.println("Nothing to commit.");
            return;
        }

        // Get commit data
        String parentId = readCurrent(repoPath);
        String timestamp = LocalDateTime.now().toString();
        Map<String, String> snapshot;
        if(parentId == null) {
            snapshot = new HashMap<>();
        } else {
            snapshot = new HashMap<>(loadCommit(repoPath, parentId).getSnapshot());
        }
        snapshot.putAll(index);

        // Create the content first to get the id for the actual commit, then create the commit with the created hash
        String commitContent = buildCommitContent(new Commit(null, message, timestamp, parentId, snapshot));
        String hash = HashUtil.sha256(commitContent.getBytes());
        Commit commit = new Commit(hash, message, timestamp, parentId, snapshot);

        // Finalise the commit
        saveCommit(repoPath, commit);

        new IndexService().saveIndex(repoPath, new HashMap<String, String>());
        writeCurrent(repoPath, hash);

        System.out.println("Snapshot committed: " + message);
        System.out.println("Commit hash: " + hash);

    }

    /**
     * Creates and returns a new commit object by fetching data using given commitId
     * @param repoPath project root repo
     * @param commitId commitId of the commit we want to load
     * @return commit linked to commitId
     */
    public Commit loadCommit(Path repoPath, String commitId) {
        Path commitPath = repoPath.resolve(".versionhandle").resolve("commits").resolve(commitId);

        String message;
        String timestamp;
        String parentId;
        Map<String, String> snapshot = new HashMap<>();

        try (Scanner fileScan = new Scanner(commitPath.toFile())) {
            message = fileScan.nextLine().replaceFirst("Message: ", "");
            timestamp = fileScan.nextLine().replaceFirst("Timestamp: ", "");
            parentId = fileScan.nextLine().replaceFirst("Parent: ", "");
            if(parentId.equals("null")) parentId = null;
            fileScan.nextLine();

            while(fileScan.hasNextLine()) {
                String[] parts = fileScan.nextLine().split(" ");
                snapshot.put(parts[0], parts[1]);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Commit", e);
        }

        return new Commit(commitId, message, timestamp, parentId, snapshot);
    }

    /**
     * Saves commit content to commits folder
     * @param repoPath project root repo
     * @param commit given commit
     */
    public void saveCommit(Path repoPath, Commit commit) {
        Path commitPath = repoPath.resolve(".versionhandle").resolve("commits").resolve(commit.getId());

        String commitContent = buildCommitContent(commit);

        try {
            Files.write(commitPath, commitContent.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save commit.", e);
        }
    }

    public String buildCommitContent(Commit commit) {
        StringBuilder commitContent = new StringBuilder();

        commitContent.append("Message: ").append(commit.getMessage()).append("\n");
        commitContent.append("Timestamp: ").append(commit.getTimestamp()).append("\n");
        commitContent.append("Parent: ").append(commit.getParentId()).append("\n\n");

        for(Map.Entry<String, String> entry: commit.getSnapshot().entrySet()) {
            commitContent.append(entry.getKey()).append(" ").append(entry.getValue()).append("\n");
        }
        return commitContent.toString();
    }


    /**
     * Reads CURRENT commit id
     * @param repoPath project root repository
     * @return id of CURRENT or null if first commit
     */
    public String readCurrent(Path repoPath) {
        Path currentPath = repoPath.resolve(".versionhandle").resolve("CURRENT");

        try {
            if(!Files.exists(currentPath)) {
                return null;
            }

            String content = Files.readString(currentPath).trim();

            if(content.isEmpty()) {
                return null;
            }

            return content;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CURRENT", e);
        }
    }

    /**
     * Writes latest commit id to CURRENT
     * @param repoPath project root repository
     * @param hash hashed content (id)
     */
    public void writeCurrent(Path repoPath, String hash) {
        Path currentPath = repoPath.resolve(".versionhandle").resolve("CURRENT");

        try {
            Files.writeString(currentPath, hash);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update CURRENT", e);
        }
    }
}
