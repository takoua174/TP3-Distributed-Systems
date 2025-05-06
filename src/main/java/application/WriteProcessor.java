package application;

import data.DataValidator;
import data.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriteProcessor {
    private static final Logger logger = LoggerFactory.getLogger(WriteProcessor.class);
    private final FileManager fileManager;
    private final DataValidator validator;

    public WriteProcessor(FileManager fileManager, DataValidator validator) {
        this.fileManager = fileManager;
        this.validator = validator;
    }

    public void processWrite(String message) {
        if (validator.isValidLine(message)) {
            fileManager.writeLine(message);
        } else {
            logger.warn("Skipping invalid write: {}", message);
        }
    }
}