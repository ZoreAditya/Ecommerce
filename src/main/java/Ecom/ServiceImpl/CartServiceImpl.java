package Ecom.ServiceImpl;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import Ecom.ModelDTO.CartItemUpdateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import Ecom.Exception.CartException;
import Ecom.Exception.ProductException;
import Ecom.Exception.UserException;
import Ecom.Model.Cart;
import Ecom.Model.CartItem;
import Ecom.Model.Product;
import Ecom.Model.User;
import Ecom.Repository.CartItemRepository;
import Ecom.Repository.CartRepository;
import Ecom.Repository.ProductRepository;
import Ecom.Repository.UserRepository;
import Ecom.Service.CartService;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

	private final ProductRepository productRepository;
	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final UserRepository userRepository;

	public Cart addProductToCart(Integer userId, Integer productId) throws CartException {

		Product existingProduct = productRepository.findById(productId)
				.orElseThrow(() -> new ProductException("Product not available in Stock..."));

		User existingUser = userRepository.findById(userId)
				.orElseThrow(() -> new UserException("User Not Found In Database"));

		if (existingUser.getCart() != null) {
			Cart userCart = existingUser.getCart();
			List<CartItem> cartItems = userCart.getCartItems();

			if (cartItems != null) {
				for (CartItem item : cartItems) {
					if (item.getProduct().getProductId().equals(productId)
							&& item.getCart().getCartId().equals(userCart.getCartId())) {
						throw new CartException("Product Already in the Cart, Please Increase the Quantity");
					}
				}
			}

			CartItem cartItem = new CartItem();
			cartItem.setProduct(existingProduct);
			cartItem.setQuantity(1);
			cartItem.setCart(userCart);
			userCart.getCartItems().add(cartItem);

			userCart.setTotalAmount(calculateCartTotal(userCart.getCartItems()));
			sortCartItems(userCart); // 🔹 maintain order
			cartRepository.save(userCart);

			return userCart;

		} else {
			Cart newCart = new Cart();
			newCart.setUser(existingUser);
			existingUser.setCart(newCart);

			CartItem cartItem = new CartItem();
			cartItem.setProduct(existingProduct);
			cartItem.setQuantity(1);
			cartItem.setCart(newCart);

			newCart.getCartItems().add(cartItem);
			newCart.setTotalAmount(calculateCartTotal(newCart.getCartItems()));
			sortCartItems(newCart); // 🔹 maintain order

			userRepository.save(existingUser);

			return existingUser.getCart();
		}
	}

	private double calculateCartTotal(List<CartItem> cartItems) {
		double total = 0.0;
		for (CartItem item : cartItems) {
			total += item.getProduct().getPrice() * item.getQuantity();
		}
		return total;
	}

	// 🔹 helper to always keep items ordered by cartItemId
	private void sortCartItems(Cart cart) {
		List<CartItem> sorted = cart.getCartItems().stream()
				.sorted(Comparator.comparing(
						CartItem::getCartItemId,
						Comparator.nullsLast(Comparator.naturalOrder())
				))
				.collect(Collectors.toList());
		cart.setCartItems(sorted);
	}	

	@Override
	public Cart updateCartItems(Integer userId, List<CartItemUpdateDTO> updatedItems) throws CartException {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserException("User Not Found"));

		Cart cart = user.getCart();
		if (cart == null) {
			throw new CartException("Cart Not Found");
		}

		List<CartItem> cartItems = cart.getCartItems();

		// Update each item’s quantity
		for (CartItemUpdateDTO update : updatedItems) {
			cartItems.stream()
					.filter(item -> item.getProduct().getProductId().equals(update.getProductId()))
					.findFirst()
					.ifPresent(item -> item.setQuantity(update.getQuantity()));
		}

		cart.setTotalAmount(calculateCartTotal(cartItems));
		sortCartItems(cart);
		cartRepository.save(cart);

		return cart;
	}


	@Override
	public Cart increaseProductQuantity(Integer userId, Integer productId) throws CartException {
		User existingUser = userRepository.findById(userId)
				.orElseThrow(() -> new UserException("User Not Found in Database"));

		if (existingUser.getCart() == null) {
			throw new CartException("Cart Not Found");
		}

		Cart userCart = existingUser.getCart();
		List<CartItem> cartItems = userCart.getCartItems();

		CartItem cartItemToUpdate = cartItems.stream()
				.filter(item -> item.getProduct().getProductId().equals(productId)
						&& item.getCart().getCartId().equals(userCart.getCartId()))
				.findFirst()
				.orElseThrow(() -> new CartException("Cart Item Not Found"));

		cartItemToUpdate.setQuantity(cartItemToUpdate.getQuantity() + 1);
		userCart.setTotalAmount(calculateCartTotal(cartItems));
		sortCartItems(userCart); // 🔹 maintain order
		cartRepository.save(userCart);

		return userCart;
	}

	@Override
	public Cart decreaseProductQuantity(Integer userId, Integer productId) throws CartException {
		User existingUser = userRepository.findById(userId)
				.orElseThrow(() -> new UserException("User Not Found in Database"));

		if (existingUser.getCart() == null) {
			throw new CartException("Cart Not Found");
		}

		Cart userCart = existingUser.getCart();
		List<CartItem> cartItems = userCart.getCartItems();

		CartItem cartItemToUpdate = cartItems.stream()
				.filter(item -> item.getProduct().getProductId().equals(productId)
						&& item.getCart().getCartId().equals(userCart.getCartId()))
				.findFirst()
				.orElseThrow(() -> new CartException("Cart Item Not Found"));

		int quantity = cartItemToUpdate.getQuantity();
		if (quantity == 1) {
			throw new CartException("Product can not be further decreased...");
		}

		if (quantity > 1) {
			cartItemToUpdate.setQuantity(quantity - 1);
			userCart.setTotalAmount(calculateCartTotal(cartItems));
		} else {
			cartItems.remove(cartItemToUpdate);
			userCart.setTotalAmount(calculateCartTotal(cartItems));
		}

		sortCartItems(userCart); // 🔹 maintain order
		cartRepository.save(userCart);

		return userCart;
	}

	@Override
	public void removeProductFromCart(Integer cartId, Integer productId) throws CartException {
		Cart existingCart = cartRepository.findById(cartId)
				.orElseThrow(() -> new CartException("Cart Not Found"));

		cartItemRepository.removeProductFromCart(cartId, productId);

		existingCart.setTotalAmount(calculateCartTotal(existingCart.getCartItems()));
		sortCartItems(existingCart); // 🔹 maintain order
		cartRepository.save(existingCart);
	}

	@Override
	public Cart getAllCartProduct(Integer userId) throws CartException {
		User existingUser = userRepository.findById(userId)
				.orElseThrow(() -> new UserException("User Not Found in Database"));

		if (existingUser.getCart() == null) {
			throw new CartException("Cart Not Found");
		}

		Cart userCart = existingUser.getCart();
//		List<CartItem> cartItems = userCart.getCartItems();
//		Cart existingCart = cartRepository.findById(cartId)
//				.orElseThrow(() -> new CartException("Cart Not Found"));
//
//		List<CartItem> cartItems = existingCart.getCartItems();
//		if (cartItems.isEmpty()) {
//			throw new CartException("Cart is Empty...");
//		}

		sortCartItems(userCart); // 🔹 maintain order
		return userCart;
	}

	@Override
	public void removeAllProductFromCart(Integer cartId) throws CartException {
		Cart existingCart = cartRepository.findById(cartId)
				.orElseThrow(() -> new CartException("Cart Not Found"));

		cartItemRepository.removeAllProductFromCart(cartId);
		existingCart.setTotalAmount(0.0);

		sortCartItems(existingCart); // 🔹 maintain order
		cartRepository.save(existingCart);
	}
}
