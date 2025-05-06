package presentation;

import application.MajorityVoter;
import com.rabbitmq.client.DeliverCallback;
import infrastructure.ConfigManager;
import messaging.MessageConsumer;
import messaging.MessagePublisher;
import messaging.QueueManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ClientReaderV2 {
    private static final Logger logger = LoggerFactory.getLogger(ClientReaderV2.class);
    private final QueueManager queueManager;
    private final MessagePublisher publisher;
    private final MessageConsumer consumer;
    private final MajorityVoter voter;
    private final String replyQueue;
    private final String[] replicaQueues;

    public ClientReaderV2(ConfigManager config) {
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
        this.voter = new MajorityVoter(replicaQueues.length);
    }

    public void readAllLines() throws InterruptedException {
        String correlationId = UUID.randomUUID().toString();
        List<String> allLines = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(replicaQueues.length);

        for (String replicaQueue : replicaQueues) {
            publisher.publish(replicaQueue, "Read All", correlationId, replyQueue);
        }

        DeliverCallback callback = (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(correlationId)) {
                String line = new String(delivery.getBody(), "UTF-8");
                synchronized (allLines) {
                    allLines.add(line);
                }
            }
        };

        consumer.consume(replyQueue, callback);

        // Wait for responses or timeout
        latch.await(5, TimeUnit.SECONDS);

        List<String> majorityLines = voter.getMajorityLines(allLines);
        logger.info("Majority lines:");
        System.out.println("Majority lines:");
        majorityLines.forEach(System.out::println);
    }

    public void close() {
        queueManager.close();
    }

    public static void main(String[] args) {
        ConfigManager config = new ConfigManager();
        ClientReaderV2 reader = new ClientReaderV2(config);
        try {
            reader.readAllLines();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted during read", e);
        } finally {
            reader.close();
        }
    }
}