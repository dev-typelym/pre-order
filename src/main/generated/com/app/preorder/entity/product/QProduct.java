package com.app.preorder.entity.product;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProduct is a Querydsl query type for Product
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProduct extends EntityPathBase<Product> {

    private static final long serialVersionUID = 303120334L;

    public static final QProduct product = new QProduct("product");

    public final EnumPath<com.app.preorder.type.CatergoryType> category = createEnum("category", com.app.preorder.type.CatergoryType.class);

    public final StringPath description = createString("description");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath productName = createString("productName");

    public final NumberPath<Long> productPrice = createNumber("productPrice", Long.class);

    public final ListPath<Stock, QStock> stocks = this.<Stock, QStock>createList("stocks", Stock.class, QStock.class, PathInits.DIRECT2);

    public QProduct(String variable) {
        super(Product.class, forVariable(variable));
    }

    public QProduct(Path<? extends Product> path) {
        super(path.getType(), path.getMetadata());
    }

    public QProduct(PathMetadata metadata) {
        super(Product.class, metadata);
    }

}

