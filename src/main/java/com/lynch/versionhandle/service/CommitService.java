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
    public void commit(String message) {

        Path repoPath = Path.of(".");
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

        String parentId = readCurrent(repoPath);

        String timestamp = LocalDateTime.now().toString();

        // Build commit file
        StringBuilder commitContent = new StringBuilder();
        commitContent.append("Message: ").append(message).append("\n");
        commitContent.append("Timestamp: ").append(timestamp).append("\n");
        commitContent.append("Parent: ").append(parentId).append("\n\n");

        for(Map.Entry<String, String> entry: index.entrySet()) {
            commitContent.append(entry.getKey()).append(" ").append(entry.getValue()).append("\n");
        }

        // Hash content and commit snapshot
        try {
            byte[] content = commitContent.toString().getBytes();
            String hash = HashUtil.sha256(content);

            Path commitPath = vhPath.resolve("commits").resolve(hash);

            Files.write(commitPath, content);

            new IndexService().saveIndex(repoPath, new HashMap<String, String>());

            System.out.println("Snapshot committed: " + message);
            System.out.println("Commit hash: " + hash);

        } catch (IOException e) {
            throw new RuntimeException("Failed to commit snapshot: " + message, e);
        }
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

    public Commit loadCommit(Path repoPath, String commitId) {
        Path commitPath = repoPath.resolve(".versionhandle").resolve("commits").resolve(commitId);

        String message;
        String timestamp;
        String parent;
        Map<String, String> snapshot = new HashMap<>();

        try (Scanner fileScan = new Scanner(commitPath.toFile())) {
            message = fileScan.nextLine().replaceFirst("Message: ", "");
            timestamp = fileScan.nextLine().replaceFirst("TimeStamp: ", "");
            parent = fileScan.nextLine().replaceFirst("Parent: ", "");
            fileScan.nextLine();

            while(fileScan.hasNextLine()) {
                String[] parts = fileScan.nextLine().split(" ");
                snapshot.put(parts[0], parts[1]);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Commit", e);
        }

        return new Commit(commitId, message, timestamp, parent, snapshot);
    }

    public void saveCommit(Path repoPath, Commit commit) {
        Path commitPath = repoPath.resolve(".versionhandle").resolve("commits").resolve(commit.getId());


    }
}
