package com.lynch.versionhandle.service;

import com.lynch.versionhandle.model.Commit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class RemoveService {

    public static final String DELETED = "<DELETED>";

    /**
     * Remove a file from working directory and mark it as deleted in index to exclude from future commits
     * @param repoPath project root repository
     * @param fileName file to remove
     */
    public void remove(Path repoPath, String fileName) {

        Path vhPath = repoPath.resolve(".versionhandle");

        // Check project is initialised
        if (!Files.exists(vhPath)) {
            System.out.print("Not a versionhandle repository. Initialise project first.");
            return;
        }

        IndexService indexService = new IndexService();
        CommitService commitService = new CommitService();

        String currentBranch = commitService.readCurrent(repoPath);

        // Check branch is set
        if(currentBranch == null) {
            System.out.println("Error: current branch not set.");
            return;
        }

        String currentCommitId = commitService.readBranch(repoPath, currentBranch);

        boolean tracked = false;

        // Check file is tracked
        if(currentCommitId != null) {
            Commit current = commitService.loadCommit(repoPath, currentCommitId);
            tracked = current.getSnapshot().containsKey(fileName);
        }

        Path filePath = repoPath.resolve(fileName);

        if(!tracked) {
            System.out.println("File not tracked: " + fileName);
            return;
        }

        try {
            Files.deleteIfExists(filePath);
        } catch(IOException e) {
            throw new RuntimeException("Failed to delete file: " + fileName);
        }

        Map<String, String> index = indexService.loadIndex(repoPath);

        index.put(fileName, DELETED);
        indexService.saveIndex(repoPath, index);

        System.out.println("File deletion staged: " + fileName);
    }
}
