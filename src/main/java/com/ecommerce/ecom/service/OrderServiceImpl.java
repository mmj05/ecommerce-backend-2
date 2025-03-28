package com.ecommerce.ecom.service;

import com.ecommerce.ecom.exceptions.APIException;
import com.ecommerce.ecom.exceptions.ResourceNotFoundException;
import com.ecommerce.ecom.model.*;
import com.ecommerce.ecom.payload.OrderDTO;
import com.ecommerce.ecom.payload.OrderItemDTO;
import com.ecommerce.ecom.payload.PaymentDTO;
import com.ecommerce.ecom.payload.ProductDTO;
import com.ecommerce.ecom.repositories.*;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    @Transactional
    public OrderDTO placeOrder(String email, Long addressId, String paymentMethod, String pgName, String pgPaymentId, String pgStatus, String pgResponseMessage) {
        Cart cart = cartRepository.findCartByEmail(email);

        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "email", email);
        }

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

        if (cart.getCartItems().isEmpty()) {
            throw new APIException("Cart is empty.");
        }

        // Create Order
        Order order = new Order();
        order.setEmail(email);
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(cart.getTotalPrice());
        order.setOrderStatus("Order Placed.");
        order.setAddress(address);

        // Create Payment
        Payment payment = new Payment(paymentMethod, pgPaymentId, pgStatus, pgResponseMessage, pgName);
        payment.setOrder(order);
        payment = paymentRepository.save(payment);
        order.setPayment(payment);

        Order savedOrder = orderRepository.save(order);

        // Convert Cart Items to Order Items
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();
            int orderedQuantity = cartItem.getQuantity();

            // Ensure sufficient stock
            if (product.getQuantity() < orderedQuantity) {
                throw new APIException("Not enough stock available for product: " + product.getProductName());
            }

            // Reduce stock quantity
            product.setQuantity(product.getQuantity() - orderedQuantity);

            productRepository.save(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(orderedQuantity);
            orderItem.setDiscount(cartItem.getDiscount());
            orderItem.setOrderedProductPrice(cartItem.getProductPrice());
            orderItem.setOrder(savedOrder);
            orderItems.add(orderItem);
        }

        orderItemRepository.saveAll(orderItems);

        // Clear cart after order placement
        cart.getCartItems().forEach(item -> cartService.deleteProductFromCart(cart.getCartId(), item.getProduct().getProductId()));

        // Convert Order to DTO
        OrderDTO orderDTO = modelMapper.map(savedOrder, OrderDTO.class);

        if (savedOrder.getPayment() != null) {
            orderDTO.setPaymentDTO(modelMapper.map(savedOrder.getPayment(), PaymentDTO.class));
        }

        orderItems.forEach(orderItem -> {
            OrderItemDTO itemDTO = modelMapper.map(orderItem, OrderItemDTO.class);
            // Explicitly map the product
            if (orderItem.getProduct() != null) {
                itemDTO.setProductDTO(modelMapper.map(orderItem.getProduct(), ProductDTO.class));
            }
            orderDTO.getOrderItemDTOs().add(itemDTO);
        });

        orderDTO.setAddressId(addressId);

        return orderDTO;
    }
}
