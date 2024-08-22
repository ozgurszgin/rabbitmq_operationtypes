package org.example.service;

import org.example.entity.BlockedIp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {


    @Autowired
    BlockedIpService blockedIpService;
    public void addBlockedIp() throws InterruptedException {
        int ip=1100000;
        int i=1;
        while(i<100) {
            ip++;
            BlockedIp blockedIp = new BlockedIp();
            blockedIp.setIp(ip);
            blockedIpService.saveBlockedIp(blockedIp);
            i++;
        }

    }
}
