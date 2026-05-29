package com.CartService.controller;

import com.CartService.model.Cart;
import com.CartService.model.CartItem;
import com.CartService.service.CartService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * POST /api/cart/{sessionId}/items
     * Adds/updates an item in the session's shopping cart.
     * Returns 201 Created with the updated Cart.
     */
    @PostMapping("/{sessionId}/items")
    public ResponseEntity<Cart> addItem(
            @PathVariable String sessionId,
            @Valid @RequestBody CartItem item) {
        Cart updatedCart = cartService.addItem(sessionId, item);
        return new ResponseEntity<>(updatedCart, HttpStatus.CREATED);
    }

    /**
     * GET /api/cart/{sessionId}
     * Retrieves the current state of a shopping cart.
     * Returns 200 OK with the Cart details.
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<Cart> getCart(@PathVariable String sessionId) {
        Cart cart = cartService.getCart(sessionId);
        return ResponseEntity.ok(cart);
    }

    /**
     * DELETE /api/cart/{sessionId}/items/{productId}
     * Removes a single product item from the shopping cart.
     * Returns 200 OK with the updated Cart.
     */
    @DeleteMapping("/{sessionId}/items/{productId}")
    public ResponseEntity<Cart> removeItem(
            @PathVariable String sessionId,
            @PathVariable String productId) {
        Cart updatedCart = cartService.removeItem(sessionId, productId);
        return ResponseEntity.ok(updatedCart);
    }

    /**
     * DELETE /api/cart/{sessionId}
     * Clears the entire shopping cart (deletes all items).
     * Returns 200 OK indicating success.
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> clearCart(@PathVariable String sessionId) {
        cartService.clearCart(sessionId);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/cart/cache-stats
     * Retrieves basic statistics about the Redis cache (total carts and hit rate).
     * Returns 200 OK with the statistics map.
     */
    @GetMapping("/cache-stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = cartService.getCacheStats();
        return ResponseEntity.ok(stats);
    }
}
