package com.breynisson.router;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ResourceReader {

    public static String read(String path) throws IOException, URISyntaxException {
        URI uri = ResourceReader.class.getResource(path).toURI();
        Path filePath = Paths.get(uri);
        return Files.readString(filePath);
    }
}
