package com.lynch.versionhandle.service;

import com.lynch.versionhandle.model.Commit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.file.Files.deleteIfExists;

public class CheckoutService {

    /**
     *
     * @param repoPath
     * @param targetName
     * @param force
     */
    public void checkout(Path repoPath, String targetName, boolean force) {

        // Check project is initialised
        Path vhPath = repoPath.resolve(".versionhandle");

        if(!Files.exists(vhPath)) {
            System.out.println("Not a versionhandle repository. Initialise project first.");
            return;
        }

        // Check whether the target is a branch or a commit
        Path branchPath = vhPath.resolve("branches").resolve(targetName);
        Path commitPath = vhPath.resolve("commits").resolve(targetName);

        String targetBranch = null;
        String targetCommit = null;

        CommitService commitService = new CommitService();

        if(Files.exists(branchPath)) {
            targetBranch = targetName;
            targetCommit = commitService.readBranch(repoPath, targetBranch);
        } else if(Files.exists(commitPath)) {
            targetCommit = targetName;
        } else {
            System.out.println("Error: Checkout target not found: " + targetName);
            return;
        }

        if(targetCommit == null) {
            System.out.println("Branch has no commits yet.");
            return;
        }

        // Load target commit
        Commit target = commitService.loadCommit(repoPath, targetCommit);

        if(force) {
            // Remove all files in working directory that are not in target including untracked
            try {
                for(Path path : Files.walk(repoPath).toList()) {
                    if(!Files.isRegularFile(path)) {
                        continue;
                    }

                    String relativePath = repoPath.relativize(path).toString();
                    if(relativePath.startsWith(".versionhandle")) {
                        continue;
                    }

                    if(!target.getSnapshot().containsKey(relativePath)) {
                        try {
                            deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete file: " + relativePath, e);
                        }
                    }
                }
            } catch(IOException e) {
                throw new RuntimeException("Failed to loop through working directory.", e);
            }
        } else {
            String currentBranch = commitService.readCurrent(repoPath);
            String currentCommitId = null;

            if(currentBranch != null) {
                currentCommitId = commitService.readBranch(repoPath, currentBranch);
            }

            if(currentCommitId != null) {
                Commit current = commitService.loadCommit(repoPath, currentCommitId);

                // Check for untracked files
                try {
                    List<Path> unTrackedFiles = new ArrayList<>();
                    boolean unTracked = false;

                    for (Path path : Files.walk(repoPath).toList()) {
                        if (!Files.isRegularFile(path)) {
                            continue;
                        }

                        String relativePath = repoPath.relativize(path).toString();

                        if (relativePath.startsWith(".versionhandle")) {
                            continue;
                        }

                        if (!current.getSnapshot().containsKey(relativePath)) {
                            unTracked = true;
                            unTrackedFiles.add(path);
                        }
                    }

                    if(unTracked) {
                        System.out.println("Checkout aborted: Working directory contains untracked files.\n\nUntracked files:");
                        for(Path path: unTrackedFiles) {
                            System.out.println("   - " + repoPath.relativize(path));
                        }
                        System.out.println("\nFixes:"  +
                                "\n   - Stage and commit files to save current working directory." +
                                "\n   - Run 'checkout <commitId> -f' to force checkout (WARNING: you will lose all untracked files).");
                        return;
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to loop through working directory.", e);
                }

                // Remove all files that are in CURRENT but not in target
                for (Map.Entry<String, String> file : current.getSnapshot().entrySet()) {
                    String fileName = file.getKey();
                    Path filePath = repoPath.resolve(fileName);

                    if (!target.getSnapshot().containsKey(fileName)) {
                        try {
                            Files.deleteIfExists(filePath);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete file: " + fileName, e);
                        }
                    }
                }
            }
        }


        // Restore all remaining files to target snapshot state
        for(Map.Entry<String, String> file: target.getSnapshot().entrySet()) {
            String fileName = file.getKey();
            Path filePath = repoPath.resolve(fileName);

            String objectHash = file.getValue();
            Path objectPath = vhPath.resolve("objects").resolve(objectHash);

            try {
                if(filePath.getParent() != null) {
                    Files.createDirectories(filePath.getParent());
                }
                Files.write(filePath, Files.readAllBytes(objectPath));
            } catch (IOException e) {
                throw new RuntimeException("Failed to read file: " + fileName, e);
            }
        }

        new IndexService().saveIndex(repoPath, new HashMap<String, String>());
        commitService.writeCurrent(repoPath, targetBranch);

        System.out.println("Checked out: " + targetName);

    }
}
