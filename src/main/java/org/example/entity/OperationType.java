package org.example.entity;

public enum OperationType {
    ADD,    // Yeni IP'leri ekle
    DELETE, // IP'leri sil
    REINITIALIZE,  // Tüm IP'leri yeniden yükle

    getAll,
    update
}
