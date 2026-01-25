package com.im.mq;

import com.im.config.RabbitMQConfig;
import com.im.entity.Message;
import com.im.mapper.MessageMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessageConsumer {

    @Autowired
    private MessageMapper messageMapper;

    @RabbitListener(queues = RabbitMQConfig.IM_MESSAGE_QUEUE)
    public void handleMessage(Message message) {
        System.out.println("MQ received message: " + message.getMsgId());
        // Async persistence
        messageMapper.insert(message);
    }
}
