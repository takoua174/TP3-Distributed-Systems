package messaging;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessagePublisher {
    private static final Logger logger = LoggerFactory.getLogger(MessagePublisher.class);
    private final Channel channel;

    public MessagePublisher(Channel channel) {
        this.channel = channel;
    }

    public void publish(String queue, String message, String correlationId, String replyTo) {
        try {
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .correlationId(correlationId)
                    .replyTo(replyTo)
                    .build();
            channel.basicPublish("", queue, props, message.getBytes("UTF-8"));
            logger.debug("Published to queue {}: {}", queue, message);
        } catch (Exception e) {
            logger.error("Failed to publish to queue {}: {}", queue, message, e);
            throw new RuntimeException("Publish failed", e);
        }
    }

    public void publishToExchange(String exchange, String routingKey, String message) {
        try {
            channel.basicPublish(exchange, routingKey, null, message.getBytes("UTF-8"));
            logger.debug("Published to exchange {}: {}", exchange, message);
        } catch (Exception e) {
            logger.error("Failed to publish to exchange {}: {}", exchange, message, e);
            throw new RuntimeException("Publish failed", e);
        }
    }
}