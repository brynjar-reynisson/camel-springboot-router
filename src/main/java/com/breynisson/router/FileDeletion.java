package com.breynisson.router;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileDeletion {

    public String deleteFile(String path) {
        Path pathToDelete = Paths.get(path);
        try {
            boolean deleted = Files.deleteIfExists(pathToDelete);
            if(deleted) {
                return path + " deleted";
            } else {
                return path + " not found, and thus not deleted";
            }
        } catch (IOException e) {
            return "Exception deleting " + path + ", " + e.getMessage();
        }

    }
}
