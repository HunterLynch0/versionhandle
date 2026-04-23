package com.lynch.versionhandle.util;

public class IgnoreUtil {

    /**
     * Helper method to filter out unwanted files
     * @param relativePath filepath relative to repository
     * @return whether the program should ignore the file or not
     */
    public static boolean shouldIgnore(String relativePath) {
        return relativePath.startsWith(".versionhandle")
                || relativePath.startsWith(".git")
                || relativePath.startsWith(".idea")
                || relativePath.startsWith("target")
                || relativePath.startsWith("out")
                || relativePath.equals(".DS_Store");
    }
}
