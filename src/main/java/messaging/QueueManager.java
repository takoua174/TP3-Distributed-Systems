package messaging;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import infrastructure.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueManager {
    private static final Logger logger = LoggerFactory.getLogger(QueueManager.class);
    private final Connection connection;
    private final ConfigManager config;

    public QueueManager(ConfigManager config) {
        this.config = config;
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.getProperty("rabbitmq.host"));
        try {
            this.connection = factory.newConnection();
        } catch (Exception e) {
            logger.error("Failed to create RabbitMQ connection", e);
            throw new RuntimeException("Connection failed", e);
        }
    }

    public Channel createChannel() {
        try {
            return connection.createChannel();
        } catch (Exception e) {
            logger.error("Failed to create channel", e);
            throw new RuntimeException("Channel creation failed", e);
        }
    }

    public void declareQueue(String queueName) {
        try (Channel channel = createChannel()) {
            channel.queueDeclare(queueName, false, false, true, null);
            logger.info("Declared queue: {}", queueName);
        } catch (Exception e) {
            logger.error("Failed to declare queue: {}", queueName, e);
            throw new RuntimeException("Queue declaration failed", e);
        }
    }

    public void declareExchange(String exchangeName, String exchangeType) {
        try (Channel channel = createChannel()) {
            channel.exchangeDeclare(exchangeName, exchangeType);
            logger.info("Declared exchange: {}", exchangeName);
        } catch (Exception e) {
            logger.error("Failed to declare exchange: {}", exchangeName, e);
            throw new RuntimeException("Exchange declaration failed", e);
        }
    }

    public void bindQueueToExchange(String queueName, String exchangeName, String routingKey) {
        try (Channel channel = createChannel()) {
            channel.queueBind(queueName, exchangeName, routingKey);
            logger.info("Bound queue {} to exchange {}", queueName, exchangeName);
        } catch (Exception e) {
            logger.error("Failed to bind queue {} to exchange {}", queueName, exchangeName, e);
            throw new RuntimeException("Queue binding failed", e);
        }
    }

    public void close() {
        try {
            if (connection != null && connection.isOpen()) {
                connection.close();
                logger.info("RabbitMQ connection closed");
            }
        } catch (Exception e) {
            logger.error("Failed to close connection", e);
        }
    }
}