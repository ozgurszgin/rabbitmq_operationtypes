package org.example.MessageService;

import org.example.repository.BlockedIpRepository;
import org.example.service.BlockedIpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageService {
    private MessageSender sender;
    private MessageReceiver receiver;

    @Autowired
    BlockedIpRepository blockedIpRepository;

    public MessageService() {
        this.sender = new MessageSender();
        this.receiver = new MessageReceiver(blockedIpRepository);
    }

    public MessageSender getSender() {
        return sender;
    }

    public MessageReceiver getReceiver() {
        return receiver;
    }
}
