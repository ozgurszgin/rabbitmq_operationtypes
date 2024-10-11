package org.example;

import org.example.MessageService.MessageReceiver;
import org.example.rabbitmq.Consumer1;
import org.example.rabbitmq.Consumer2;
import org.example.rabbitmq.Consumer3;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {

        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
        Consumer3 consumer3 = context.getBean(Consumer3.class);
        Consumer1 consumer1 = context.getBean(Consumer1.class);
        Consumer2 consumer2 = context.getBean(Consumer2.class);
        MessageReceiver messageReceiver = context.getBean(MessageReceiver.class);
        Thread startConsumer1Thread = new Thread(() -> {
            try {
                consumer1.startConsumer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        Thread startConsumer2Thread = new Thread(() -> {
            try {
                consumer2.startConsumer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread startConsumer3Thread = new Thread(() -> {
            try {
                consumer3.startConsumer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        Thread messageListenerThread = new Thread(() -> {
            try {
                messageReceiver.listenForMessages();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        startConsumer3Thread.start();
        startConsumer2Thread.start();
        startConsumer1Thread.start();
        messageListenerThread.start();

    }
}