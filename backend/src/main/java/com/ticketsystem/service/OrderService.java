package com.ticketsystem.service;

import com.ticketsystem.dto.OrderResponse;
import com.ticketsystem.dto.ReserveRequest;
import com.ticketsystem.entity.*;
import com.ticketsystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Scheduled;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EventSeatRepository eventSeatRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final StripeService stripeService;
    // private final QrCodeService qrCodeService;
    // private final PdfService pdfService;
    // private final EmailService emailService;

    /**
     * Reserve seats - lock with PESSIMISTIC_WRITE
     */
    @Transactional
    public OrderResponse reserveSeats(String username, ReserveRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User không tồn tại!"));

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Sự kiện không tồn tại!"));

        List<EventSeat> seatsToReserve = new ArrayList<>();
        double totalAmount = 0;

        for (Long seatId : request.getEventSeatIds()) {
            // Lock seat with PESSIMISTIC_WRITE
            EventSeat eventSeat = eventSeatRepository.findByIdWithLock(seatId)
                    .orElseThrow(() -> new RuntimeException("Ghế không tồn tại: " + seatId));

            if (eventSeat.getStatus() != SeatStatus.AVAILABLE) {
                throw new RuntimeException("Ghế " + eventSeat.getSeat().getRowLabel() +
                        eventSeat.getSeat().getColNumber() + " đã được đặt!");
            }

            eventSeat.setStatus(SeatStatus.RESERVED);
            seatsToReserve.add(eventSeat);
            totalAmount += eventSeat.getPrice();
        }

        eventSeatRepository.saveAll(seatsToReserve);

        // Create order
        Order order = Order.builder()
                .user(user)
                .event(event)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .build();

        order = orderRepository.save(order);

        // Create order items
        List<OrderItem> orderItems = new ArrayList<>();
        for (EventSeat seat : seatsToReserve) {
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .eventSeat(seat)
                    .price(seat.getPrice())
                    .build();
            orderItems.add(item);
        }
        orderItemRepository.saveAll(orderItems);
        order.setOrderItems(orderItems);

        // Generate Stripe Checkout URL
        String seats = order.getOrderItems().stream()
                .map(item -> item.getEventSeat().getSeat().getRowLabel() +
                        item.getEventSeat().getSeat().getColNumber())
                .collect(Collectors.joining(", "));
        try {
            String paymentUrl = stripeService.createCheckoutSession(
                    order.getId(), totalAmount, event.getTitle(), seats);
            OrderResponse response = toResponse(order);
            response.setPaymentUrl(paymentUrl);
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo phiên thanh toán Stripe: " + e.getMessage());
        }
    }

    /**
     * Process payment after Stripe/simulated return
     */
    @Transactional
    public OrderResponse processPayment(Long orderId, String sessionId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại!"));

        if (order.getStatus() == OrderStatus.PAID) {
            // Already processed - return success (handles Stripe redirect retry)
            return toResponse(order);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Đơn hàng đã được xử lý!");
        }

        // Update order status
        order.setStatus(OrderStatus.PAID);
        order.setPaymentTime(LocalDateTime.now());
        order.setStripeSessionId(sessionId);

        // Update seats to SOLD
        for (OrderItem item : order.getOrderItems()) {
            EventSeat seat = item.getEventSeat();
            seat.setStatus(SeatStatus.SOLD);
            eventSeatRepository.save(seat);
        }

        // Generate QR Code
        String seats = order.getOrderItems().stream()
                .map(item -> item.getEventSeat().getSeat().getRowLabel() +
                        item.getEventSeat().getSeat().getColNumber())
                .collect(Collectors.joining(","));

        String qrContent = order.getId() + "|" +
                order.getEvent().getId() + "|" +
                order.getUser().getId() + "|" +
                seats + "|" +
                UUID.randomUUID().toString().substring(0, 8);

        //String qrPath = qrCodeService.generateQrCode(qrContent, "order_" + order.getId());
        //order.setQrCodePath(qrPath);

        // Generate PDF
        //String pdfPath = pdfService.generateTicketPdf(order, qrContent);
        //order.setTicketPdfPath(pdfPath);

        orderRepository.save(order);

        // Send email
        System.out.println("\n\n########## EMAIL SENDING START ##########");
        System.out.println("User email: " + order.getUser().getEmail());
        System.out.println("User fullName: " + order.getUser().getFullName());
        //System.out.println("PDF path: " + pdfPath);
        try {
            Map<String, Object> emailVars = new HashMap<>();
            emailVars.put("eventTitle", order.getEvent().getTitle());
            emailVars.put("eventDate", order.getEvent().getEventDate());
            emailVars.put("venueName", order.getEvent().getVenue().getName());
            emailVars.put("seats", seats);
            emailVars.put("fullName", order.getUser().getFullName());
            emailVars.put("totalAmount", order.getTotalAmount());
            emailVars.put("orderId", order.getId());

            System.out.println("Calling emailService.sendTicketEmail...");
            // emailService.sendTicketEmail(
            //         order.getUser().getEmail(),
            //         "Vé sự kiện: " + order.getEvent().getTitle(),
            //         emailVars,
            //         pdfPath);
            System.out.println("########## EMAIL SENT OK ##########\n\n");
        } catch (Exception e) {
            System.out.println("########## EMAIL FAILED ##########");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace(System.out);
            System.out.println("##################################\n\n");
        }

        return toResponse(order);
    }

    /**
     * Người dùng tự hủy đơn hàng chưa thanh toán
     */
    @Transactional
    public OrderResponse cancelOrder(Long orderId, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại!"));

        if (!order.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Bạn không có quyền tác động lên đơn hàng này!");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể hủy đơn hàng đang chờ thanh toán!");
        }

        order.setStatus(OrderStatus.CANCELLED);

        // Release seats back to AVAILABLE
        for (OrderItem item : order.getOrderItems()) {
            EventSeat seat = item.getEventSeat();
            seat.setStatus(SeatStatus.AVAILABLE);
            eventSeatRepository.save(seat);
        }

        order = orderRepository.save(order);
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public String generatePaymentUrl(Long orderId, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại!"));

        if (!order.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Bạn không có quyền tác động lên đơn hàng này!");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể thanh toán cho đơn hàng đang chờ!");
        }

        String seats = order.getOrderItems().stream()
                .map(item -> item.getEventSeat().getSeat().getRowLabel() +
                        item.getEventSeat().getSeat().getColNumber())
                .collect(Collectors.joining(", "));
        try {
            return stripeService.createCheckoutSession(
                    order.getId(), order.getTotalAmount(), order.getEvent().getTitle(), seats);
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo phiên thanh toán Stripe: " + e.getMessage());
        }
    }

    /**
     * Cron Job tự động bắt các đơn hàng treo (PENDING) đã tạo quá 5 phút để hủy.
     * Chạy mỗi 1 phút (60,000 ms).
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoCancelUnpaidOrders() {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING,
                fiveMinutesAgo);

        if (expiredOrders.isEmpty())
            return;

        System.out.println("\n[CRON] Khởi chạy auto-cancel. Tìm thấy " + expiredOrders.size()
                + " đơn hàng hết hạn.");

        for (Order order : expiredOrders) {
            order.setStatus(OrderStatus.CANCELLED);

            // Release seats
            for (OrderItem item : order.getOrderItems()) {
                EventSeat seat = item.getEventSeat();
                seat.setStatus(SeatStatus.AVAILABLE);
                eventSeatRepository.save(seat);
            }
            orderRepository.save(order);
            System.out.println("[CRON] Đã hủy đơn hàng ID: " + order.getId());
        }
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User không tồn tại!"));
        return orderRepository.findByUserId(user.getId()).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại!"));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public String getTicketPdfPath(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại!"));
        if (order.getStatus() != OrderStatus.PAID) {
            throw new RuntimeException("Đơn hàng chưa thanh toán!");
        }
        return order.getTicketPdfPath();
    }

    @Transactional(readOnly = true)
    public String getQrCodePath(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại!"));
        if (order.getStatus() != OrderStatus.PAID) {
            throw new RuntimeException("Đơn hàng chưa thanh toán!");
        }
        return order.getQrCodePath();
    }

    private OrderResponse toResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setEventId(order.getEvent().getId());
        response.setEventTitle(order.getEvent().getTitle());
        response.setVenueName(order.getEvent().getVenue().getName());
        response.setEventDate(order.getEvent().getEventDate());
        response.setStatus(order.getStatus().name());
        response.setTotalAmount(order.getTotalAmount());
        response.setPaymentTime(order.getPaymentTime());
        response.setCheckInTime(order.getCheckInTime());
        response.setCreatedAt(order.getCreatedAt());

        if (order.getOrderItems() != null) {
            List<String> seatNames = order.getOrderItems().stream()
                    .map(item -> item.getEventSeat().getSeat().getRowLabel() +
                            item.getEventSeat().getSeat().getColNumber())
                    .toList();
            response.setSeats(seatNames);
        }

        return response;
    }
}
