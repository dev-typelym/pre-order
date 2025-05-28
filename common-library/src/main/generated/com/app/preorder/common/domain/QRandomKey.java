package com.app.preorder.common.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QRandomKey is a Querydsl query type for RandomKey
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRandomKey extends EntityPathBase<RandomKey> {

    private static final long serialVersionUID = -1863071521L;

    public static final QRandomKey randomKey1 = new QRandomKey("randomKey1");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Member> member = createNumber("member", Member.class);

    public final StringPath randomKey = createString("randomKey");

    public QRandomKey(String variable) {
        super(RandomKey.class, forVariable(variable));
    }

    public QRandomKey(Path<? extends RandomKey> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRandomKey(PathMetadata metadata) {
        super(RandomKey.class, metadata);
    }

}

