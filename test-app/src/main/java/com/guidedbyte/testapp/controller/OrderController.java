package com.guidedbyte.testapp.controller;

import com.guidedbyte.testapp.model.orders.OrderEntity;
import com.guidedbyte.testapp.model.orders.OrderItemEntity;
import com.guidedbyte.testapp.model.orders.AddressEntity;
import com.guidedbyte.testapp.model.orders.PaymentInfoEntity;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderEntity> getOrder(@PathVariable Long id) {
        // Create sample order using generated DTOs (with Entity suffix)
        AddressEntity shippingAddress = AddressEntity.builder()
            .street("123 Main St")
            .apartment("Apt 4B")
            .city("Springfield")
            .state("IL")
            .postalCode("62704")
            .country(AddressEntity.CountryEnum.US)
            .build();
        
        List<String> customizations = new ArrayList<>();
        customizations.add("red collar");
        customizations.add("name tag: Max");
        
        OrderItemEntity item = OrderItemEntity.builder()
            .petId(123L)
            .quantity(2)
            .unitPrice(BigDecimal.valueOf(149.99))
            .customizations(customizations)
                .build();
        
        List<OrderItemEntity> items = new ArrayList<>();
        items.add(item);
        
        OrderEntity order = OrderEntity.builder()
            .id(id)
            .customerId(123L)
            .items(items)
            .status(OrderEntity.StatusEnum.PLACED)
            .orderDate(OffsetDateTime.now())
            .shippingAddress(shippingAddress)
            .billingAddress(shippingAddress)
            .totalAmount(BigDecimal.valueOf(299.99))
            .currency(OrderEntity.CurrencyEnum.USD)
            .discountCode("SAVE10")
            .notes("Please deliver to the back door")
                .build();
        
        return ResponseEntity.ok(order);
    }
    
    @PostMapping
    public ResponseEntity<OrderEntity> createOrder(@Valid @RequestBody OrderEntity order) {
        // In a real app, this would save to database
        order.id(System.currentTimeMillis())
             .orderDate(OffsetDateTime.now())
             .status(OrderEntity.StatusEnum.PLACED);
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/{id}/payment")
    public ResponseEntity<PaymentInfoEntity> getPaymentInfo(@PathVariable Long id) {
        PaymentInfoEntity payment = new PaymentInfoEntity()
            .method(PaymentInfoEntity.MethodEnum.CREDIT_CARD)
            .amount(BigDecimal.valueOf(299.99))
            .transactionId("txn_123456789")
            .processedAt(OffsetDateTime.now());
        
        return ResponseEntity.ok(payment);
    }
}