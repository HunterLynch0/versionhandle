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
     */
    public void log(Path repoPath) {

        Path vhPath = repoPath.resolve(".versionhandle");

        // Check project is initialised
        if(!Files.exists(vhPath)) {
            System.out.println("Not a versionhandle repository. Initialise project first.");
            return;
        }

        // Read CURRENT
        CommitService commitService = new CommitService();
        String currentBranch = commitService.readCurrent(repoPath);

        if(currentBranch == null) {
            System.out.println("Error: Current branch not set.");
            return;
        }

        String commitId = commitService.readBranch(repoPath, currentBranch);

        if(commitId == null) {
            System.out.println("Branch has no commits yet.");
            return;
        }

        // Warn that commits are only listed from CURRENT
        System.out.println("Note:" +
                "\n   - Commits are listed descending from current." +
                "\n   - To log all commits run 'log -a'\n");

        // Print commit chain starting from CURRENT
        while(commitId != null) {
            Commit commit = commitService.loadCommit(repoPath, commitId);

            System.out.println("commit " + commit.getId());
            System.out.println("Message: " + commit.getMessage());
            System.out.println("Timestamp: " + commit.getTimestamp());
            System.out.println("Parent: " + commit.getParentId());
            System.out.println();

            commitId = commit.getParentId();
        }
    }

    /**
     * Prints commit history from top with details of timestamp and message
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

            // Print contents
            for(Commit commit: commits) {
                System.out.println("commit " + commit.getId());
                System.out.println("Message: " + commit.getMessage());
                System.out.println("Timestamp: " + commit.getTimestamp());
                System.out.println("Parent: " + commit.getParentId());
                System.out.println();
            }
        } catch(IOException e) {
            throw new RuntimeException("Failed to fetch commits", e);
        }
    }
}
