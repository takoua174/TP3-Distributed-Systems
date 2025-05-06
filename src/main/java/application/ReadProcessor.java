package application;

import data.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ReadProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ReadProcessor.class);
    private final FileManager fileManager;

    public ReadProcessor(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    public String readLast() {
        return fileManager.readLastLine();
    }

    public List<String> readAll() {
        return fileManager.readAllLines();
    }
}