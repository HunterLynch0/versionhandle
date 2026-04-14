package com.lynch.versionhandle.service;

import com.lynch.versionhandle.model.Commit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class CheckoutService {

    public void checkout(Path repoPath, String commitId) {

        // Check project is initialised
        Path vhPath = repoPath.resolve(".versionhandle");

        if(!Files.exists(vhPath)) {
            System.out.println("Not a versionhandle repository. Initialise project first.");
            return;
        }

        // Check that the given id matches a commit
        Path commitPath = vhPath.resolve("commits").resolve(commitId);
        if(!Files.exists(commitPath)) {
            System.out.println("Commit not found. Run 'log -a' to list all commit ids.");
            return;
        }

        // Load target commit and latest commit
        CommitService commitService = new CommitService();

        Commit target = commitService.loadCommit(repoPath, commitId);
        Commit current = commitService.loadCommit(repoPath, commitService.readCurrent(repoPath));


        // Remove all files in current snapshot that are not in target
        for(Map.Entry<String, String> file: current.getSnapshot().entrySet()) {
            String fileName = file.getKey();
            Path filePath = repoPath.resolve(fileName);

            if(!target.getSnapshot().containsKey(fileName)) {
                try {
                    Files.deleteIfExists(filePath);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to delete file: " + fileName, e);
                }
            }
        }

        // Restore all remaining files to target snapshot state
        for(Map.Entry<String, String> file: target.getSnapshot().entrySet()) {
            String fileName = file.getKey();
            Path filePath = repoPath.resolve(fileName);

            String objectHash = file.getValue();
            Path objectPath = vhPath.resolve("objects").resolve(objectHash);

            try {
                if(filePath.getParent() != null) {
                    Files.createDirectories(filePath.getParent());
                }
                Files.write(filePath, Files.readAllBytes(objectPath));
            } catch (IOException e) {
                throw new RuntimeException("Failed to read file: " + fileName, e);
            }
        }

        new IndexService().saveIndex(repoPath, new HashMap<String, String>());
        commitService.writeCurrent(repoPath, commitId);

        System.out.println("Checked out commit: " + commitId);
    }
}
