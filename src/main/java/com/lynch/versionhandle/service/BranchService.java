package com.lynch.versionhandle.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BranchService {

    /**
     * Creates a branch
     * @param repoPath project root repository
     * @param branchName name of new branch
     */
    public void branch(Path repoPath, String branchName) {

        // Check project is initialised
        Path vhPath = repoPath.resolve(".versionhandle");

        if(!Files.exists(vhPath)) {
            System.out.println("Not a versionhandle repository. Initialise project first.");
            return;
        }

        // Check branch doesnt already exist
        Path branchPath = vhPath.resolve("branches").resolve(branchName);

        if(Files.exists(branchPath)) {
            System.out.println("Branch already exists: " + branchName);
            return;
        }

        CommitService commitService = new CommitService();

        // Get latest commitId and make sure there is a current branch set + a pointer to latest commit
        String currentBranch = commitService.readCurrent(repoPath);
        if(currentBranch == null) {
            System.out.println("Error: no current branch set.");
            return;
        }
        String commitId = commitService.readBranch(repoPath, currentBranch);
        if(commitId == null) {
            commitId = "";
        }

        // Branch and set latest commit
        try {
            Files.createFile(branchPath);
            Files.writeString(branchPath, commitId);
        } catch(IOException e) {
            throw new RuntimeException("Failed to create branch: " + branchName, e);
        }
    }
}
