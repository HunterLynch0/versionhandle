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

    }
}
