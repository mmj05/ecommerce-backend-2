package com.ecommerce.ecom.controller;

import com.ecommerce.ecom.exceptions.ResourceNotFoundException;
import com.ecommerce.ecom.model.Order;
import com.ecommerce.ecom.payload.OrderDTO;
import com.ecommerce.ecom.payload.OrderRequestDTO;
import com.ecommerce.ecom.repositories.OrderRepository;
import com.ecommerce.ecom.service.OrderService;
import com.ecommerce.ecom.util.AuthUtil;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private AuthUtil authUtil;

    @PostMapping("/order/users/payments/{paymentMethod}")
    public ResponseEntity<OrderDTO> orderProducts(@PathVariable String paymentMethod, @RequestBody OrderRequestDTO orderRequestDTO) {
        logger.info("Received order request with payment method: {}", paymentMethod);

        String email;
        try {
            email = authUtil.loggedInEmail();
            logger.info("Authenticated email: {}", email);
        } catch (Exception e) {
            logger.error("Authentication error in orderProducts: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            OrderDTO orderDTO = orderService.placeOrder(
                    email,
                    orderRequestDTO.getAddressId(),
                    paymentMethod,
                    orderRequestDTO.getPgName(),
                    orderRequestDTO.getPgPaymentId(),
                    orderRequestDTO.getPgStatus(),
                    orderRequestDTO.getPgResponseMessage()
            );

            logger.info("Order created successfully for email {} with order ID: {}",
                    email, orderDTO.getOrderId());

            return new ResponseEntity<>(orderDTO, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error creating order: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/order/users/all")
    public ResponseEntity<List<OrderDTO>> getUserOrders() {
        String email;
        try {
            email = authUtil.loggedInEmail();
            logger.info("Looking up orders for email: {}", email);
        } catch (Exception e) {
            logger.error("Authentication error in getUserOrders: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            // Find all orders for the current user's email
            List<Order> orders = orderRepository.findByEmail(email);
            logger.info("Found {} orders for user {}", orders.size(), email);

            if (orders.isEmpty()) {
                // Return empty list with OK status
                logger.info("No orders found for {}", email);
                return new ResponseEntity<>(List.of(), HttpStatus.OK);
            }

            // Convert orders to DTOs
            List<OrderDTO> orderDTOs = orders.stream()
                    .map(order -> {
                        OrderDTO dto = modelMapper.map(order, OrderDTO.class);
                        // Log each order ID for debugging
                        logger.debug("Mapped order ID: {}", dto.getOrderId());
                        return dto;
                    })
                    .collect(Collectors.toList());

            return new ResponseEntity<>(orderDTOs, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving orders: {}", e.getMessage(), e);
            // Return empty list with OK status on error
            return new ResponseEntity<>(List.of(), HttpStatus.OK);
        }
    }

    @GetMapping("/order/users/{orderId}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long orderId) {
        logger.info("Request to get order details for ID: {}", orderId);

        String email;
        try {
            email = authUtil.loggedInEmail();
            logger.info("Authenticated email: {}", email);
        } catch (Exception e) {
            logger.error("Authentication error in getOrderById: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            // Find specific order for the current user
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> {
                        logger.error("Order not found with ID: {}", orderId);
                        return new ResourceNotFoundException("Order", "id", orderId);
                    });

            // Verify that the order belongs to the current user
            if (!order.getEmail().equals(email)) {
                logger.error("Order {} belongs to {} but was requested by {}",
                        orderId, order.getEmail(), email);
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            OrderDTO orderDTO = modelMapper.map(order, OrderDTO.class);
            logger.info("Returning order details for ID: {}", orderId);

            return new ResponseEntity<>(orderDTO, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            logger.error("Order not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error retrieving order {}: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }
}