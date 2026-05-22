package com.im.config;

import com.im.mq.MessageRouteListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisPubSubConfig {

    public static final String ROUTE_CHANNEL = "im:route:channel";

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                                   MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new ChannelTopic(ROUTE_CHANNEL));
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(MessageRouteListener receiver) {
        // MessageRouteListener implements MessageListener, so we don't need to specify a delegate method.
        // It will directly invoke onMessage on MessageRouteListener.
        return new MessageListenerAdapter(receiver);
    }
}
