package messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageConsumer {
    private static final Logger logger = LoggerFactory.getLogger(MessageConsumer.class);
    private final Channel channel;

    public MessageConsumer(Channel channel) {
        this.channel = channel;
    }

    public void consume(String queue, DeliverCallback callback) {
        try {
            channel.basicConsume(queue, true, callback, consumerTag -> {});
            //logger.info("Started consuming from queue: {}", queue);
        } catch (Exception e) {
            //ogger.error("Failed to consume from queue: {}", queue, e);
            throw new RuntimeException("Consume failed", e);
        }
    }
}