package com.ecommerce.ecom.controller;

import com.ecommerce.ecom.config.AppConstants;
import com.ecommerce.ecom.payload.ProductDTO;
import com.ecommerce.ecom.payload.ProductResponse;
import com.ecommerce.ecom.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping("/admin/categories/{categoryId}/products")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ProductDTO> addProduct(@Valid @RequestBody ProductDTO productDTO,
                                                 @PathVariable Long categoryId) {
        ProductDTO savedProductDTO = productService.addProduct(productDTO, categoryId);
        return new ResponseEntity<>(savedProductDTO, HttpStatus.CREATED);
    }

    @GetMapping("/public/products")
    public ResponseEntity<ProductResponse> getAllProducts(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_PRODUCTS_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder
    ) {
        return new ResponseEntity<>(productService.getAllProducts(pageNumber, pageSize, sortBy, sortOrder), HttpStatus.OK);
    }

    @GetMapping("/public/products/{productId}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long productId) {
        return new ResponseEntity<>(productService.getProductById(productId), HttpStatus.OK);
    }

    @GetMapping("/public/categories/{categoryId}/products")
    public ResponseEntity<ProductResponse> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_PRODUCTS_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder) {

        return new ResponseEntity<>(productService.getProductsByCategory(categoryId,pageNumber, pageSize, sortBy,sortOrder), HttpStatus.OK);
    }

    @GetMapping("/public/products/keyword/{keyWord}")
    public ResponseEntity<ProductResponse> getProductsByKeyWord(
            @PathVariable String keyWord,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_PRODUCTS_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder) {
        return new ResponseEntity<>(productService.getProductsByKeyWord(keyWord, pageNumber, pageSize, sortBy, sortOrder), HttpStatus.FOUND);
    }

    // Admin can delete any product, Seller can only delete their own products
    @DeleteMapping("/admin/products/{productId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('SELLER') and @productService.isProductOwner(#productId))")
    public ResponseEntity<ProductDTO> deleteProduct(@PathVariable Long productId) {
        return new ResponseEntity<>(productService.deleteProduct(productId), HttpStatus.OK);
    }

    // Only the product owner (seller) can update product details
    @PutMapping("/admin/products/{productId}")
    @PreAuthorize("hasRole('SELLER') and @productService.isProductOwner(#productId)")
    public ResponseEntity<ProductDTO> updateProduct(@Valid @RequestBody ProductDTO productDTO,
                                                    @PathVariable Long productId) {
        return new ResponseEntity<>(productService.updateProduct(productDTO, productId), HttpStatus.OK);
    }

    // Only the product owner (seller) can update product image
    @PutMapping("/admin/products/{productId}/image")
    @PreAuthorize("hasRole('SELLER') and @productService.isProductOwner(#productId)")
    public ResponseEntity<ProductDTO> updateProductImage(@PathVariable Long productId,
                                                         @RequestParam("image") MultipartFile image) throws IOException {
        return new ResponseEntity<>(productService.updateProductImage(productId, image), HttpStatus.OK);
    }

    // Get products for the current seller
    @GetMapping("/seller/products")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> getSellerProducts(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_PRODUCTS_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder) {
        return new ResponseEntity<>(productService.getSellerProducts(pageNumber, pageSize, sortBy, sortOrder), HttpStatus.OK);
    }

    // Check if current user can edit a specific product
    @GetMapping("/products/{productId}/can-edit")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<Boolean> canEditProduct(@PathVariable Long productId) {
        boolean canEdit = productService.canCurrentUserEditProduct(productId);
        return new ResponseEntity<>(canEdit, HttpStatus.OK);
    }

    // Check if current user can delete a specific product
    @GetMapping("/products/{productId}/can-delete")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<Boolean> canDeleteProduct(@PathVariable Long productId) {
        boolean canDelete = productService.canCurrentUserDeleteProduct(productId);
        return new ResponseEntity<>(canDelete, HttpStatus.OK);
    }
}