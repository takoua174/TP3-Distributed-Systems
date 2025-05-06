package data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class FileManager {
    private static final Logger logger = LoggerFactory.getLogger(FileManager.class);
    private final String directory;
    private final String fileName = "data.txt";

    public FileManager(String directory) {
        this.directory = directory;
        createDirectory();
    }

    private void createDirectory() {
        try {
            Files.createDirectories(Paths.get(directory));
            logger.info("Directory created or already exists: {}", directory);
        } catch (IOException e) {
            logger.error("Failed to create directory: {}", directory, e);
            throw new RuntimeException("Directory creation failed", e);
        }
    }

    public void writeLine(String line) {
        try {
            Path filePath = Paths.get(directory, fileName);
            Files.write(filePath, (line + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            logger.info("Wrote line to {}: {}", filePath, line);
        } catch (IOException e) {
            logger.error("Failed to write line to file: {}", line, e);
            throw new RuntimeException("File write failed", e);
        }
    }

    public String readLastLine() {
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(directory, fileName)))) {
            String lastLine = "";
            String line;
            while ((line = reader.readLine()) != null) {
                lastLine = line;
            }
            logger.info("Read last line: {}", lastLine.isEmpty() ? "No data" : lastLine);
            return lastLine.isEmpty() ? "No data" : lastLine;
        } catch (IOException e) {
            logger.error("Failed to read last line", e);
            return "Error reading file";
        }
    }

    public List<String> readAllLines() {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(directory, fileName)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            logger.info("Read {} lines from {}", lines.size(), fileName);
        } catch (IOException e) {
            logger.error("Failed to read all lines", e);
        }
        return lines;
    }
}