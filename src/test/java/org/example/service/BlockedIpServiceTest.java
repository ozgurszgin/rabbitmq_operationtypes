package org.example.service;

import org.example.entity.BlockedIp;
import org.example.repository.BlockedIpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BlockedIpServiceTest {

    @Mock
    private BlockedIpRepository blockedIpRepository;

    @InjectMocks
    private BlockedIpService blockedIpService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void saveBlockedIp_ShouldSaveIp() {
        BlockedIp blockedIp = new BlockedIp();
        blockedIp.setIp(123456);

        blockedIpService.saveBlockedIp(blockedIp);

        verify(blockedIpRepository, times(1)).save(blockedIp);
    }

    @Test
    void saveBlockedIp_ShouldHandleException() {
        // Arrange
        BlockedIp blockedIp = new BlockedIp();
        blockedIp.setIp(123456);

        doThrow(new RuntimeException("Database error")).when(blockedIpRepository).save(any(BlockedIp.class));

        // Act & Assert
        assertDoesNotThrow(() -> blockedIpService.saveBlockedIp(blockedIp));
    }

    @Test
    void getBlockedIp_ShouldReturnBlockedIp() {
        // Arrange
        int id = 1;
        BlockedIp blockedIp = new BlockedIp();
        blockedIp.setId(id);
        when(blockedIpRepository.getBlockedIpById(id)).thenReturn(blockedIp);

        // Act
        BlockedIp result = blockedIpService.getBlockedIp(id);

        // Assert
        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(blockedIpRepository, times(1)).getBlockedIpById(id);
    }

    @Test
    void isIpExist_ShouldReturnTrueIfIpExists() {
        // Arrange
        int ip = 123456;
        when(blockedIpRepository.existsBlockedIpByIp(ip)).thenReturn(true);

        // Act
        boolean exists = blockedIpService.isIpExist(ip);

        // Assert
        assertTrue(exists);
        verify(blockedIpRepository, times(1)).existsBlockedIpByIp(ip);
    }

    @Test
    void isIpExist_ShouldReturnFalseIfIpDoesNotExist() {
        // Arrange
        int ip = 123456;
        when(blockedIpRepository.existsBlockedIpByIp(ip)).thenReturn(false);

        // Act
        boolean exists = blockedIpService.isIpExist(ip);

        // Assert
        assertFalse(exists);
        verify(blockedIpRepository, times(1)).existsBlockedIpByIp(ip);
    }

    @Test
    void getLastBlockedIp_ShouldReturnLastBlockedIp() {
        // Arrange
        BlockedIp lastBlockedIp = new BlockedIp();
        lastBlockedIp.setIp(987654);
        when(blockedIpRepository.findTopByOrderByIdDesc()).thenReturn(lastBlockedIp);

        // Act
        BlockedIp result = blockedIpService.getLastBlockedIp();

        // Assert
        assertNotNull(result);
        assertEquals(lastBlockedIp.getIp(), result.getIp());
        verify(blockedIpRepository, times(1)).findTopByOrderByIdDesc();
    }
}
