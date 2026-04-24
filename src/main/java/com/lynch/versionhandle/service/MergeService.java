package com.lynch.versionhandle.service;

import com.lynch.versionhandle.model.Commit;
import com.lynch.versionhandle.util.HashUtil;
import com.lynch.versionhandle.util.IgnoreUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MergeService {

    public void merge(Path repoPath, String targetName) {

        // Check project is initialised
        Path vhPath = repoPath.resolve(".versionhandle");
        if(!Files.exists(vhPath)) {
            System.out.println("Not a versionhandle repository. Initialise project first.");
            return;
        }

        CommitService commitService = new CommitService();

        // Check user is on branch
        String currentBranch = commitService.readCurrent(repoPath);
        if(currentBranch == null) {
            System.out.println("Error: operation not available in detached HEAD state.");
            return;
        }

        // Check target branch exists
        Path targetBranchPath = vhPath.resolve("branches").resolve(targetName);
        if(!Files.exists(targetBranchPath)) {
            System.out.println("Error: branch not found: " + targetName);
            return;
        }

        // Check target is not the current branch
        if(currentBranch.equals(targetName)) {
            System.out.println("Error: cannot merge branch '" + currentBranch + "' into itself.");
            return;
        }

        String currentCommitId = commitService.readBranch(repoPath, currentBranch);
        String targetCommitId = commitService.readBranch(repoPath, targetName);

        // Check branches have commits
        if(currentCommitId == null && targetCommitId == null) {
            System.out.println("Error: neither branches '" + currentBranch + "' or '" + targetName + "' have commits.");
            return;
        }
        if(currentCommitId == null || targetCommitId == null) {
            System.out.println("Error: branch '" + (currentCommitId == null ? currentBranch : targetName) + "' has no commits.");
            return;
        }

        // Load commit
        Commit current = commitService.loadCommit(repoPath, currentCommitId);

        // Check working directory for untracked or modified files
        try {
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

            // Abort merge if there are untracked or modified files
            if(!untracked.isEmpty() || !modified.isEmpty()) {
                System.out.println("Merge aborted: you have uncommitted changes in your working directory.");
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
                        "\n   - Run 'checkout <target> -f' to force checkout (WARNING: you will lose local changes).");
                return;
            }
        } catch(IOException e) {
            throw new RuntimeException("Failed to loop through working directory.", e);
        }

        // Load target and ancestor
        Commit target = commitService.loadCommit(repoPath, targetCommitId);
        String ancestorId = findCommonAncestor(repoPath, currentCommitId, targetCommitId);
        if(ancestorId != null) {
            Commit ancestor = commitService.loadCommit(repoPath, ancestorId);
        }

    }

    public String findCommonAncestor(Path repoPath, String commitId1, String commitId2) {
        CommitService commitService = new CommitService();
        Set<String> commitIds = new HashSet<>();

        while(commitId1 != null) {
            commitIds.add(commitId1);

            Commit commit = commitService.loadCommit(repoPath, commitId1);
            commitId1 = commit.getParentId();
        }

        while(commitId2 != null) {
            if(commitIds.contains(commitId2)) {
                return commitId2;
            }
            Commit commit = commitService.loadCommit(repoPath, commitId2);
            commitId2 = commit.getParentId();
        }

        return null;
    }
}
