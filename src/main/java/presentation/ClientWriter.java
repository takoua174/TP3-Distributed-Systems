package presentation;

import infrastructure.ConfigManager;
import messaging.MessagePublisher;
import messaging.QueueManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientWriter {
    private static final Logger logger = LoggerFactory.getLogger(ClientWriter.class);
    private final QueueManager queueManager;
    private final MessagePublisher publisher;
    private final String writeExchange;

    public ClientWriter(ConfigManager config) {
        this.queueManager = new QueueManager(config);
        this.writeExchange = config.getProperty("write.exchange");
        queueManager.declareExchange(writeExchange, "fanout"); // Ensure exchange is declared
        this.publisher = new MessagePublisher(queueManager.createChannel());
    }

    public void writeMessages() {
        String[] messages = {
                "1 Texte message1",
                "2 Texte message2",
                "3 Texte message3",
                "4 Texte message4"
        };
        for (String message : messages) {
            publisher.publishToExchange(writeExchange, "", message);
            logger.info("Sent: {}", message);
            try {
                Thread.sleep(1000); // Simulate delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted during write", e);
            }
        }
    }

    public void close() {
        queueManager.close();
        logger.info("ClientWriter closed");
    }

    public static void main(String[] args) {
        ConfigManager config = new ConfigManager();
        ClientWriter writer = new ClientWriter(config);
        try {
            writer.writeMessages();
        } finally {
            writer.close();
        }
    }
}