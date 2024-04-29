package com.app.preorder.entity.member;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRandomKey is a Querydsl query type for RandomKey
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRandomKey extends EntityPathBase<RandomKey> {

    private static final long serialVersionUID = -1501647126L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRandomKey randomKey1 = new QRandomKey("randomKey1");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QMember member;

    public final StringPath randomKey = createString("randomKey");

    public QRandomKey(String variable) {
        this(RandomKey.class, forVariable(variable), INITS);
    }

    public QRandomKey(Path<? extends RandomKey> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRandomKey(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRandomKey(PathMetadata metadata, PathInits inits) {
        this(RandomKey.class, metadata, inits);
    }

    public QRandomKey(Class<? extends RandomKey> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new QMember(forProperty("member"), inits.get("member")) : null;
    }

}

