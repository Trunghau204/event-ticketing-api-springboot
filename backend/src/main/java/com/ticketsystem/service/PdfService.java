package com.ticketsystem.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.ticketsystem.entity.Order;
import com.ticketsystem.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PdfService {

    @Value("${ticket.storage-path}")
    private String storagePath;

    private final QrCodeService qrCodeService;

    public String generateTicketPdf(Order order, String qrContent) {
        try {
            Path directory = Paths.get(storagePath, "pdfs");
            Files.createDirectories(directory);

            String fileName = "ticket_" + order.getId() + ".pdf";
            Path filePath = directory.resolve(fileName);

            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(filePath.toFile()));
            document.open();

            // Title
            Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, new Color(41, 128, 185));
            Paragraph title = new Paragraph("VÉ SỰ KIỆN", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Divider
            Paragraph divider = new Paragraph("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            divider.setAlignment(Element.ALIGN_CENTER);
            divider.setSpacingAfter(15);
            document.add(divider);

            // Event info table
            Font labelFont = new Font(Font.HELVETICA, 12, Font.BOLD, Color.DARK_GRAY);
            Font valueFont = new Font(Font.HELVETICA, 12, Font.NORMAL, Color.BLACK);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(90);
            table.setWidths(new float[] { 1, 2 });
            table.setSpacingAfter(20);

            addTableRow(table, "Ma don hang:", String.valueOf(order.getId()), labelFont, valueFont);
            addTableRow(table, "Su kien:", order.getEvent().getTitle(), labelFont, valueFont);
            addTableRow(table, "Thoi gian:",
                    order.getEvent().getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    labelFont, valueFont);
            addTableRow(table, "Dia diem:",
                    order.getEvent().getVenue().getName() + " - " + order.getEvent().getVenue().getAddress(),
                    labelFont, valueFont);

            String seats = order.getOrderItems().stream()
                    .map(item -> item.getEventSeat().getSeat().getRowLabel()
                            + item.getEventSeat().getSeat().getColNumber())
                    .collect(Collectors.joining(", "));
            addTableRow(table, "Ghe:", seats, labelFont, valueFont);

            addTableRow(table, "Nguoi mua:", order.getUser().getFullName(), labelFont, valueFont);
            addTableRow(table, "Email:", order.getUser().getEmail(), labelFont, valueFont);
            addTableRow(table, "Tong tien:", String.format("%,.0f VND", order.getTotalAmount()), labelFont, valueFont);

            document.add(table);

            // QR Code
            byte[] qrBytes = qrCodeService.generateQrCodeBytes(qrContent);
            Image qrImage = Image.getInstance(qrBytes);
            qrImage.scaleToFit(200, 200);
            qrImage.setAlignment(Element.ALIGN_CENTER);
            document.add(qrImage);

            Paragraph qrNote = new Paragraph("Vui long xuat trinh ma QR khi check-in",
                    new Font(Font.HELVETICA, 10, Font.ITALIC, Color.GRAY));
            qrNote.setAlignment(Element.ALIGN_CENTER);
            qrNote.setSpacingBefore(10);
            document.add(qrNote);

            document.close();

            return filePath.toString();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Khong the tao PDF: " + e.getMessage());
        }
    }

    private void addTableRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(0);
        labelCell.setPadding(5);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(0);
        valueCell.setPadding(5);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }
}
