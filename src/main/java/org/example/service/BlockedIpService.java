package org.example.service;


import org.example.entity.BlockedIp;
import org.example.repository.BlockedIpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BlockedIpService  {

    @Autowired
    private final BlockedIpRepository blockedIpRepository;

    public BlockedIpService(BlockedIpRepository blockedIpRepository) {
        this.blockedIpRepository = blockedIpRepository;
    }

    public void saveBlockedIp(BlockedIp blockedIp)
    {
        try {
            blockedIpRepository.save(blockedIp);
        } catch (Exception e) {
            // Log the exception or handle it
            System.err.println("Error saving IP: " + e.getMessage());
        }
    }

    public BlockedIp getBlockedIp(int id)
    {
        return blockedIpRepository.getBlockedIpById(id);
    }

    public boolean isIpExist(int ip)
    {
        return blockedIpRepository.existsBlockedIpByIp(ip);
    }
}
