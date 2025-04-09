package com.ecommerce.ecom.controller;

import com.ecommerce.ecom.model.Cart;
import com.ecommerce.ecom.payload.APIResponse;
import com.ecommerce.ecom.payload.CartDTO;
import com.ecommerce.ecom.repositories.CartRepository;
import com.ecommerce.ecom.service.CartService;
import com.ecommerce.ecom.util.AuthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private AuthUtil authUtil;

    @PostMapping("/carts/products/{productId}/quantity/{quantity}")
    public ResponseEntity<CartDTO> addProductToCart(@PathVariable Long productId, @PathVariable Integer quantity) {
        CartDTO cartDTO = cartService.addProductToCart(productId, quantity);
        return new ResponseEntity<>(cartDTO, HttpStatus.CREATED);
    }

    @GetMapping("/carts")
    public ResponseEntity<List<CartDTO>> getCart() {
        List<CartDTO> cartDTOs = cartService.getAllCarts();
        return new ResponseEntity<List<CartDTO>>(cartDTOs, HttpStatus.OK);
    }

    @GetMapping("/carts/users/cart")
    public ResponseEntity<CartDTO> getCartById() {
        String emailId = authUtil.loggedInEmail();
        Cart cart = cartRepository.findCartByEmail(emailId);

        // If no cart exists for the user, return an empty cart
        if (cart == null) {
            return new ResponseEntity<>(new CartDTO(), HttpStatus.OK);
        }

        Long cartId = cart.getCartId();
        CartDTO cartDTO = cartService.getCart(emailId, cartId);
        return new ResponseEntity<>(cartDTO, HttpStatus.OK);
    }

    @PutMapping("/cart/products/{productId}/quantity/{operation}")
    public ResponseEntity<CartDTO> updateCartProduct(@PathVariable Long productId,
                                                     @PathVariable String operation) {
        CartDTO cartDTO = cartService.updateProductQuantityInCart(productId, operation);

        // Check if the cart is empty after updating and delete if needed
        if (cartDTO.getProducts() != null && cartDTO.getProducts().isEmpty()) {
            // Delete the empty cart
            String emailId = authUtil.loggedInEmail();
            Cart cart = cartRepository.findCartByEmail(emailId);
            if (cart != null) {
                cartRepository.delete(cart);
            }
        }

        return new ResponseEntity<CartDTO>(cartDTO, HttpStatus.OK);
    }

    @DeleteMapping("/carts/{cartId}/product/{productId}")
    public ResponseEntity<String> deleteProductFromCart(@PathVariable Long cartId,
                                                        @PathVariable Long productId) {
        String status = cartService.deleteProductFromCart(cartId, productId);

        // Check if the cart is empty after deletion and delete if needed
        String emailId = authUtil.loggedInEmail();
        Cart cart = cartRepository.findCartByEmail(emailId);

        if (cart != null && (cart.getCartItems() == null || cart.getCartItems().isEmpty())) {
            cartRepository.delete(cart);
            status = "Product removed and empty cart deleted";
        }

        return new ResponseEntity<String>(status, HttpStatus.OK);
    }

    @DeleteMapping("/carts/empty")
    public ResponseEntity<?> deleteEmptyCart() {
        try {
            String email = authUtil.loggedInEmail();
            logger.info("Request to delete empty cart for user: {}", email);

            String result = cartService.deleteEmptyCart(email);

            logger.info("Empty cart deletion result: {}", result);
            return new ResponseEntity<>(new APIResponse(result, true), HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error deleting empty cart: {}", e.getMessage(), e);
            return new ResponseEntity<>(
                    new APIResponse("Error deleting empty cart: " + e.getMessage(), false),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}