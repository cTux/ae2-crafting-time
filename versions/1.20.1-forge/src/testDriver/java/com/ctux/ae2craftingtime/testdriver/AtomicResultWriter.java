package com.ctux.ae2craftingtime.testdriver;

import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class AtomicResultWriter {
    public static void write(Path outputDirectory, DriverResult result) throws IOException {
        Files.createDirectories(outputDirectory);
        var temporary = outputDirectory.resolve("result.json.tmp");
        var destination = outputDirectory.resolve("result.json");
        Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(result),
                StandardCharsets.UTF_8);
        Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    private AtomicResultWriter() {
    }
}
