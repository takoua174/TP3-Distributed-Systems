package infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingUtility {
    private static final Logger logger = LoggerFactory.getLogger(LoggingUtility.class);

    /**
     * Logs an info message with a standardized format.
     *
     * @param component The component name (e.g., ClientWriter, Replica).
     * @param message   The message to log.
     */
    public static void logInfo(String component, String message) {
        logger.info("[{}] {}", component, message);
    }

    /**
     * Logs an error message with an exception.
     *
     * @param component The component name.
     * @param message   The error message.
     * @param throwable The exception.
     */
    public static void logError(String component, String message, Throwable throwable) {
        logger.error("[{}] {}", component, message, throwable);
    }

    /**
     * Logs a debug message (only visible if log level is DEBUG).
     *
     * @param component The component name.
     * @param message   The debug message.
     */
    public static void logDebug(String component, String message) {
        logger.debug("[{}] {}", component, message);
    }
}