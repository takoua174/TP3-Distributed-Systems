package presentation;

import application.ReadProcessor;
import application.WriteProcessor;
import com.rabbitmq.client.DeliverCallback;
import data.DataValidator;
import data.FileManager;
import infrastructure.ConfigManager;
import messaging.MessageConsumer;
import messaging.MessagePublisher;
import messaging.QueueManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Replica {
    private static final Logger logger = LoggerFactory.getLogger(Replica.class);
    private final String replicaId;
    private final QueueManager queueManager;
    private final MessagePublisher publisher;
    private final MessageConsumer consumer;
    private final WriteProcessor writeProcessor;
    private final ReadProcessor readProcessor;
    private final String writeExchange;
    private final String writeQueue; // Per-replica write queue
    private final String replicaQueue;

    public Replica(String replicaId, ConfigManager config) {
        this.replicaId = replicaId;
        this.queueManager = new QueueManager(config);
        this.writeExchange = config.getProperty("write.exchange");
        this.writeQueue = config.getProperty("write.queue.prefix") + replicaId + "_queue";
        this.replicaQueue = config.getProperty("replica.queue.prefix") + replicaId + "_queue";
        String directory = config.getProperty("replica.directory.prefix") + replicaId;

        // Declare exchange and queues
        queueManager.declareExchange(writeExchange, "fanout");
        queueManager.declareQueue(writeQueue);
        queueManager.declareQueue(replicaQueue);

        // Bind write queue to exchange
        queueManager.bindQueueToExchange(writeQueue, writeExchange, "");

        this.publisher = new MessagePublisher(queueManager.createChannel());
        this.consumer = new MessageConsumer(queueManager.createChannel());
        this.writeProcessor = new WriteProcessor(new FileManager(directory), new DataValidator());
        this.readProcessor = new ReadProcessor(new FileManager(directory));
    }

    public void start() {
        // Handle write messages
        DeliverCallback writeCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            writeProcessor.processWrite(message);
            logger.info("Replica {} processed write: {}", replicaId, message);
        };
        consumer.consume(writeQueue, writeCallback);

        // Handle read requests
        DeliverCallback readCallback = (consumerTag, delivery) -> {
            String request = new String(delivery.getBody(), "UTF-8");
            String replyTo = delivery.getProperties().getReplyTo();
            String correlationId = delivery.getProperties().getCorrelationId();

            if (request.equals("Read Last")) {
                String lastLine = readProcessor.readLast();
                publisher.publish(replyTo, lastLine, correlationId, null);
                logger.info("Replica {} sent last line: {}", replicaId, lastLine);
            } else if (request.equals("Read All")) {
                readProcessor.readAll().forEach(line -> {
                    publisher.publish(replyTo, line, correlationId, null);
                    logger.info("Replica {} sent line: {}", replicaId, line);
                });
            }
        };
        consumer.consume(replicaQueue, readCallback);

        logger.info("Replica {} started", replicaId);
    }

    public void close() {
        queueManager.close();
        logger.info("Replica {} closed", replicaId);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            logger.error("Usage: java Replica <replicaId>");
            System.out.println("Usage: java Replica <replicaId>");
            return;
        }
        ConfigManager config = new ConfigManager();
        Replica replica = new Replica(args[0], config);
        try {
            replica.start();
            // Keep running
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Replica {} interrupted", args[0], e);
        } finally {
            replica.close();
        }
    }
}