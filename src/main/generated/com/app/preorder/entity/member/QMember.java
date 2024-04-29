package com.app.preorder.entity.member;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMember is a Querydsl query type for Member
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMember extends EntityPathBase<Member> {

    private static final long serialVersionUID = 1135072524L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMember member = new QMember("member1");

    public final com.app.preorder.entity.cart.QCart cart;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.app.preorder.entity.embeddable.QAddress memberAddress;

    public final StringPath memberEmail = createString("memberEmail");

    public final StringPath memberPassword = createString("memberPassword");

    public final StringPath memberPhone = createString("memberPhone");

    public final DateTimePath<java.time.LocalDateTime> memberRegisterDate = createDateTime("memberRegisterDate", java.time.LocalDateTime.class);

    public final EnumPath<com.app.preorder.type.Role> memberRole = createEnum("memberRole", com.app.preorder.type.Role.class);

    public final EnumPath<com.app.preorder.type.SleepType> memberSleep = createEnum("memberSleep", com.app.preorder.type.SleepType.class);

    public final StringPath name = createString("name");

    public final ListPath<com.app.preorder.entity.order.Order, com.app.preorder.entity.order.QOrder> orders = this.<com.app.preorder.entity.order.Order, com.app.preorder.entity.order.QOrder>createList("orders", com.app.preorder.entity.order.Order.class, com.app.preorder.entity.order.QOrder.class, PathInits.DIRECT2);

    public final ListPath<com.app.preorder.entity.like.ProductLike, com.app.preorder.entity.like.QProductLike> productLikes = this.<com.app.preorder.entity.like.ProductLike, com.app.preorder.entity.like.QProductLike>createList("productLikes", com.app.preorder.entity.like.ProductLike.class, com.app.preorder.entity.like.QProductLike.class, PathInits.DIRECT2);

    public final ListPath<com.app.preorder.entity.product.Product, com.app.preorder.entity.product.QProduct> products = this.<com.app.preorder.entity.product.Product, com.app.preorder.entity.product.QProduct>createList("products", com.app.preorder.entity.product.Product.class, com.app.preorder.entity.product.QProduct.class, PathInits.DIRECT2);

    public final ListPath<RandomKey, QRandomKey> randomKeys = this.<RandomKey, QRandomKey>createList("randomKeys", RandomKey.class, QRandomKey.class, PathInits.DIRECT2);

    public final QSalt salt;

    public final StringPath username = createString("username");

    public QMember(String variable) {
        this(Member.class, forVariable(variable), INITS);
    }

    public QMember(Path<? extends Member> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMember(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMember(PathMetadata metadata, PathInits inits) {
        this(Member.class, metadata, inits);
    }

    public QMember(Class<? extends Member> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.cart = inits.isInitialized("cart") ? new com.app.preorder.entity.cart.QCart(forProperty("cart"), inits.get("cart")) : null;
        this.memberAddress = inits.isInitialized("memberAddress") ? new com.app.preorder.entity.embeddable.QAddress(forProperty("memberAddress")) : null;
        this.salt = inits.isInitialized("salt") ? new QSalt(forProperty("salt")) : null;
    }

}

