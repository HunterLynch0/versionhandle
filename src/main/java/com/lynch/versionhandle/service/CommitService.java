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

    private static final String DELETED = "<DELETED>";

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


        IndexService indexService = new IndexService();
        Map<String, String> index = indexService.loadIndex(repoPath);

        // Check that there are any files staged ready for a commit
        if(index.isEmpty()) {
            System.out.println("Nothing to commit.");
            return;
        }

        // Get commit data
        String branchName = readCurrent(repoPath);
        if(branchName == null) {
            System.out.println("Error: no current branch set.");
            return;
        }
        String parentId = readBranch(repoPath, branchName);
        String timestamp = LocalDateTime.now().toString();
        Map<String, String> snapshot;
        if(parentId == null) {
            snapshot = new HashMap<>();
        } else {
            snapshot = new HashMap<>(loadCommit(repoPath, parentId).getSnapshot());
        }

        for(Map.Entry<String, String> entry: index.entrySet()) {
            if(entry.getValue().equals(DELETED)) {
                snapshot.remove(entry.getKey());
            } else {
                snapshot.put(entry.getKey(), entry.getValue());
            }
        }

        // Create the content first to get the hash for the actual commit, then create the commit with the created commitId
        String commitContent = buildCommitContent(new Commit(null, message, timestamp, parentId, snapshot));
        String commitId = HashUtil.sha256(commitContent.getBytes());
        Commit commit = new Commit(commitId, message, timestamp, parentId, snapshot);

        // Finalise the commit
        saveCommit(repoPath, commit);

        indexService.saveIndex(repoPath, new HashMap<String, String>());
        writeBranch(repoPath, branchName, commitId);
        writeHead(repoPath, commitId);

        System.out.println("Snapshot committed: " + message);
        System.out.println("Branch: " + branchName);
        System.out.println("commitId: " + commitId);

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

    /**
     * Builds content of commit file
     * @param commit commit to build file for
     * @return formatted commit content
     */
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
     * Reads CURRENT branch name
     * @param repoPath project root repository
     * @return current branch name or null if missing
     */
    public String readCurrent(Path repoPath) {
        Path currentPath = repoPath.resolve(".versionhandle").resolve("CURRENT");

        try {
            if(!Files.exists(currentPath)) {
                return null;
            }

            String branch = Files.readString(currentPath).trim();

            if(branch.isEmpty()) {
                return null;
            }

            return branch;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CURRENT branch", e);
        }
    }

    /**
     * Writes current branch name to CURRENT
     * @param repoPath project root repository
     * @param branchName name of current branch
     */
    public void writeCurrent(Path repoPath, String branchName) {
        Path currentPath = repoPath.resolve(".versionhandle").resolve("CURRENT");

        if(branchName == null) {
            branchName = "";
        }

        try {
            Files.writeString(currentPath, branchName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update CURRENT branch", e);
        }
    }

    /**
     * Reads the latest commit id from a branch
     * @param repoPath project root repository
     * @param branchName branch name
     * @return latest commit id from branch or null if missing
     */
    public String readBranch(Path repoPath, String branchName) {
        Path branchPath = repoPath.resolve(".versionhandle").resolve("branches").resolve(branchName);

        try {
            if(!Files.exists(branchPath)) {
                return null;
            }

            String commitId = Files.readString(branchPath).trim();

            if(commitId.isEmpty()) {
                return null;
            }

            return commitId;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read branch: " + branchName, e);
        }
    }

    /**
     * Updates a branch to point to a commit
     * @param repoPath project root repository
     * @param branchName branch name
     * @param commitId commit id to write
     */
    public void writeBranch(Path repoPath, String branchName, String commitId) {
        Path branchPath = repoPath.resolve(".versionhandle").resolve("branches").resolve(branchName);

        if(commitId == null) {
            commitId = "";
        }

        try {
            Files.writeString(branchPath, commitId);
        } catch(IOException e) {
            throw new RuntimeException("Failed to update branch: " + branchName, e);
        }
    }

    /**
     * Reads the latest commit id from HEAD
     * @param repoPath project root repository
     * @return latest commit id from HEAD or null if missing
     */
    public String readHead(Path repoPath) {
        Path headPath = repoPath.resolve(".versionhandle").resolve("HEAD");

        try {
            if(!Files.exists(headPath)) {
                return null;
            }

            String commitId = Files.readString(headPath).trim();

            if(commitId.isEmpty()) {
                return null;
            }

            return commitId;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read HEAD: ", e);
        }
    }

    /**
     * Updates HEAD to point to a commit
     * @param repoPath project root repository
     * @param commitId commit id to write
     */
    public void writeHead(Path repoPath, String commitId) {
        Path branchPath = repoPath.resolve(".versionhandle").resolve("HEAD");

        if(commitId == null) {
            commitId = "";
        }

        try {
            Files.writeString(branchPath, commitId);
        } catch(IOException e) {
            throw new RuntimeException("Failed to update HEAD: ", e);
        }
    }
}
