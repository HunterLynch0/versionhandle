package com.lynch.versionhandle.service;

import com.lynch.versionhandle.model.Commit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;


public class LogService {

    /**
     * Prints commit history from current with details of timestamp and message
     * @param repoPath project root repository
     */
    public void log(Path repoPath) {

        Path vhPath = repoPath.resolve(".versionhandle");

        // Check project is initialised
        if(!Files.exists(vhPath)) {
            System.out.println("Not a versionhandle repository. Initialise project first.");
            return;
        }

        CommitService commitService = new CommitService();

        String currentBranch = commitService.readCurrent(repoPath);
        String commitId = commitService.readHead(repoPath);

        if(commitId == null) {
            System.out.println("No commits.");
            return;
        }

        // Basic info before log
        if(currentBranch == null) {
            System.out.println("Detached HEAD at " + commitId +
                    "\n   - Commits are listed descending from HEAD." +
                    "\n   - To log all commits run 'log -a'\n");
        } else {
            System.out.println("On branch " + currentBranch +
                    "\n   - Commits are listed descending from HEAD." +
                    "\n   - To log all commits run 'log -a'\n");
        }

        // Print commit chain starting from CURRENT
        while(commitId != null) {
            Commit commit = commitService.loadCommit(repoPath, commitId);
            printCommit(commit);

            commitId = commit.getParentId();
        }
    }

    /**
     * Prints commit history from top with details of timestamp and message
     * @param  repoPath project root repository
     */
    public void logAll(Path repoPath) {

        Path vhPath = repoPath.resolve(".versionhandle");
        Path commitsPath = vhPath.resolve("commits");

        // Check project is initialised
        if(!Files.exists(vhPath)) {
            System.out.println("Not a versionhandle repository. Initialise project first.");
            return;
        }

        // Print commits latest to oldest
        try {
            CommitService commitService = new CommitService();

            // Create a list of Commit objects so that we can sort by timestamp
            List<Commit> commits = Files.list(commitsPath).map(path -> commitService.loadCommit(repoPath, path.getFileName().toString())).collect(Collectors.toList());

            // Check if any commits
            if(commits.isEmpty()) {
                System.out.println("No commits.");
                return;
            }

            commits.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

            // Basic info before log
            String currentBranch = commitService.readCurrent(repoPath);
            String commitId = commitService.readHead(repoPath);

            if(currentBranch == null) {
                System.out.println("Detached HEAD at " + commitId +
                        "\n   - Commits are listed descending from newest to oldest.");
            } else {
                System.out.println("On branch " + currentBranch +
                        "\n   - Commits are listed descending from newest to oldest.");
            }

            // Print contents
            for(Commit commit: commits) {
                printCommit(commit);
            }
        } catch(IOException e) {
            throw new RuntimeException("Failed to fetch commits", e);
        }
    }

    public void printCommit(Commit commit) {
        System.out.println("commit " + commit.getId());
        System.out.println("Message: " + commit.getMessage());
        System.out.println("Timestamp: " + commit.getTimestamp());
        System.out.println("Parent: " + commit.getParentId());
        if(commit.getSecondParentId() != null) {
            System.out.println("Parent2: " + commit.getSecondParentId());
        }
        System.out.println();
    }
}
