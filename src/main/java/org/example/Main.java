package org.example;

import org.example.MessageService.MessageReceiver;
import org.example.MessageService.MessageSender;
import org.example.repository.BlockedIpRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
        BlockedIpRepository blockedIpRepository=context.getBean(BlockedIpRepository.class);

        MessageSender messageSender=new MessageSender();
        MessageReceiver messageReceiver=new MessageReceiver(blockedIpRepository);
        Thread messageSenderThread = new Thread(() -> {
            try {
                messageSender.sendMessage("getall","getall.*");
            } catch (Exception e) {
                e.printStackTrace();
            }            try {
                Thread.sleep(20000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });Thread messageReceiverThread = new Thread(() -> {
            try {
                messageReceiver.listenForMessages();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
            messageSenderThread.start();
            messageReceiverThread.start();
    }
}