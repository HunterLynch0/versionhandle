package com.lynch.versionhandle.service;

import com.lynch.versionhandle.model.Commit;
import com.lynch.versionhandle.util.HashUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatusService {

    private static final String DELETED = "<DELETED>";

    public void status(Path repoPath) {

        // Check project is initialised
        Path vhPath = repoPath.resolve(".versionhandle");

        if(!Files.exists(vhPath)) {
            System.out.println("Not a versionhandle repository. Initialise project first.");
            return;
        }

        CommitService commitService = new CommitService();
        IndexService indexService = new IndexService();

        Map<String, String> index = indexService.loadIndex(repoPath);

        String currentBranch = commitService.readCurrent(repoPath);
        String headId = commitService.readHead(repoPath);

        Map<String, String> currentSnapshot = new HashMap<>();

        if(currentBranch == null) {
            if(headId == null) {
                System.out.println("Detached HEAD with no commit.");
            } else {
                System.out.println("Detached HEAD at " + headId);
                Commit current = commitService.loadCommit(repoPath, headId);
                currentSnapshot = current.getSnapshot();
            }
        } else {
            System.out.println("On branch " + currentBranch);

            if(headId == null) {
                System.out.println("No commits.");
            } else {
                Commit current = commitService.loadCommit(repoPath, headId);
                currentSnapshot = current.getSnapshot();
            }
        }

        System.out.println();

        List<String> staged = new ArrayList<>();
        List<String> modified = new ArrayList<>();
        List<String> untracked = new ArrayList<>();
        List<String> deleted = new ArrayList<>();

        try {
            for(Path path : Files.walk(repoPath).toList()) {
                if(!Files.isRegularFile(path)) {
                    continue;
                }

                String relativePath = repoPath.relativize(path).toString();

                if(relativePath.startsWith(".versionhandle")) {
                    continue;
                }

                // Skip deleted
                if(index.containsKey(relativePath) && index.get(relativePath).equals(DELETED));

                String hash = HashUtil.sha256(Files.readAllBytes(path));

                // Add to staged - in index but different file content than current file content
                if(index.containsKey(relativePath) && !index.get(relativePath).equals(currentSnapshot.get(relativePath))) {
                    staged.add(relativePath);
                }

                // Add to modified - in index but working directory file content is different from index file content
                if(index.containsKey(relativePath) && !hash.equals(index.get(relativePath))) {
                    modified.add(relativePath);
                }

                // Add to untracked - not in index or current
                if(!index.containsKey(relativePath) && !currentSnapshot.containsKey(relativePath)) {
                    untracked.add(relativePath);
                }
            }

        } catch(IOException e) {
            throw new RuntimeException("Failed to scan working directory", e);
        }

        for(Map.Entry<String, String> entry: index.entrySet()) {
            if(entry.getValue().equals(DELETED)) {
                deleted.add(entry.getKey());
            }
        }

        System.out.println("Staged changes:");
        if(staged.isEmpty()) {
            System.out.println("     <empty>");
        } else {
            for(String file: staged) {
                System.out.println("   - " + file);
            }
        }

        System.out.println();

        System.out.println("Changes not staged:");
        if(modified.isEmpty()) {
            System.out.println("     <empty>");
        } else {
            for(String file: modified) {
                System.out.println("   - " + file);
            }
        }

        System.out.println();

        System.out.println("Untracked files:");
        if(untracked.isEmpty()) {
            System.out.println("     <empty>");
        } else {
            for(String file: untracked) {
                System.out.println("   - " + file);
            }
        }
        System.out.println();

        System.out.println("Staged deletions:");
        if(deleted.isEmpty()) {
            System.out.println("     <empty>");
        } else {
            for(String file : deleted) {
                System.out.println("   - " + file);
            }
        }
        System.out.println();
    }
}
