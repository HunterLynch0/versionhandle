package com.lynch.versionhandle.service;

import com.lynch.versionhandle.model.Commit;
import com.lynch.versionhandle.util.HashUtil;
import com.lynch.versionhandle.util.IgnoreUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class StatusService {

    private static final String DELETED = "<DELETED>";

    /**
     * Gives information on current branch, files staged, files modified, files untracked, files deleted
     * @param repoPath project root repository
     */
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

        List<String> untracked = new ArrayList<>();
        List<String> staged = new ArrayList<>();
        List<String> modified = new ArrayList<>();
        List<String> deleted = new ArrayList<>();

        Set<String> workingSeen = new HashSet<>();

        try {
            for(Path path : Files.walk(repoPath).toList()) {
                if(!Files.isRegularFile(path)) {
                    continue;
                }

                String relativePath = repoPath.relativize(path).toString();

                if(IgnoreUtil.shouldIgnore(relativePath)) {
                    continue;
                }

                workingSeen.add(relativePath);

                boolean inSnapshot = currentSnapshot.containsKey(relativePath);
                boolean inIndex = index.containsKey(relativePath);

                // Add to untracked - not in current or index
                if(!inSnapshot && !inIndex) {
                    untracked.add(relativePath);
                    continue;
                }

                // Add to staged - in index but different file content than current file content
                if(inIndex && !index.get(relativePath).equals(currentSnapshot.get(relativePath))) {
                    staged.add(relativePath);
                }

                // Add to modified - in index but working directory file content is different from index file content
                String workingHash = HashUtil.sha256(Files.readAllBytes(path));
                String indexHash = null;

                if(inIndex && !index.get(relativePath).equals(DELETED)) {
                    indexHash = index.get(relativePath);
                } else if(inSnapshot) {
                    indexHash = currentSnapshot.get(relativePath);
                }

                if(indexHash != null && !workingHash.equals(indexHash)) {
                    modified.add(relativePath);
                }
            }

            // Find deleted files
            Set<String> trackedOrStaged = new HashSet<>();
            trackedOrStaged.addAll(currentSnapshot.keySet());
            trackedOrStaged.addAll(index.keySet());

            for(String fileName: trackedOrStaged) {
                if(IgnoreUtil.shouldIgnore(fileName)) {
                    continue;
                }

                boolean inWorking = workingSeen.contains(fileName);
                boolean inSnapshot = currentSnapshot.containsKey(fileName);
                boolean inIndex = index.containsKey(fileName);

                // Staged deletion
                if(inIndex && index.get(fileName).equals(DELETED)) {
                    if(!staged.contains(fileName)) {
                        staged.add(fileName);
                    }
                    continue;
                }

                // Unstaged deletion
                if(inSnapshot && !inWorking) {
                    deleted.add(fileName);
                }
            }

        } catch(IOException e) {
            throw new RuntimeException("Failed to scan working directory", e);
        }

        // Print staged changes
        System.out.println("Staged changes:");
        if(staged.isEmpty()) {
            System.out.println("     <empty>");
        } else {
            for(String file: staged) {
                if(index.containsKey(file) && index.get(file).equals(DELETED)) {
                    System.out.println("   - " + file + " (deleted)");
                } else if(!currentSnapshot.containsKey(file)) {
                    System.out.println("   - " + file + " (added)");
                } else {
                    System.out.println("   - " + file + " (modified)");
                }
            }
        }

        System.out.println();

        // Print modified files
        System.out.println("Modified files:");
        if(modified.isEmpty()) {
            System.out.println("     <empty>");
        } else {
            for(String file: modified) {
                System.out.println("   - " + file);
            }
        }
        System.out.println();

        // Print untracked files
        System.out.println("Untracked files:");
        if(untracked.isEmpty()) {
            System.out.println("     <empty>");
        } else {
            for(String file: untracked) {
                System.out.println("   - " + file);
            }
        }
        System.out.println();

        // Print unstaged deletions
        System.out.println("Deleted files:");
        if(deleted.isEmpty()) {
            System.out.println("     <empty>");
        } else {
            for(String file: deleted) {
                System.out.println("   - " + file);
            }
        }
        System.out.println();
    }
}
