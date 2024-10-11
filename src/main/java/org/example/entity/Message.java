package org.example.entity;

import lombok.Data;

@Data
public class Message {

    OperationType operationType;
    String queueNamePattern;
    int lastIpId;
}
