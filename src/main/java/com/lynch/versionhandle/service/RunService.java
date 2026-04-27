package com.lynch.versionhandle.service;

import com.lynch.versionhandle.model.Commit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class RunService {

    /**
     *
     * @param repoPath project root repository
     * @param commitId target commit id
     * @param command command to run
     */
    public void run(Path repoPath, String commitId, List<String> command) {

        Path vhPath = repoPath.resolve(".versionhandle");

        // Check project is initialised
        if(!Files.exists(vhPath)) {
            System.out.println("Not a versionhandle repository. Initialise project first.");
            return;
        }

        // Check commit exists
        Path commitPath = vhPath.resolve("commits").resolve(commitId);
        if(!Files.exists(commitPath)) {
            System.out.println("Error: commit not found: " + commitId);
            return;
        }

        Path tempDir;

        // Create temporary directory
        try {
            tempDir = Files.createTempDirectory("versionhandle-run");
        } catch(IOException e) {
            throw new RuntimeException("Error: failed to create temporary directory.", e);
        }

        // Load commit and populate tempDir with snapshot
        CommitService commitService = new CommitService();
        Commit commit = commitService.loadCommit(repoPath, commitId);
        Map<String, String> snapshot = commit.getSnapshot();

        for(Map.Entry<String, String> entry: snapshot.entrySet()) {
            String filename = entry.getKey();
            String hash = entry.getValue();

            Path filePath = tempDir.resolve(filename);
            Path objectPath = vhPath.resolve("objects").resolve(hash);

            try {
                if(filePath.getParent() != null) {
                    Files.createDirectories(filePath.getParent());
                }

                Files.write(filePath, Files.readAllBytes(objectPath));
            } catch(IOException e) {
                throw new RuntimeException("Failed to write file: " + filename);
            }
        }

        // Run command
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(tempDir.toFile());
            pb.inheritIO();

            Process process = pb.start();
            int exitCode = process.waitFor();

            System.out.println("Process exited with code: " + exitCode);
        } catch(IOException | InterruptedException e) {
            throw new RuntimeException("Failed to run command.", e);
        }

        // Clear tempDir
        try {
            for(Path path: Files.walk(tempDir).toList()) {
                Files.deleteIfExists(path);
            }
        } catch(IOException e) {
            throw new RuntimeException("Failed to clear temporary directory.", e);
        }
    }
}
