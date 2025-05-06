package presentation;

import com.rabbitmq.client.DeliverCallback;
import infrastructure.ConfigManager;
import messaging.MessageConsumer;
import messaging.MessagePublisher;
import messaging.QueueManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ClientReader {
    private static final Logger logger = LoggerFactory.getLogger(ClientReader.class);
    private final QueueManager queueManager;
    private final MessagePublisher publisher;
    private final MessageConsumer consumer;
    private final String replyQueue;
    private final String[] replicaQueues;

    public ClientReader(ConfigManager config) {
        this.queueManager = new QueueManager(config);
        this.replyQueue = config.getProperty("reply.queue");
        String prefix = config.getProperty("replica.queue.prefix");
        this.replicaQueues = new String[]{prefix + "1_queue", prefix + "2_queue", prefix + "3_queue"};
        queueManager.declareQueue(replyQueue);
        for (String queue : replicaQueues) {
            queueManager.declareQueue(queue);
        }
        this.publisher = new MessagePublisher(queueManager.createChannel());
        this.consumer = new MessageConsumer(queueManager.createChannel());
    }

    public void readLastLine() {
        String correlationId = UUID.randomUUID().toString();

        for (String replicaQueue : replicaQueues) {
            publisher.publish(replicaQueue, "Read Last", correlationId, replyQueue);
        }

        DeliverCallback callback = (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(correlationId)) {
                String message = new String(delivery.getBody(), "UTF-8");
                logger.info("Received: {}", message);
                System.out.println("Last line: " + message);
            }
        };

        consumer.consume(replyQueue, callback);
    }

    public void close() {
        queueManager.close();
    }

    public static void main(String[] args) {
        ConfigManager config = new ConfigManager();
        ClientReader reader = new ClientReader(config);
        try {
            reader.readLastLine();
            // Keep consuming
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted during read", e);
        } finally {
            reader.close();
        }
    }
}