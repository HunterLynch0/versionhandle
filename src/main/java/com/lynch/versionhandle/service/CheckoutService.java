package com.lynch.versionhandle.service;

import com.lynch.versionhandle.model.Commit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.file.Files.deleteIfExists;

public class CheckoutService {

    /**
     *
     * @param repoPath
     * @param commitId
     * @param force
     */
    public void checkout(Path repoPath, String commitId, boolean force) {

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

        CommitService commitService = new CommitService();

        // Load target commit
        Commit target = commitService.loadCommit(repoPath, commitId);

        if(force) {
            // Remove all files in working directory that are not in target including untracked
            try {
                for (Path path : Files.walk(repoPath).toList()) {
                    if (!Files.isRegularFile(path)) {
                        continue;
                    }

                    String relativePath = repoPath.relativize(path).toString();

                    if (relativePath.startsWith(".versionhandle")) {
                        continue;
                    }

                    if (!target.getSnapshot().containsKey(relativePath)) {
                        try {
                            deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete file: " + relativePath, e);
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to loop through working directory.", e);
            }
        } else {
            // Remove all files that are in CURRENT but not in target
            Commit current = commitService.loadCommit(repoPath, commitService.readCurrent(repoPath));

            // Check for tracked files
            try {
                for (Path path : Files.walk(repoPath).toList()) {
                    if (!Files.isRegularFile(path)) {
                        continue;
                    }

                    String relativePath = repoPath.relativize(path).toString();

                    if (relativePath.startsWith(".versionhandle")) {
                        continue;
                    }

                    if(!current.getSnapshot().containsKey(path.toString())) {
                        System.out.println("Checkout aborted: you have untracked files, stage and commit." +
                                "\nrun 'checkout <commitId> -f' to force checkout (Warning: you will loose untracked files for good)");
                        return;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to loop through working directory.", e);
            }


            for(Map.Entry<String, String> file: current.getSnapshot().entrySet()) {
                String fileName = file.getKey();
                Path filePath = vhPath.resolve(fileName);

                if(!target.getSnapshot().containsKey(fileName)) {
                    try {
                        Files.deleteIfExists(filePath);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete file: " + fileName, e);
                    }
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
