package com.ecommerce.ecom.service;

import com.ecommerce.ecom.exceptions.APIException;
import com.ecommerce.ecom.exceptions.ResourceNotFoundException;
import com.ecommerce.ecom.model.Cart;
import com.ecommerce.ecom.model.CartItem;
import com.ecommerce.ecom.model.Product;
import com.ecommerce.ecom.model.User;
import com.ecommerce.ecom.payload.CartDTO;
import com.ecommerce.ecom.payload.ProductDTO;
import com.ecommerce.ecom.repositories.CartItemRepository;
import com.ecommerce.ecom.repositories.CartRepository;
import com.ecommerce.ecom.repositories.ProductRepository;
import com.ecommerce.ecom.repositories.UserRepository;
import com.ecommerce.ecom.util.AuthUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class CartServiceImpl implements CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartServiceImpl.class);

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    ModelMapper modelMapper;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Recalculates the total price of a cart based on its items
     * This ensures the price is always correct regardless of past operations
     *
     * @param cart The cart to recalculate totals for
     * @return The updated cart with correct total price
     */
    private Cart recalculateCartTotal(Cart cart) {
        if (cart == null) return null;

        // Get fresh cart data from database to avoid stale collections
        Long cartId = cart.getCartId();
        cart = cartRepository.findById(cartId).orElse(cart);

        double totalPrice = 0.0;

        // Calculate total based on current items in cart
        if (cart.getCartItems() != null && !cart.getCartItems().isEmpty()) {
            // Use a new ArrayList to avoid ConcurrentModificationException
            List<CartItem> items = new ArrayList<>(cart.getCartItems());

            for (CartItem item : items) {
                if (item.getProduct() != null) {
                    // Use the item's stored product price (which should reflect special price)
                    totalPrice += item.getProductPrice() * item.getQuantity();
                }
            }
        }

        // Ensure precision - round to 2 decimal places
        totalPrice = Math.round(totalPrice * 100.0) / 100.0;

        // Set the new total and save the cart
        cart.setTotalPrice(totalPrice);
        return cartRepository.save(cart);
    }

    @Override
    public CartDTO addProductToCart(Long productId, Integer quantity) {
        Cart cart = createCart();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);

        if (cartItem != null) {
            // Instead of throwing exception, update existing item
            logger.info("Product {} already exists in the cart, updating quantity", product.getProductName());
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setDiscount(product.getDiscount());
            cartItemRepository.save(cartItem);
        } else {
            // Check product availability
            if (product.getQuantity() == 0) {
                throw new APIException(product.getProductName() + " is not available");
            }

            if (product.getQuantity() < quantity) {
                throw new APIException("Please, make an order of the " + product.getProductName()
                        + " less than or equal to the quantity " + product.getQuantity() + ".");
            }

            // Create new cart item
            CartItem newCartItem = new CartItem();
            newCartItem.setProduct(product);
            newCartItem.setCart(cart);
            newCartItem.setQuantity(quantity);
            newCartItem.setDiscount(product.getDiscount());
            newCartItem.setProductPrice(product.getSpecialPrice());
            cartItemRepository.save(newCartItem);
        }

        // Recalculate cart total instead of manually adding
        cart = recalculateCartTotal(cart);

        // Map to DTO and return
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        List<CartItem> cartItems = cart.getCartItems();
        Stream<ProductDTO> productStream = cartItems.stream().map(item -> {
            ProductDTO map = modelMapper.map(item.getProduct(), ProductDTO.class);
            map.setQuantity(item.getQuantity());
            return map;
        });
        cartDTO.setProducts(productStream.toList());
        return cartDTO;
    }

    @Override
    public List<CartDTO> getAllCarts() {
        List<Cart> carts = cartRepository.findAll();

        if (carts.isEmpty()) {
            throw new APIException("No cart found");
        }

        // Check totals for all carts and fix if needed
        carts.forEach(this::recalculateCartTotal);

        List<CartDTO> cartDTOs = carts.stream().map(cart -> {
            CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

            List<ProductDTO> products = cart.getCartItems().stream().map(cartItem -> {
                ProductDTO productDTO = modelMapper.map(cartItem.getProduct(), ProductDTO.class);
                productDTO.setQuantity(cartItem.getQuantity());
                return productDTO;
            }).toList();

            cartDTO.setProducts(products);

            return cartDTO;
        }).toList();

        return cartDTOs;
    }

    @Override
    public CartDTO getCart(String emailId, Long cartId) {
        Cart cart = cartRepository.findCartByEmailAndCartId(emailId, cartId);

        if (cart == null) {
            // Return empty cart if not found
            CartDTO emptyCart = new CartDTO();
            emptyCart.setProducts(new ArrayList<>());
            emptyCart.setTotalPrice(0.0);
            return emptyCart;
        }

        // Ensure cart total is accurate
        cart = recalculateCartTotal(cart);

        cart.getCartItems().forEach(c -> c.getProduct().setQuantity(c.getQuantity()));

        List<ProductDTO> products = cart.getCartItems().stream()
                .map(p -> modelMapper.map(p.getProduct(), ProductDTO.class)).toList();

        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        cartDTO.setProducts(products);

        return cartDTO;
    }

    @Transactional
    @Override
    public CartDTO updateProductQuantityInCart(Long productId, Integer quantity) {
        String emailId = authUtil.loggedInEmail();
        Cart userCart = cartRepository.findCartByEmail(emailId);
        Long cartId  = userCart.getCartId();

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        if (product.getQuantity() == 0) {
            throw new APIException(product.getProductName() + " is not available");
        }

        if (product.getQuantity() < quantity) {
            throw new APIException("Please, make an order of the " + product.getProductName()
                    + " less than or equal to the quantity " + product.getQuantity() + ".");
        }

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if (cartItem == null) {
            throw new APIException("Product " + product.getProductName() + " not available in the cart!!!");
        }

        // Update cart item properties
        cartItem.setProductPrice(product.getSpecialPrice());
        cartItem.setQuantity(cartItem.getQuantity() + quantity);
        cartItem.setDiscount(product.getDiscount());

        // Save cart item first
        cartItemRepository.save(cartItem);

        // Recalculate cart total instead of manually adding
        cart = recalculateCartTotal(cart);

        // Map to DTO and return
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        List<CartItem> cartItems = cart.getCartItems();
        Stream<ProductDTO> productStream = cartItems.stream().map(item -> {
            ProductDTO prd = modelMapper.map(item.getProduct(), ProductDTO.class);
            prd.setQuantity(item.getQuantity());
            return prd;
        });
        cartDTO.setProducts(productStream.toList());
        return cartDTO;
    }

    @Transactional
    @Override
    public CartDTO updateProductQuantityInCart(Long productId, String operation) {
        String emailId = authUtil.loggedInEmail();
        Cart userCart = cartRepository.findCartByEmail(emailId);

        if (userCart == null) {
            throw new ResourceNotFoundException("Cart", "email", emailId);
        }

        Long cartId = userCart.getCartId();

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if (cartItem == null) {
            throw new APIException("Product " + product.getProductName() + " not available in the cart!!!");
        }

        cartItem.setProductPrice(product.getSpecialPrice());

        // Update quantity based on operation
        if ("increase".equalsIgnoreCase(operation)) {
            if (product.getQuantity() == 0) {
                throw new APIException(product.getProductName() + " is not available");
            }

            if (cartItem.getQuantity() >= product.getQuantity()) {
                throw new APIException("Cannot add more than available stock (" + product.getQuantity() + ")");
            }

            cartItem.setQuantity(cartItem.getQuantity() + 1);
        } else if ("decrease".equalsIgnoreCase(operation)) {
            if (cartItem.getQuantity() <= 1) {
                // Remove item if quantity would be 0 or less
                cartItemRepository.deleteById(cartItem.getCartItemId());
                cart.getCartItems().remove(cartItem);
            } else {
                cartItem.setQuantity(cartItem.getQuantity() - 1);
            }
        } else if ("delete".equalsIgnoreCase(operation)) {
            // Remove item completely
            cartItemRepository.deleteById(cartItem.getCartItemId());
            cart.getCartItems().remove(cartItem);
        }

        // Save any item changes if the item wasn't deleted
        if (!"delete".equalsIgnoreCase(operation) && cartItem.getQuantity() > 0) {
            cartItemRepository.save(cartItem);
        }

        // Recalculate cart total
        cart = recalculateCartTotal(cart);

        // If cart is now empty, return a CartDTO with empty products list
        if (cart.getCartItems().isEmpty()) {
            CartDTO emptyCartDTO = new CartDTO();
            emptyCartDTO.setCartId(cart.getCartId());
            emptyCartDTO.setTotalPrice(0.0);
            emptyCartDTO.setProducts(new ArrayList<>());
            return emptyCartDTO;
        }

        // Map to DTO and return
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        List<ProductDTO> products = cart.getCartItems().stream().map(item -> {
            ProductDTO prd = modelMapper.map(item.getProduct(), ProductDTO.class);
            prd.setQuantity(item.getQuantity());
            return prd;
        }).toList();
        cartDTO.setProducts(products);
        return cartDTO;
    }

    // Rest of the methods remain the same but should use recalculateCartTotal
    // instead of manual price calculations

    private Cart createCart() {
        Cart userCart = cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if(userCart != null) {
            return userCart;
        }

        Cart cart = new Cart();
        cart.setTotalPrice(0.0);
        cart.setUser(authUtil.loggedInUser());
        Cart newCart = cartRepository.save(cart);

        return newCart;
    }

    @Transactional
    @Override
    public String deleteProductFromCart(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        // Get product information before deleting for the return message
        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if (cartItem == null) {
            throw new ResourceNotFoundException("Product", "productId", productId);
        }

        String productName = cartItem.getProduct().getProductName();

        // Remove the cart item
        cartItemRepository.deleteCartItemByProductIdAndCartId(cartId, productId);

        // Force a flush to ensure changes are written to the database
        entityManager.flush();

        // IMPORTANT: Don't use the cart.getCartItems() collection after deletion
        // Instead, re-fetch the cart from the database to get fresh state
        cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        // Recalculate cart total and update in database
        double totalPrice = 0.0;
        if (cart.getCartItems() != null && !cart.getCartItems().isEmpty()) {
            totalPrice = cart.getCartItems().stream()
                    .mapToDouble(item -> item.getProductPrice() * item.getQuantity())
                    .sum();
        }

        cart.setTotalPrice(totalPrice);
        cartRepository.save(cart);

        // Check if cart is empty based on count from database
        Long remainingItems = cartItemRepository.countByCartId(cartId);
        logger.info("Remaining items in cart after deletion: {}", remainingItems);

        if (remainingItems == 0) {
            logger.info("Deleting empty cart with ID: {}", cartId);
            cartRepository.delete(cart);
            return "Product removed and empty cart deleted";
        }

        return "Product " + productName + " removed from the cart";
    }

    // The deleteEmptyCart and updateProductInCarts methods should also use recalculateCartTotal
    // for consistent cart pricing

    @Transactional
    @Override
    public String deleteEmptyCart(String email) {
        try {
            Cart cart = cartRepository.findCartByEmail(email);

            if (cart == null) {
                logger.warn("No cart found for email: {}", email);
                return "No cart found to delete";
            }

            // Force a flush and refresh to get the most recent state
            entityManager.flush();
            entityManager.refresh(cart);

            // Check count directly from database
            Long itemCount = cartItemRepository.countByCartId(cart.getCartId());
            logger.info("Items in cart: {} for email: {}", itemCount, email);

            if (itemCount == 0) {
                logger.info("Deleting empty cart for email: {}", email);

                // Get the user to update their reference
                User user = cart.getUser();
                if (user != null) {
                    user.setCart(null);
                    userRepository.save(user);
                }

                cartRepository.delete(cart);
                return "Empty cart deleted successfully";
            } else {
                logger.warn("Cannot delete non-empty cart with {} items", itemCount);
                throw new APIException("Cart is not empty");
            }
        } catch (Exception e) {
            logger.error("Error deleting empty cart: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void updateProductInCarts(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if (cartItem == null) {
            throw new APIException("Product " + product.getProductName() + " not available in the cart!!!");
        }

        // Update cart item price
        cartItem.setProductPrice(product.getSpecialPrice());
        cartItemRepository.save(cartItem);

        // Recalculate cart total
        recalculateCartTotal(cart);
    }
}