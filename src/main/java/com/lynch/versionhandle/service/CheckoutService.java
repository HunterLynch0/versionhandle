package com.lynch.versionhandle.service;

import com.lynch.versionhandle.model.Commit;
import com.lynch.versionhandle.util.HashUtil;
import com.lynch.versionhandle.util.IgnoreUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckoutService {

    /**
     * Move users working directory to a given branch or commit(detached head mode)
     * @param repoPath project root repository
     * @param targetName target branch or commit
     * @param force force option to check out with untracked files meaning you will lose them
     */
    public void checkout(Path repoPath, String targetName, boolean force) {

        // Check project is initialised
        Path vhPath = repoPath.resolve(".versionhandle");

        if(!Files.exists(vhPath)) {
            System.out.println("Not a versionhandle repository. Initialise project first.");
            return;
        }

        CommitService commitService = new CommitService();
        IndexService indexService = new IndexService();

        // Resolve target
        Path branchPath = vhPath.resolve("branches").resolve(targetName);
        Path commitPath = vhPath.resolve("commits").resolve(targetName);

        String targetBranch = null;
        String targetCommit;

        if(Files.exists(branchPath)) {
            targetBranch = targetName;
            targetCommit = commitService.readBranch(repoPath, targetBranch);
        } else if(Files.exists(commitPath)) {
            targetCommit = targetName;
        } else {
            System.out.println("Error: checkout target not found: " + targetName);
            return;
        }

        // Handle empty branch
        if(targetBranch != null && targetCommit == null) {
            if(!force) {
                Map<String, String> index = indexService.loadIndex(repoPath);
                if(!index.isEmpty()) {
                    System.out.println("Checkout aborted: you have staged changes.");
                    System.out.println("\nTip:"  +
                            "\n   - Stage and commit changes to save current working directory." +
                            "\n   - Or run 'checkout -f <target>' to force checkout (WARNING: you will lose local changes).");
                    return;
                }

                try {
                    String currentCommitId = commitService.readHead(repoPath);

                    if(currentCommitId != null) {
                        Commit current = commitService.loadCommit(repoPath, currentCommitId);

                        List<String> untracked = new ArrayList<>();
                        List<String> modified = new ArrayList<>();

                        for(Path path: Files.walk(repoPath).toList()) {
                            if(!Files.isRegularFile(path)) {
                                continue;
                            }

                            String relativePath = repoPath.relativize(path).toString();
                            if(IgnoreUtil.shouldIgnore(relativePath)) {
                                continue;
                            }

                            // Files untracked - in working directory but not in current commit
                            if(!current.getSnapshot().containsKey(relativePath)) {
                                untracked.add(relativePath);
                                continue;
                            }

                            String currentHash = current.getSnapshot().get(relativePath);
                            String workingHash = HashUtil.sha256(Files.readAllBytes(path));

                            // Files modified - current content different from working content
                            if(!currentHash.equals(workingHash)) {
                                modified.add(relativePath);
                            }
                        }

                        // Files deleted - in current snapshot but no in working
                        for(Map.Entry<String, String> entry: current.getSnapshot().entrySet()) {
                            if(!Files.exists(repoPath.resolve(entry.getKey()))) {
                                modified.add(entry.getKey() + " (deleted locally)");
                            }
                        }

                        // Abort checkout if there are untracked or modified files
                        if(!untracked.isEmpty() || !modified.isEmpty()) {
                            System.out.println("Checkout aborted: you have uncommitted changes in your working directory.");
                            if(!untracked.isEmpty()) {
                                System.out.println("\nUntracked files:");
                                for (String path : untracked) {
                                    System.out.println("   - " + path);
                                }
                            }
                            if(!modified.isEmpty()) {
                                System.out.println("\nModified files:");
                                for (String path: modified) {
                                    System.out.println("   - " + path);
                                }
                            }
                            System.out.println("\nTip:"  +
                                    "\n   - Stage and commit changes to save current working directory." +
                                    "\n   - Or run 'checkout -f <target>' to force checkout (WARNING: you will lose local changes).");
                            return;
                        }
                    }
                } catch(IOException e) {
                    throw new RuntimeException("Failed to loop through working directory.", e);
                }
            }

            try {
                for(Path path: Files.walk(repoPath).toList()) {
                    if(!Files.isRegularFile(path)) {
                        continue;
                    }

                    String relativePath = repoPath.relativize(path).toString();
                    if(IgnoreUtil.shouldIgnore(relativePath)) {
                        continue;
                    }

                    Files.deleteIfExists(path);
                }
            } catch(IOException e) {
                throw new RuntimeException("Failed to clear working directory.");
            }

            indexService.saveIndex(repoPath, new HashMap<>());
            commitService.writeCurrent(repoPath, targetBranch);
            commitService.writeHead(repoPath, null);

            System.out.println("Checked out empty branch: " + targetBranch);
            return;
        }


        // Load target commit
        Commit target = commitService.loadCommit(repoPath, targetCommit);

        if (force) {
            try {
                for(Path path : Files.walk(repoPath).toList()) {
                    if(!Files.isRegularFile(path)) {
                        continue;
                    }

                    String relativePath = repoPath.relativize(path).toString();
                    if (IgnoreUtil.shouldIgnore(relativePath)) {
                        continue;
                    }

                    if (!target.getSnapshot().containsKey(relativePath)) {
                        Files.deleteIfExists(path);
                    }
                }
            } catch(IOException e) {
                throw new RuntimeException("Failed to clear working directory.");
            }

        } else {
            String currentCommitId = commitService.readHead(repoPath);

            if(currentCommitId != null) {
                Commit current = commitService.loadCommit(repoPath, currentCommitId);

                // Check for staged
                Map<String, String> index = indexService.loadIndex(repoPath);
                if (!index.isEmpty()) {
                    System.out.println("Checkout aborted: you have staged changes.");
                    System.out.println("\nTip:" +
                            "\n   - Stage and commit changes to save current working directory." +
                            "\n   - Or run 'checkout -f <target>' to force checkout (WARNING: you will lose local changes).");
                    return;
                }

                try {
                    List<String> untracked = new ArrayList<>();
                    List<String> modified = new ArrayList<>();

                    for (Path path : Files.walk(repoPath).toList()) {
                        if(!Files.isRegularFile(path)) {
                            continue;
                        }

                        String relativePath = repoPath.relativize(path).toString();
                        if(IgnoreUtil.shouldIgnore(relativePath)) {
                            continue;
                        }

                        // Files untracked - in working directory but not in current commit
                        if(!current.getSnapshot().containsKey(relativePath)) {
                            untracked.add(relativePath);
                            continue;
                        }

                        String currentHash = current.getSnapshot().get(relativePath);
                        String workingHash = HashUtil.sha256(Files.readAllBytes(path));

                        // Files modified - current content different from working content
                        if(!currentHash.equals(workingHash)) {
                            modified.add(relativePath);
                        }
                    }

                    // Files deleted - in current snapshot but no in working
                    for(Map.Entry<String, String> entry : current.getSnapshot().entrySet()) {
                        if(!Files.exists(repoPath.resolve(entry.getKey()))) {
                            modified.add(entry.getKey() + " (deleted locally)");
                        }
                    }

                    if(!untracked.isEmpty() || !modified.isEmpty()) {
                        System.out.println("Checkout aborted: you have uncommitted changes in your working directory.");
                        if(!untracked.isEmpty()) {
                            System.out.println("\nUntracked files:");
                            for(String path : untracked) {
                                System.out.println("   - " + path);
                            }
                        }
                        if(!modified.isEmpty()) {
                            System.out.println("\nModified files:");
                            for(String path : modified) {
                                System.out.println("   - " + path);
                            }
                        }
                        System.out.println("\nTip:" +
                                "\n   - Stage and commit changes to save current working directory." +
                                "\n   - Or run 'checkout -f <target>' to force checkout (WARNING: you will lose local changes).");
                        return;
                    }
                } catch(IOException e) {
                    throw new RuntimeException("Failed to loop through working directory.", e);
                }

                // Remove all files that are in CURRENT but not in target
                for(Map.Entry<String, String> file : current.getSnapshot().entrySet()) {
                    String fileName = file.getKey();
                    Path filePath = repoPath.resolve(fileName);

                    if(!target.getSnapshot().containsKey(fileName)) {
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

        indexService.saveIndex(repoPath, new HashMap<String, String>());
        commitService.writeCurrent(repoPath, targetBranch);
        commitService.writeHead(repoPath, targetCommit);

        System.out.println("Checked out: " + targetName);
    }
}
