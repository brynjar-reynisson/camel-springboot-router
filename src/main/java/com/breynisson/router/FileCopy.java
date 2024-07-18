package com.breynisson.router;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileCopy {

    public String copyFile(String src, String destination) {
        Path srcPath = Paths.get(src);
        if(!srcPath.toFile().isFile()) {
            throw new RouterException("Can't copy " + src + " to " + destination + ", as it doesn't exist");
        }
        Path destinationPath = Paths.get(destination);
        try {
            Files.copy(srcPath, destinationPath);
            return "Copied " + src + " to " + destination;
        } catch (IOException e) {
            throw new RouterException("Could not copy " + src + ", exception: " + e.getMessage(), e);
        }
    }
}
