package com.lynch.versionhandle.service;

import com.lynch.versionhandle.util.HashUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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

        String timestamp = LocalDateTime.now().toString();

        // Build commit file
        StringBuilder commitContent = new StringBuilder();
        commitContent.append("Message: ").append(message).append("\n");
        commitContent.append("Timestamp: ").append(timestamp).append("\n\n");

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
}
