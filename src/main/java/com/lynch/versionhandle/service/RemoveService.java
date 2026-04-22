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

        // Check for detached head state
        String currentBranch = commitService.readCurrent(repoPath);
        if(currentBranch == null) {
            System.out.println("Error: operation not available in detached HEAD state.");
            return;
        }

        String commitId = commitService.readHead(repoPath);

        boolean tracked = false;
        // Check file is tracked

        if(commitId != null) {
            Commit current = commitService.loadCommit(repoPath, commitId);
            tracked = current.getSnapshot().containsKey(fileName);
        }

        Map<String, String> index = indexService.loadIndex(repoPath);
        boolean staged = index.containsKey(fileName);

        if(!tracked && !staged) {
            System.out.println("File is not tracked or staged: " + fileName);
            return;
        }

        Path filePath = repoPath.resolve(fileName);

        try {
            Files.deleteIfExists(filePath);
        } catch(IOException e) {
            throw new RuntimeException("Failed to delete file: " + fileName);
        }

        index.put(fileName, DELETED);
        indexService.saveIndex(repoPath, index);

        System.out.println("File deletion staged: " + fileName);
    }
}
