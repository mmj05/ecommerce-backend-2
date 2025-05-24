package com.ecommerce.ecom.controller;

import com.ecommerce.ecom.exceptions.ResourceNotFoundException;
import com.ecommerce.ecom.model.Order;
import com.ecommerce.ecom.model.OrderItem;
import com.ecommerce.ecom.model.Product;
import com.ecommerce.ecom.model.User;
import com.ecommerce.ecom.payload.APIResponse;
import com.ecommerce.ecom.payload.AddressDTO;
import com.ecommerce.ecom.payload.OrderDTO;
import com.ecommerce.ecom.payload.OrderItemDTO;
import com.ecommerce.ecom.payload.PaymentDTO;
import com.ecommerce.ecom.payload.ProductDTO;
import com.ecommerce.ecom.payload.SellerDashboardStatsDTO;
import com.ecommerce.ecom.repositories.OrderItemRepository;
import com.ecommerce.ecom.repositories.OrderRepository;
import com.ecommerce.ecom.repositories.ProductRepository;
import com.ecommerce.ecom.util.AuthUtil;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/seller")
public class SellerOrderController {
    private static final Logger logger = LoggerFactory.getLogger(SellerOrderController.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private AuthUtil authUtil;

    // Get dashboard statistics for seller
    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasRole('ROLE_SELLER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> getSellerDashboardStats() {
        try {
            User seller = authUtil.loggedInUser();

            // Get all products by this seller
            List<Product> sellerProducts = productRepository.findByUser(seller);
            List<Long> productIds = sellerProducts.stream()
                    .map(Product::getProductId)
                    .collect(Collectors.toList());

            if (productIds.isEmpty()) {
                // Return empty stats if seller has no products
                SellerDashboardStatsDTO stats = new SellerDashboardStatsDTO();
                stats.setTotalProducts(0L);
                stats.setTotalOrders(0L);
                stats.setTotalSales(0.0);
                stats.setTotalCustomers(0L);
                return new ResponseEntity<>(stats, HttpStatus.OK);
            }

            // Get all order items for seller's products
            List<OrderItem> sellerOrderItems = orderItemRepository.findByProductProductIdIn(productIds);

            // Calculate statistics
            SellerDashboardStatsDTO stats = new SellerDashboardStatsDTO();
            stats.setTotalProducts((long) sellerProducts.size());

            // Get unique orders
            Set<Long> uniqueOrderIds = sellerOrderItems.stream()
                    .map(item -> item.getOrder().getOrderId())
                    .collect(Collectors.toSet());
            stats.setTotalOrders((long) uniqueOrderIds.size());

            // Calculate total sales
            double totalSales = sellerOrderItems.stream()
                    .mapToDouble(item -> item.getOrderedProductPrice() * item.getQuantity())
                    .sum();
            stats.setTotalSales(totalSales);

            // Get unique customers (by email)
            Set<String> uniqueCustomers = sellerOrderItems.stream()
                    .map(item -> item.getOrder().getEmail())
                    .collect(Collectors.toSet());
            stats.setTotalCustomers((long) uniqueCustomers.size());

            return new ResponseEntity<>(stats, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error fetching seller dashboard stats: {}", e.getMessage(), e);
            return new ResponseEntity<>(new APIResponse("Error fetching dashboard stats: " + e.getMessage(), false),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get orders containing seller's products with pagination
    @GetMapping("/orders")
    @PreAuthorize("hasRole('ROLE_SELLER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> getSellerOrders(
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder) {
        try {
            User seller = authUtil.loggedInUser();

            // Get all products by this seller
            List<Product> sellerProducts = productRepository.findByUser(seller);
            List<Long> productIds = sellerProducts.stream()
                    .map(Product::getProductId)
                    .collect(Collectors.toList());

            if (productIds.isEmpty()) {
                // Return empty result if seller has no products
                Map<String, Object> response = new HashMap<>();
                response.put("content", new ArrayList<>());
                response.put("pageNumber", pageNumber);
                response.put("pageSize", pageSize);
                response.put("totalElements", 0L);
                response.put("totalPages", 0);
                response.put("lastPage", true);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }

            // Get all orders that contain at least one of the seller's products
            Sort sort = sortOrder.equalsIgnoreCase("ASC")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

            Page<Order> ordersPage = orderRepository.findOrdersContainingProducts(productIds, pageable);

            // Convert to DTOs with only seller's products in orderItems
            List<OrderDTO> orderDTOs = ordersPage.getContent().stream()
                    .map(order -> {
                        OrderDTO orderDTO = modelMapper.map(order, OrderDTO.class);

                        // Explicitly map Payment to PaymentDTO
                        if (order.getPayment() != null) {
                            PaymentDTO paymentDTO = modelMapper.map(order.getPayment(), PaymentDTO.class);
                            orderDTO.setPaymentDTO(paymentDTO);
                        }

                        // Explicitly map Address to AddressDTO
                        if (order.getAddress() != null) {
                            AddressDTO addressDTO = modelMapper.map(order.getAddress(), AddressDTO.class);
                            orderDTO.setAddressDTO(addressDTO);
                        }

                        // Filter order items to include only seller's products with detailed mapping
                        List<OrderItemDTO> sellerOrderItems = order.getOrderItems().stream()
                                .filter(item -> productIds.contains(item.getProduct().getProductId()))
                                .map(item -> {
                                    OrderItemDTO itemDTO = new OrderItemDTO();
                                    itemDTO.setOrderItemId(item.getOrderItemId());
                                    itemDTO.setQuantity(item.getQuantity());
                                    itemDTO.setDiscount(item.getDiscount());
                                    itemDTO.setOrderedProductPrice(item.getOrderedProductPrice());
                                    
                                    // Manually map the product to ensure all fields are included
                                    if (item.getProduct() != null) {
                                        ProductDTO productDTO = new ProductDTO();
                                        productDTO.setProductId(item.getProduct().getProductId());
                                        productDTO.setProductName(item.getProduct().getProductName());
                                        productDTO.setDescription(item.getProduct().getDescription());
                                        productDTO.setPrice(item.getProduct().getPrice());
                                        productDTO.setSpecialPrice(item.getProduct().getSpecialPrice());
                                        productDTO.setImage(item.getProduct().getImage());
                                        
                                        itemDTO.setProductDTO(productDTO);
                                    }
                                    
                                    return itemDTO;
                                })
                                .collect(Collectors.toList());

                        orderDTO.setOrderItemDTOs(sellerOrderItems);

                        // Calculate total for seller's products only
                        double sellerTotal = sellerOrderItems.stream()
                                .mapToDouble(item -> item.getOrderedProductPrice() * item.getQuantity())
                                .sum();
                        orderDTO.setSellerTotal(sellerTotal);

                        return orderDTO;
                    })
                    .collect(Collectors.toList());

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("content", orderDTOs);
            response.put("pageNumber", ordersPage.getNumber());
            response.put("pageSize", ordersPage.getSize());
            response.put("totalElements", ordersPage.getTotalElements());
            response.put("totalPages", ordersPage.getTotalPages());
            response.put("lastPage", ordersPage.isLast());

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error fetching seller orders: {}", e.getMessage(), e);
            return new ResponseEntity<>(new APIResponse("Error fetching orders: " + e.getMessage(), false),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get specific order details (only if it contains seller's products)
    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasRole('ROLE_SELLER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> getSellerOrderById(@PathVariable Long orderId) {
        try {
            User seller = authUtil.loggedInUser();

            Order order = orderRepository.findByIdWithDetails(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

            // Get seller's product IDs
            List<Product> sellerProducts = productRepository.findByUser(seller);
            List<Long> productIds = sellerProducts.stream()
                    .map(Product::getProductId)
                    .collect(Collectors.toList());

            // Check if order contains any of seller's products
            boolean hasSellerProducts = order.getOrderItems().stream()
                    .anyMatch(item -> productIds.contains(item.getProduct().getProductId()));

            if (!hasSellerProducts) {
                return new ResponseEntity<>(new APIResponse("You don't have access to this order", false),
                        HttpStatus.FORBIDDEN);
            }

            OrderDTO orderDTO = modelMapper.map(order, OrderDTO.class);

            // Explicitly map Payment to PaymentDTO
            if (order.getPayment() != null) {
                PaymentDTO paymentDTO = modelMapper.map(order.getPayment(), PaymentDTO.class);
                orderDTO.setPaymentDTO(paymentDTO);
            }

            // Explicitly map Address to AddressDTO
            if (order.getAddress() != null) {
                AddressDTO addressDTO = modelMapper.map(order.getAddress(), AddressDTO.class);
                orderDTO.setAddressDTO(addressDTO);
            }

            // Filter order items to include only seller's products with detailed mapping
            List<OrderItemDTO> sellerOrderItems = order.getOrderItems().stream()
                    .filter(item -> productIds.contains(item.getProduct().getProductId()))
                    .map(item -> {
                        OrderItemDTO itemDTO = new OrderItemDTO();
                        itemDTO.setOrderItemId(item.getOrderItemId());
                        itemDTO.setQuantity(item.getQuantity());
                        itemDTO.setDiscount(item.getDiscount());
                        itemDTO.setOrderedProductPrice(item.getOrderedProductPrice());
                        
                        // Manually map the product to ensure all fields are included
                        if (item.getProduct() != null) {
                            ProductDTO productDTO = new ProductDTO();
                            productDTO.setProductId(item.getProduct().getProductId());
                            productDTO.setProductName(item.getProduct().getProductName());
                            productDTO.setDescription(item.getProduct().getDescription());
                            productDTO.setPrice(item.getProduct().getPrice());
                            productDTO.setSpecialPrice(item.getProduct().getSpecialPrice());
                            productDTO.setImage(item.getProduct().getImage());
                            
                            itemDTO.setProductDTO(productDTO);
                            
                            // Debug log
                            logger.info("Mapped product: ID={}, Name={}", 
                                item.getProduct().getProductId(), 
                                item.getProduct().getProductName());
                        } else {
                            logger.warn("Product is null for order item ID: {}", item.getOrderItemId());
                        }
                        
                        return itemDTO;
                    })
                    .collect(Collectors.toList());

            orderDTO.setOrderItemDTOs(sellerOrderItems);

            // Calculate total for seller's products only
            double sellerTotal = sellerOrderItems.stream()
                    .mapToDouble(item -> item.getOrderedProductPrice() * item.getQuantity())
                    .sum();
            orderDTO.setSellerTotal(sellerTotal);

            return new ResponseEntity<>(orderDTO, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(new APIResponse(e.getMessage(), false), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error fetching order details: {}", e.getMessage(), e);
            return new ResponseEntity<>(new APIResponse("Error fetching order details: " + e.getMessage(), false),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Update order status (seller can only update to "Shipped")
    @PutMapping("/orders/{orderId}/status")
    @PreAuthorize("hasRole('ROLE_SELLER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {
        try {
            User seller = authUtil.loggedInUser();

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

            // Get seller's product IDs
            List<Product> sellerProducts = productRepository.findByUser(seller);
            List<Long> productIds = sellerProducts.stream()
                    .map(Product::getProductId)
                    .collect(Collectors.toList());

            // Check if order contains any of seller's products
            boolean hasSellerProducts = order.getOrderItems().stream()
                    .anyMatch(item -> productIds.contains(item.getProduct().getProductId()));

            if (!hasSellerProducts) {
                return new ResponseEntity<>(new APIResponse("You don't have access to this order", false),
                        HttpStatus.FORBIDDEN);
            }

            // Sellers can only update to "Shipped" status
            if (!status.equalsIgnoreCase("Shipped") && !authUtil.loggedInUser().getRoles().stream()
                    .anyMatch(role -> role.getRoleName().name().equals("ROLE_ADMIN"))) {
                return new ResponseEntity<>(new APIResponse("Sellers can only update order status to 'Shipped'", false),
                        HttpStatus.BAD_REQUEST);
            }

            // Check current status
            String currentStatus = order.getOrderStatus().toLowerCase();
            if (currentStatus.contains("deliver") || currentStatus.contains("complet") ||
                    currentStatus.contains("cancel")) {
                return new ResponseEntity<>(new APIResponse("Cannot update order in its current status", false),
                        HttpStatus.BAD_REQUEST);
            }

            // Update order status
            order.setOrderStatus(status);
            orderRepository.save(order);

            return new ResponseEntity<>(new APIResponse("Order status updated successfully", true), HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(new APIResponse(e.getMessage(), false), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error updating order status: {}", e.getMessage(), e);
            return new ResponseEntity<>(new APIResponse("Error updating order status: " + e.getMessage(), false),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}