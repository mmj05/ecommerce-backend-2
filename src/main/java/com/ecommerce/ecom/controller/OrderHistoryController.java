// src/main/java/com/ecommerce/ecom/controller/OrderHistoryController.java
package com.ecommerce.ecom.controller;

import com.ecommerce.ecom.exceptions.ResourceNotFoundException;
import com.ecommerce.ecom.model.Order;
import com.ecommerce.ecom.model.OrderItem;
import com.ecommerce.ecom.payload.APIResponse;
import com.ecommerce.ecom.payload.OrderDTO;
import com.ecommerce.ecom.payload.OrderItemDTO;
import com.ecommerce.ecom.repositories.OrderItemRepository;
import com.ecommerce.ecom.repositories.OrderRepository;
import com.ecommerce.ecom.util.AuthUtil;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderHistoryController {
    private static final Logger logger = LoggerFactory.getLogger(OrderHistoryController.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private AuthUtil authUtil;

    // Get all orders for current user
    @GetMapping("/users")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserOrders() {
        try {
            String email = authUtil.loggedInEmail();

            List<Order> orders = orderRepository.findByEmailOrderByOrderDateDesc(email);

            if (orders.isEmpty()) {
                return new ResponseEntity<>(new APIResponse("No orders found for user", false), HttpStatus.NOT_FOUND);
            }

            List<OrderDTO> orderDTOs = orders.stream()
                    .map(order -> modelMapper.map(order, OrderDTO.class))
                    .collect(Collectors.toList());

            return new ResponseEntity<>(orderDTOs, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error fetching user orders: {}", e.getMessage(), e);
            return new ResponseEntity<>(new APIResponse("Error fetching orders: " + e.getMessage(), false), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get order by ID
    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getOrderById(@PathVariable Long orderId) {
        try {
            String email = authUtil.loggedInEmail();

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

            // Ensure user can only see their own orders
            if (!order.getEmail().equals(email)) {
                return new ResponseEntity<>(new APIResponse("You are not authorized to view this order", false), HttpStatus.FORBIDDEN);
            }

            OrderDTO orderDTO = modelMapper.map(order, OrderDTO.class);

            // Map order items
            List<OrderItemDTO> orderItemDTOs = order.getOrderItems().stream()
                    .map(item -> modelMapper.map(item, OrderItemDTO.class))
                    .collect(Collectors.toList());

            orderDTO.setOrderItemDTOs(orderItemDTOs);

            return new ResponseEntity<>(orderDTO, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(new APIResponse(e.getMessage(), false), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error fetching order details: {}", e.getMessage(), e);
            return new ResponseEntity<>(new APIResponse("Error fetching order details: " + e.getMessage(), false), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get order items for an order
    @GetMapping("/{orderId}/items")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getOrderItems(@PathVariable Long orderId) {
        try {
            String email = authUtil.loggedInEmail();

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

            // Ensure user can only see their own orders
            if (!order.getEmail().equals(email)) {
                return new ResponseEntity<>(new APIResponse("You are not authorized to view this order", false), HttpStatus.FORBIDDEN);
            }

            List<OrderItem> orderItems = order.getOrderItems();

            List<OrderItemDTO> orderItemDTOs = orderItems.stream()
                    .map(item -> modelMapper.map(item, OrderItemDTO.class))
                    .collect(Collectors.toList());

            return new ResponseEntity<>(orderItemDTOs, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(new APIResponse(e.getMessage(), false), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error fetching order items: {}", e.getMessage(), e);
            return new ResponseEntity<>(new APIResponse("Error fetching order items: " + e.getMessage(), false), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Cancel order
    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        try {
            String email = authUtil.loggedInEmail();

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

            // Ensure user can only cancel their own orders
            if (!order.getEmail().equals(email)) {
                return new ResponseEntity<>(new APIResponse("You are not authorized to cancel this order", false), HttpStatus.FORBIDDEN);
            }

            // Check if order can be canceled (e.g., not already shipped)
            String currentStatus = order.getOrderStatus().toLowerCase();
            if (currentStatus.contains("deliver") || currentStatus.contains("complet") ||
                    currentStatus.contains("ship") || currentStatus.contains("transit") ||
                    currentStatus.contains("cancel")) {
                return new ResponseEntity<>(new APIResponse("Order cannot be canceled in its current status", false), HttpStatus.BAD_REQUEST);
            }

            // Update order status
            order.setOrderStatus("Canceled");
            orderRepository.save(order);

            return new ResponseEntity<>(new APIResponse("Order canceled successfully", true), HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(new APIResponse(e.getMessage(), false), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error canceling order: {}", e.getMessage(), e);
            return new ResponseEntity<>(new APIResponse("Error canceling order: " + e.getMessage(), false), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}