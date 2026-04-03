package com.ticketsystem.controller;

import com.ticketsystem.dto.OrderResponse;
import com.ticketsystem.dto.ReserveRequest;
import com.ticketsystem.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/reserve")
    public ResponseEntity<OrderResponse> reserveSeats(
            Authentication authentication,
            @Valid @RequestBody ReserveRequest request) {
        return ResponseEntity.ok(orderService.reserveSeats(authentication.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication authentication) {
        return ResponseEntity.ok(orderService.getUserOrders(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    /**
     * Thanh toán giả lập
     */
    @PostMapping("/{id}/simulate-pay")
    public ResponseEntity<OrderResponse> simulatePayment(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.processPayment(id, "SIMULATED_" + System.currentTimeMillis()));
    }

    /**
     * Hủy đơn hàng chưa thanh toán
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(orderService.cancelOrder(id, authentication.getName()));
    }

    /**
     * Lấy lại URL thanh toán Stripe cho đơn hàng chưa thanh toán
     */
    @GetMapping("/{id}/payment-url")
    public ResponseEntity<?> getPaymentUrl(@PathVariable Long id, Authentication authentication) {
        String url = orderService.generatePaymentUrl(id, authentication.getName());
        return ResponseEntity.ok(java.util.Map.of("url", url));
    }

    /**
     * Download vé PDF
     */
    @GetMapping("/{id}/download-ticket")
    public ResponseEntity<Resource> downloadTicket(@PathVariable Long id) {
        String pdfPath = orderService.getTicketPdfPath(id);
        if (pdfPath == null) {
            return ResponseEntity.notFound().build();
        }
        File file = new File(pdfPath);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ticket_" + id + ".pdf")
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    /**
     * Xem mã QR code
     */
    @GetMapping("/{id}/qrcode")
    public ResponseEntity<Resource> getQrCode(@PathVariable Long id) {
        String qrPath = orderService.getQrCodePath(id);
        if (qrPath == null) {
            return ResponseEntity.notFound().build();
        }
        File file = new File(qrPath);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(resource);
    }
}
