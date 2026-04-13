package com.lynch.versionhandle.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;


public class LogService {

    /**
     * Prints commit history with details of timestamp and message
     */
    public void log() {

        Path repoPath = Path.of(".");
        Path vhPath = repoPath.resolve(".versionhandle");
        Path commitsPath = vhPath.resolve("commits");

        // Check project is initialised
        if(!Files.exists(vhPath)) {
            System.out.println("Not a versionhandle repository. Initialise project first.");
            return;
        }

        try {
            // Get list of files in .versionhandle/commits
            List<Path> commits = Files.list(commitsPath).toList();

            if(commits.isEmpty()) {
                System.out.println("No commits.");
                return;
            }

            // Prints details of commits
            for(Path commit: commits) {
                Scanner fileScan = new Scanner(commit.toFile());
                System.out.println("commit " + commit.getFileName());

                int count = 0;
                while(fileScan.hasNextLine() && count < 2) {
                    System.out.println(fileScan.nextLine());
                    count++;
                }

                System.out.println();
                fileScan.close();
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch commits", e);
        }
    }
}
