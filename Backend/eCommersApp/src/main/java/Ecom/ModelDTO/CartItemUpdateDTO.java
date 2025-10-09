package Ecom.ModelDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemUpdateDTO {
    private Integer productId;
    private Integer quantity;
}
