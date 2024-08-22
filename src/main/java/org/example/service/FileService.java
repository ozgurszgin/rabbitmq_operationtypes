package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Service
public class FileService {
    private static final String FILE_PATH = "E:\\blocked_ip\\src\\main\\resources\\";
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    public synchronized void writeToFile(String content,String filename) {
        filename=FILE_PATH+filename;
        File file = new File(filename);

        if (!file.exists()) {
            try {
                file.createNewFile();
                logger.info("Yeni dosya oluşturuldu: " + filename);
            } catch (IOException e) {
                logger.error("Dosya oluşturulurken hata meydana geldi: " + filename, e);
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) {
            writer.write(content);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
