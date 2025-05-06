package infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    public static <T> T executeWithRetry(SupplierWithException<T> operation, String operationName) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                return operation.get();
            } catch (Exception e) {
                attempt++;
                logger.warn("Attempt {} failed for {}: {}", attempt, operationName, e.getMessage());
                if (attempt == MAX_RETRIES) {
                    logger.error("Operation {} failed after {} attempts", operationName, MAX_RETRIES, e);
                    throw new RuntimeException("Operation failed: " + operationName, e);
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
        }
        throw new RuntimeException("Unexpected error in retry logic");
    }

    @FunctionalInterface
    public interface SupplierWithException<T> {
        T get() throws Exception;
    }
}