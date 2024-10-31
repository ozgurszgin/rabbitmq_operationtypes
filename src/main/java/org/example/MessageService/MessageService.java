package org.example.MessageService;

import com.rabbitmq.client.Channel;
import org.example.repository.BlockedIpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageService {
    private MessageSender sender;
    private MessageReceiver receiver;

    @Autowired
    BlockedIpRepository blockedIpRepository;

    @Autowired
    Channel channel;

    public MessageService() {
        this.sender = new MessageSender();
        this.receiver = new MessageReceiver(blockedIpRepository, channel);
    }

    public MessageSender getSender() {
        return sender;
    }

    public MessageReceiver getReceiver() {
        return receiver;
    }
}
