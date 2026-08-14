package com.contenthub.dto;

import java.math.BigDecimal;
import java.util.List;

public class CartDtos {

    public record CartItemResponse(
            Long contentId,
            String title,
            String contentType,
            String thumbnailUrl,
            BigDecimal price,
            boolean alreadyOwned
    ) {}

    public record CartResponse(
            List<CartItemResponse> items,
            int itemCount,
            BigDecimal totalAmount
    ) {}
}
