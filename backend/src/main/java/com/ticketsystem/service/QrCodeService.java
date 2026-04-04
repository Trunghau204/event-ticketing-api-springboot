package com.ticketsystem.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class QrCodeService {

    @Value("${ticket.storage-path}")
    private String storagePath;

    public String generateQrCode(String content, String fileName) {
        try {
            Path directory = Paths.get(storagePath, "qrcodes");
            Files.createDirectories(directory);

            Path filePath = directory.resolve(fileName + ".png");

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 500, 500);
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", filePath);

            return filePath.toString();
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Không thể tạo QR Code: " + e.getMessage());
        }
    }

    public byte[] generateQrCodeBytes(String content) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 500, 500);

            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Không thể tạo QR Code: " + e.getMessage());
        }
    }
}
