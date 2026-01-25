package com.im.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String IM_MESSAGE_QUEUE = "im_message_queue";

    @Bean
    public Queue messageQueue() {
        return new Queue(IM_MESSAGE_QUEUE, true);
    }
}
