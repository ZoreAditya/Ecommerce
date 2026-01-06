package Ecom.Controller;

import java.util.List;

import Ecom.ModelDTO.CartItemUpdateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import Ecom.Model.Cart;
import Ecom.Model.Product;
import Ecom.Service.CartService;

@RestController
@RequestMapping("/ecom/cart")
@RequiredArgsConstructor
public class CartController {

    @Autowired
    private  CartService cartService;

    @PostMapping("/add-product")
    public ResponseEntity<Cart> addProductToCart(@RequestParam Integer userId, @RequestParam Integer productId) {
        Cart cart = cartService.addProductToCart(userId, productId);
        return new ResponseEntity<>(cart, HttpStatus.CREATED);

    }

    @PutMapping("/increase-productQty/{userId}/{productId}")
    public ResponseEntity<Cart> increaseProductQuantity(
            @PathVariable Integer userId,
            @PathVariable Integer productId

    ) {
        Cart cart = cartService.increaseProductQuantity(userId, productId);
        return ResponseEntity.ok(cart);
    }

    @PutMapping("/decrease-productQty/{userId}/{productId}")
    public ResponseEntity<Cart> decreaseProductQuantity(
            @PathVariable Integer userId,
            @PathVariable Integer productId

    ) {
        Cart cart = cartService.decreaseProductQuantity(userId, productId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/remove-product/{cartId}/{productId}")
    public ResponseEntity<String> removeProductFromCart(@PathVariable Integer cartId, @PathVariable Integer productId) {
        cartService.removeProductFromCart(cartId, productId);
        String msg = "Prodcut is removed from cart";
        return new ResponseEntity<String>(msg, HttpStatus.OK);
    }

    @DeleteMapping("/empty-Cart/{cartId}")
    public ResponseEntity<String> removeAllProductFromCart(@PathVariable Integer cartId) {
        cartService.removeAllProductFromCart(cartId);
        String msg = "All product Remove From cart";
        return new ResponseEntity<String>(msg, HttpStatus.OK);
    }

    @GetMapping("/products/{userId}")
    public ResponseEntity<Cart> getAllCartProducts(@PathVariable Integer userId) {
        Cart products = cartService.getAllCartProduct(userId);
        return ResponseEntity.ok(products);
    }

    @PutMapping("/update/{userId}")
    public ResponseEntity<Cart> updateCart(
            @PathVariable Integer userId,
            @RequestBody List<CartItemUpdateDTO> updatedItems) {
        Cart updatedCart = cartService.updateCartItems(userId, updatedItems);
        return ResponseEntity.ok(updatedCart);
    }

}
