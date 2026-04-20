package com.lynch.versionhandle.service;

import com.lynch.versionhandle.model.Commit;
import com.lynch.versionhandle.util.HashUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatusService {

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

        if(currentBranch == null) {
            System.out.println("Error: Current branch not set.");
            return;
        }

        String currentId = commitService.readBranch(repoPath, currentBranch);

        if(currentId == null) {
            System.out.println("Branch has no commits yet.");
            return;
        }

        Commit current = commitService.loadCommit(repoPath, currentId);

        List<String> staged = new ArrayList<>();
        List<String> modified = new ArrayList<>();
        List<String> untracked = new ArrayList<>();

        try {
            System.out.println("On branch " + currentBranch);

            for(Path path : Files.walk(repoPath).toList()) {
                if(!Files.isRegularFile(path)) {
                    continue;
                }

                String relativePath = repoPath.relativize(path).toString();

                if(relativePath.startsWith(".versionhandle")) {
                    continue;
                }

                String hash = HashUtil.sha256(Files.readAllBytes(path));

                // Add to staged - in index but different file content than current file content
                if(index.containsKey(relativePath) && !index.get(relativePath).equals(current.getSnapshot().get(relativePath))) {
                    staged.add(relativePath);
                }

                // Add to modified - in index but working directory file content is different from index file content
                if(index.containsKey(relativePath) && !hash.equals(index.get(relativePath))) {
                    modified.add(relativePath);
                }

                // Add to untracked - not in index or current
                if(!index.containsKey(relativePath) && !current.getSnapshot().containsKey(relativePath)) {
                    untracked.add(relativePath);
                }
            }

        } catch(IOException e) {
            throw new RuntimeException("Failed to scan working directory", e);
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
    }
}
