package com.lynch.versionhandle.service;

import com.lynch.versionhandle.model.Commit;
import com.lynch.versionhandle.util.HashUtil;
import com.lynch.versionhandle.util.IgnoreUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MergeService {

    private static final String DELETED = "<DELETED>";

    /**
     * Merge target branch into current branch
     * @param repoPath project root repository
     * @param targetName branch to be merged into current
     */
    public void merge(Path repoPath, String targetName) {

        // Check project is initialised
        Path vhPath = repoPath.resolve(".versionhandle");
        if (!Files.exists(vhPath)) {
            System.out.println("Not a versionhandle repository. Initialise project first.");
            return;
        }

        CommitService commitService = new CommitService();
        IndexService indexService = new IndexService();

        // Check user is on branch
        String currentBranch = commitService.readCurrent(repoPath);
        if (currentBranch == null) {
            System.out.println("Error: operation not available in detached HEAD state.");
            return;
        }

        // Check target branch exists
        Path targetBranchPath = vhPath.resolve("branches").resolve(targetName);
        if (!Files.exists(targetBranchPath)) {
            System.out.println("Error: branch not found: " + targetName);
            return;
        }

        // Check target is not the current branch
        if (currentBranch.equals(targetName)) {
            System.out.println("Error: cannot merge branch '" + currentBranch + "' into itself.");
            return;
        }

        String currentCommitId = commitService.readBranch(repoPath, currentBranch);
        String targetCommitId = commitService.readBranch(repoPath, targetName);

        // Check branches have commits
        if (currentCommitId == null && targetCommitId == null) {
            System.out.println("Error: neither branches '" + currentBranch + "' or '" + targetName + "' have commits.");
            return;
        }
        if (currentCommitId == null || targetCommitId == null) {
            System.out.println("Error: branch '" + (currentCommitId == null ? currentBranch : targetName) + "' has no commits.");
            return;
        }

        // Abort merge if there are staged changes
        Map<String, String> index = indexService.loadIndex(repoPath);
        if (!index.isEmpty()) {
            System.out.println("Merge aborted: you have staged changes.");
            System.out.println("\nTip:" +
                    "\n   - Stage and commit changes to save current working directory.");
            return;
        }

        // Load commit
        Commit current = commitService.loadCommit(repoPath, currentCommitId);

        // Check working directory for untracked or modified files
        try {
            List<String> untracked = new ArrayList<>();
            List<String> modified = new ArrayList<>();

            for (Path path: Files.walk(repoPath).toList()) {
                if (!Files.isRegularFile(path)) {
                    continue;
                }

                String relativePath = repoPath.relativize(path).toString();
                if (IgnoreUtil.shouldIgnore(relativePath)) {
                    continue;
                }

                // Files untracked - in working directory but not in current commit
                if (!current.getSnapshot().containsKey(relativePath)) {
                    untracked.add(relativePath);
                    continue;
                }

                String currentHash = current.getSnapshot().get(relativePath);
                String workingHash = HashUtil.sha256(Files.readAllBytes(path));

                // Files modified - current content different from working content
                if (!currentHash.equals(workingHash)) {
                    modified.add(relativePath);
                }
            }

            // Files deleted - in current snapshot but no in working
            for (Map.Entry<String, String> entry : current.getSnapshot().entrySet()) {
                if (!Files.exists(repoPath.resolve(entry.getKey()))) {
                    modified.add(entry.getKey() + " (deleted locally)");
                }
            }

            // Abort merge if there are untracked or modified files
            if(!untracked.isEmpty() || !modified.isEmpty()) {
                System.out.println("Merge aborted: you have uncommitted changes in your working directory.");
                if(!untracked.isEmpty()) {
                    System.out.println("\nUntracked files:");
                    for(String path: untracked) {
                        System.out.println("   - " + path);
                    }
                }
                if(!modified.isEmpty()) {
                    System.out.println("\nModified files:");
                    for(String path: modified) {
                        System.out.println("   - " + path);
                    }
                }
                System.out.println("\nTip:" +
                        "\n   - Stage and commit changes to save current working directory.");
                return;
            }
        } catch(IOException e) {
            throw new RuntimeException("Failed to loop through working directory.", e);
        }

        // Load commits + snapshots
        Commit target = commitService.loadCommit(repoPath, targetCommitId);
        Commit ancestor = null;
        String ancestorId = findCommonAncestor(repoPath, currentCommitId, targetCommitId);
        if (ancestorId != null) {
            ancestor = commitService.loadCommit(repoPath, ancestorId);
        }
        Map<String, String> ancestorSnapshot = ancestor == null ? new HashMap<>() : ancestor.getSnapshot();
        Map<String, String> currentSnapshot = current.getSnapshot();
        Map<String, String> targetSnapshot = target.getSnapshot();

        // Get all files
        Set<String> files = new HashSet<>();
        files.addAll(ancestorSnapshot.keySet());
        files.addAll(currentSnapshot.keySet());
        files.addAll(targetSnapshot.keySet());

        Map<String, String> mergeSnapshot = new HashMap<>(currentSnapshot);
        List<String> conflicts = new ArrayList<>();

        // Sort files depending on their difference between snapshots
        for(String file: files) {
            String baseHash = ancestorSnapshot.get(file);
            String currentHash = currentSnapshot.get(file);
            String targetHash = targetSnapshot.get(file);

            if(Objects.equals(currentHash, targetHash)) {
                mergeSnapshot.put(file, currentSnapshot.get(file));
            } else if(Objects.equals(baseHash, currentHash)) {
                if(targetHash == null) {
                    mergeSnapshot.remove(file);
                } else {
                    mergeSnapshot.put(file, targetHash);
                }
            } else if(Objects.equals(baseHash, targetHash)) {
                mergeSnapshot.put(file, currentHash);
            } else {
                conflicts.add(file);
            }
        }

        if(!conflicts.isEmpty()) {
            try {
                for(String file: conflicts) {
                    // Get content from both snapshots
                    String currentHash = currentSnapshot.get(file);
                    String targetHash = targetSnapshot.get(file);
                    String currentContent;
                    String targetContent;
                    if(currentHash == null) {
                        currentContent = DELETED;
                    } else {
                        currentContent = new String(Files.readAllBytes(vhPath.resolve("objects").resolve(currentHash)));
                    }
                    if(targetHash == null)  {
                        targetContent = DELETED;
                    } else {
                        targetContent = new String(Files.readAllBytes(vhPath.resolve("objects").resolve(targetHash)));
                    }


                    String conflictContent = "<<<<<<< CURRENT\n"+
                            currentContent +
                            "\n=======\n" +
                            targetContent +
                            "\n>>>>>>> " + targetName + "\n";
                    Path filePath = repoPath.resolve(file);

                    if(filePath.getParent() != null) {
                        Files.createDirectories(filePath.getParent());
                    }

                    Files.writeString(filePath, conflictContent);
                }
            } catch(IOException e) {
                throw new RuntimeException("Failed to write conflict markers", e);
            }

            System.out.println("Merge conflict in:");
            for(String file : conflicts) {
                System.out.println("   - " + file);
            }

            System.out.println("\nConflict markers have been written to the files.");
            System.out.println("\nTip: Fix conflicts, then stage and commit the resolved files.");
            return;
        } else {
            // Rewrite working directory to merged
            try {
                for(Path path: Files.walk(repoPath).toList()) {
                    if(!Files.isRegularFile(path)) {
                        continue;
                    }

                    String relativePath = repoPath.relativize(path).toString();
                    if(IgnoreUtil.shouldIgnore(relativePath)) {
                        continue;
                    }

                    if(!mergeSnapshot.containsKey(relativePath)) {
                        Files.deleteIfExists(path);
                        continue;
                    }

                    String currentHash = HashUtil.sha256(Files.readAllBytes(path));
                    String mergedHash = mergeSnapshot.get(relativePath);

                    if(!Objects.equals(currentHash, mergedHash)) {
                        Path objectPath = vhPath.resolve("objects").resolve(mergedHash);
                        Files.write(path, Files.readAllBytes(objectPath));
                    }
                }
            } catch(IOException e) {
                throw new RuntimeException("Failed to loop through working directory", e);
            }

            // Create missing files from working
            for(String file: mergeSnapshot.keySet()) {
                Path filePath = repoPath.resolve(file);

                if(!Files.exists(filePath)) {
                    String objectHash = mergeSnapshot.get(file);
                    Path objectPath = vhPath.resolve("objects").resolve(objectHash);

                    try {
                        if(filePath.getParent() != null) {
                            Files.createDirectories(filePath.getParent());
                        }
                        Files.write(filePath, Files.readAllBytes(objectPath));
                    } catch(IOException e) {
                        throw new RuntimeException("Failed to create and write file: " + filePath);
                    }
                }
            }

            // Commit merge
            String message = "Merge branch '" + targetName + "' into '" +  currentBranch + "'";
            String mergeCommitId = commitService.mergeCommit(repoPath, message, currentCommitId, targetCommitId, mergeSnapshot);

            indexService.saveIndex(repoPath, new HashMap<>());
            commitService.writeBranch(repoPath, currentBranch, mergeCommitId);
            commitService.writeHead(repoPath, mergeCommitId);

            System.out.println("Merged branch '" + targetName + "' into '" +  currentBranch + "'");
            System.out.println("commitId: " + mergeCommitId);
        }
    }

    /**
     * Finds the most recent common ancestor between to commits
     * @param repoPath project root repository
     * @param currentId commitId1
     * @param targetId commitId2
     * @return commitId of common ancestor or null is there is none
     */
    public String findCommonAncestor(Path repoPath, String currentId, String targetId) {
        CommitService commitService = new CommitService();

        Set<String> currentAncestors = new HashSet<>();
        Set<String> visited = new HashSet<>();
        Stack<String> stack = new Stack<>();

        // Get all current ancestors
        stack.push(currentId);
        while(!stack.isEmpty()) {
            String commitId = stack.pop();

            if(commitId == null || visited.contains(commitId)) {
                continue;
            }

            visited.add(commitId);
            currentAncestors.add(commitId);

            Commit commit = commitService.loadCommit(repoPath, commitId);
            stack.push(commit.getParentId());
            stack.push(commit.getSecondParentId());
        }

        // Find the lowest common ancestor in target
        visited.clear();
        stack.push(targetId);

        while(!stack.isEmpty()) {
            String commitId = stack.pop();

            if(commitId == null || visited.contains(commitId)) {
                continue;
            }

            if(currentAncestors.contains(commitId)) {
                return commitId;
            }

            visited.add(commitId);

            Commit commit = commitService.loadCommit(repoPath, commitId);
            stack.push(commit.getParentId());
            stack.push(commit.getSecondParentId());
        }

        return null;
    }
}
