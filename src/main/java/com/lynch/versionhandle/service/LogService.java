package com.lynch.versionhandle.service;

import com.lynch.versionhandle.model.Commit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;


public class LogService {

    /**
     * Prints commit history with details of timestamp and message
     */
    public void log(Path repoPath) {

        Path vhPath = repoPath.resolve(".versionhandle");
        Path commitsPath = vhPath.resolve("commits");

        // Check project is initialised
        if(!Files.exists(vhPath)) {
            System.out.println("Not a versionhandle repository. Initialise project first.");
            return;
        }


        // Read CURRENT
        String commitId = new CommitService().readCurrent(repoPath);

        if(commitId == null) {
            System.out.println("No commits.");
            return;
        }

        // Print commit chain starting from CURRENT
        while(commitId != null) {
            Commit commit = new CommitService().loadCommit(repoPath, commitId);

            System.out.println("commit " + commit.getId());
            System.out.println("Message: " + commit.getMessage());
            System.out.println("Timestamp: " + commit.getTimestamp());
            System.out.println("Parent: " + commit.getParentId());
            System.out.println();

            commitId = commit.getParentId();
        }

    }
}
