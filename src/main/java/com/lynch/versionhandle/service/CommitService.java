package com.lynch.versionhandle.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class CommitService {

    /**
     *
     */
    public void commit(String message) {

        Path repoPath = Path.of(".");
        Path vhPath = repoPath.resolve(".versionhandle");
        Path commitPath = vhPath.resolve("commits");

        if(!Files.exists(vhPath)) {
            System.out.println("Not a versionhandle repository. Initialise project first.");
            return;
        }

        Map<String, String> index = new IndexService().loadIndex(repoPath);

        if(index.isEmpty()) {
            System.out.println("Nothing added, Nothing to commit.");
        }
    }
}
