package data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataValidator {
    private static final Logger logger = LoggerFactory.getLogger(DataValidator.class);

    public boolean isValidLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            logger.warn("Invalid line: null or empty");
            return false;
        }
        String[] parts = line.trim().split("\\s+", 2);
        if (parts.length < 2) {
            logger.warn("Invalid line format: {}", line);
            return false;
        }
        try {
            Integer.parseInt(parts[0]);
            return true;
        } catch (NumberFormatException e) {
            logger.warn("Invalid line number in: {}", line);
            return false;
        }
    }
}