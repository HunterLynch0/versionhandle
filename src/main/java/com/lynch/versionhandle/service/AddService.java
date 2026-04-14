package com.lynch.versionhandle.service;

import com.lynch.versionhandle.util.HashUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class AddService {

    /**
     * Adds a file or all files in the repository to staging area
     * @param fileName name of file, or "." to add all
     */
    public void add(Path repoPath, String fileName) {

        // Either add recursively or adds a single file
        if(fileName.equals(".")) {
            try {
                List<Path> files = Files.walk(repoPath).toList();

                for(Path file: files) {
                    if(!Files.isRegularFile(file)) continue;

                    String relativeName = repoPath.relativize(file).toString();

                    if(relativeName.startsWith(".versionhandle")) continue;

                    addSingleFile(repoPath, relativeName);
                }

            } catch (IOException e) {
                throw new RuntimeException("Failed to fetch files", e);
            }
        } else {
            addSingleFile(repoPath, fileName);
        }
    }

    /**
     * Checks conditions for the file to be added then adds to staging area if valid
     * @param repoPath project root repository
     * @param fileName name of file requested to add
     */
    public void addSingleFile(Path repoPath, String fileName) {

        Path vhPath = repoPath.resolve(".versionhandle");
        Path filePath = repoPath.resolve(fileName);

        // Checks project is initialised
        if(!Files.exists(vhPath)) {
            System.out.println("Not a versionhandle repository. Initialise project first.");
            return;
        }

        // Checks file exists in relevant directory and is a file not a directory
        if(!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            System.out.println("File does not exist: " + fileName);
            return;
        }

        // Hash content then store to objects + stage
        try {
            byte[] content = Files.readAllBytes(filePath);
            String hash = HashUtil.sha256(content);

            Path objectPath = vhPath.resolve("objects").resolve(hash);

            // Store
            if(!Files.exists(objectPath)) {
                Files.write(objectPath, content);
            }

            // Stage
            new IndexService().stageFile(repoPath, fileName, hash);

            System.out.println("File staged: " + fileName);
            System.out.println("Object hash: " + hash + "\n");

        } catch(IOException e) {
            throw new RuntimeException("Failed to add file: " + fileName, e);
        }
    }
}
