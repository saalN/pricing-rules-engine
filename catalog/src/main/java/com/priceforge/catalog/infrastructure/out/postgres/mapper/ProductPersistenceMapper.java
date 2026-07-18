package com.priceforge.catalog.infrastructure.out.postgres.mapper;

import com.priceforge.catalog.domain.model.Product;
import com.priceforge.catalog.infrastructure.out.postgres.entity.ProductDocument;
import com.priceforge.sharedkernel.Money;
import com.priceforge.sharedkernel.ProductId;
import java.util.Currency;
import org.springframework.stereotype.Component;

@Component
public class ProductPersistenceMapper {

    public ProductDocument toEntity(Product product) {
        return new ProductDocument(
            product.id().value(),
            product.sku(),
            product.name(),
            product.description(),
            product.basePrice().amount(),
            product.basePrice().currency().getCurrencyCode(),
            product.category(),
            product.customAttributes(),
            product.status(),
            product.createdAt());
    }

    public Product toDomain(ProductDocument document) {
        return Product.reconstitute(
            new ProductId(document.getId()),
            document.getSku(),
            document.getName(),
            document.getDescription(),
            new Money(document.getBasePriceAmount(), Currency.getInstance(document.getCurrency())),
            document.getCategory(),
            document.getCustomAttributes(),
            document.getStatus(),
            document.getCreatedAt());
    }
}
