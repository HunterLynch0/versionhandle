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

        // Abort on reserved name
        if(branchName.equals("--abort")) {
            System.out.println("Error: name '--abort' not allowed, please choose a different branch name.");
            return;
        }

        // Check branch doesnt already exist
        Path branchPath = vhPath.resolve("branches").resolve(branchName);

        if(Files.exists(branchPath)) {
            System.out.println("Branch already exists: " + branchName);
            return;
        }

        CommitService commitService = new CommitService();

        String commitId = commitService.readHead(repoPath);
        if(commitId == null) {
            commitId = "";
        }

        // Branch and set latest commit
        try {
            Files.createFile(branchPath);
            Files.writeString(branchPath, commitId);
            System.out.println("Created branch: " + branchName);
        } catch(IOException e) {
            throw new RuntimeException("Failed to create branch: " + branchName, e);
        }
    }
}
