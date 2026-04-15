package com.lynch.versionhandle.service;

import com.lynch.versionhandle.model.Commit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatusService {

    public void status(Path repoPath) {

        // Check project is initialised
        Path vhPath = repoPath.resolve(".versionhandle");

        if(!Files.exists(vhPath)) {
            System.out.println("Not a versionhandle repository. Initialise project first.");
            return;
        }

        List<File> staged = new ArrayList<>();
        List<File> modified = new ArrayList<>();
        List<File> untracked = new ArrayList<>();

        Map<String, String> index = new IndexService().loadIndex(repoPath);

        // Collect staged files
        for(Map.Entry<String, String> entry: index.entrySet()) {
            String fileName = entry.getKey();
            Path filePath = repoPath.resolve(fileName);

            staged.add(filePath.toFile());
        }

        // Collect untracked files
        try {
            CommitService commitService = new CommitService();
            Commit current = commitService.loadCommit(repoPath, commitService.readCurrent(repoPath));

            for(Path path : Files.walk(repoPath).toList()) {
                if(!Files.isRegularFile(path)) {
                    continue;
                }

                Path relativePath = repoPath.relativize(path);
                if(relativePath.startsWith(".versionhandle")) {
                    continue;
                }

                if(!current.getSnapshot().containsKey(relativePath.toString())) {
                    untracked.add(path.toFile());
                }
            }

        } catch(IOException e) {
            throw new RuntimeException("Failed to scan working directory", e);
        }

//        for(File file: staged) {
//            System.out.println(file.toString());
//        }

        for(File file: untracked) {
            System.out.println(file);
        }
    }
}
