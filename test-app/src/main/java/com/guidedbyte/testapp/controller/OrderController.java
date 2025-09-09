package com.guidedbyte.testapp.controller;

import com.guidedbyte.testapp.model.orders.CustomOrderEntity;
import com.guidedbyte.testapp.model.orders.CustomOrderItemEntity;
import com.guidedbyte.testapp.model.orders.CustomAddressEntity;
import com.guidedbyte.testapp.model.orders.CustomPaymentInfoEntity;
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
    public ResponseEntity<CustomOrderEntity> getOrder(@PathVariable Long id) {
        // Create sample order using generated DTOs (with Entity suffix)
        CustomAddressEntity shippingAddress = CustomAddressEntity.builder()
            .street("123 Main St")
            .apartment("Apt 4B")
            .city("Springfield")
            .state("IL")
            .postalCode("62704")
            .country(CustomAddressEntity.CountryEnum.US)
            .build();
        
        List<String> customizations = new ArrayList<>();
        customizations.add("red collar");
        customizations.add("name tag: Max");
        
        CustomOrderItemEntity item = CustomOrderItemEntity.builder()
            .petId(123L)
            .quantity(2)
            .unitPrice(BigDecimal.valueOf(149.99))
            .customizations(customizations)
                .build();
        
        List<CustomOrderItemEntity> items = new ArrayList<>();
        items.add(item);
        
        CustomOrderEntity order = CustomOrderEntity.builder()
            .id(id)
            .customerId(123L)
            .items(items)
            .status(CustomOrderEntity.StatusEnum.PLACED)
            .orderDate(OffsetDateTime.now())
            .shippingAddress(shippingAddress)
            .billingAddress(shippingAddress)
            .totalAmount(BigDecimal.valueOf(299.99))
            .currency(CustomOrderEntity.CurrencyEnum.USD)
            .discountCode("SAVE10")
            .notes("Please deliver to the back door")
                .build();
        
        return ResponseEntity.ok(order);
    }
    
    @PostMapping
    public ResponseEntity<CustomOrderEntity> createOrder(@Valid @RequestBody CustomOrderEntity order) {
        // In a real app, this would save to database
        order.id(System.currentTimeMillis())
             .orderDate(OffsetDateTime.now())
             .status(CustomOrderEntity.StatusEnum.PLACED);
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/{id}/payment")
    public ResponseEntity<CustomPaymentInfoEntity> getPaymentInfo(@PathVariable Long id) {
        CustomPaymentInfoEntity payment = new CustomPaymentInfoEntity()
            .method(CustomPaymentInfoEntity.MethodEnum.CREDIT_CARD)
            .amount(BigDecimal.valueOf(299.99))
            .transactionId("txn_123456789")
            .processedAt(OffsetDateTime.now());
        
        return ResponseEntity.ok(payment);
    }
}