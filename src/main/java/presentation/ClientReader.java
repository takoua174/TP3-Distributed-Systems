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
    private volatile boolean responseReceived = false;

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
            logger.info("Sent Read Last to {}", replicaQueue);
        }

        DeliverCallback callback = (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(correlationId) && !responseReceived) {
                synchronized (this) {
                    if (!responseReceived) {
                        responseReceived = true;
                        String message = new String(delivery.getBody(), "UTF-8");
                        logger.info("Received last message: {}", message);
                        System.out.println("Last line: " + message);
                        try {
                            consumer.getChannel().basicCancel(consumerTag);
                            logger.info("Consumer cancelled after last message");
                        } catch (Exception e) {
                            logger.error("Failed to cancel consumer", e);
                        }
                    }
                }
            }
        };

        consumer.consume(replyQueue, callback);
    }

    public void close() {
        queueManager.close();
        logger.info("ClientReader closed");
    }

    public static void main(String[] args) {
        ConfigManager config = new ConfigManager();
        ClientReader reader = new ClientReader(config);
        try {
            reader.readLastLine();
            // Wait briefly for response
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted during read", e);
        } finally {
            reader.close();
        }
    }
}