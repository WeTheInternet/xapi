package net.wti.lang.parser.ast;

import net.wti.lang.parser.ast.expr.AnnotationExpr;
import xapi.fu.In1Out1;
import xapi.fu.Maybe;
import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;
import xapi.fu.itr.EmptyIterator;
import xapi.fu.itr.SizedIterable;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 8/20/18 @ 3:55 AM.
 */
public interface HasAnnotationExprs {

    default <T> Maybe<AnnotationExpr> getAnnotation(In1Out1<AnnotationExpr, T> mapper, In1Out1<T, Boolean> filter) {
        return getAnnotation(mapper.mapOut(filter));
    }

    default boolean hasAnnotationBool(boolean dflt, String ... names) {
        return findAnnotation(names)
            .mapIfPresent(anno-> {
                // check the body of the annotation for a true/false statement
                if (anno.getMembers().isEmpty()) {
                    return dflt;
                }
                // TODO: use a transformer that ignored "string" and `template` wrappers
                return "true".equalsIgnoreCase(anno.getMembers().first().toSource());
            }).ifAbsentReturn(false);
    }

    default Maybe<AnnotationExpr> findAnnotation(String ... names) {
        return getAnnotation(expr-> {
            // find the annotation
            for (String name : names) {
                if (name.equalsIgnoreCase(expr.getNameString())) {
                    return true;
                }
            }
            return false;
        });
    }

    default SizedIterable<AnnotationExpr> findAnnotations(String ... names) {
        return getAnnotationsMatching(expr-> {
            // find the annotation
            for (String name : names) {
                if (name.equalsIgnoreCase(expr.getNameString())) {
                    return true;
                }
            }
            return false;
        });
    }

    default Maybe<AnnotationExpr> getAnnotation(In1Out1<AnnotationExpr, Boolean> filter) {
        final Iterable<AnnotationExpr> annotations = getAnnotations();
        if (annotations != null) {
            for (AnnotationExpr annotation : annotations) {
                if (filter.io(annotation)) {
                    return Maybe.nullable(annotation);
                }
            }
        }
        return Maybe.not();
    }

    default SizedIterable<AnnotationExpr> getAnnotationsMatching(In1Out1<AnnotationExpr, Boolean> filter) {
        final Iterable<AnnotationExpr> annotations = getAnnotations();
        if (annotations == null) {
            return EmptyIterator.none();
        }
        ChainBuilder<AnnotationExpr> result = Chain.startChain();
        for (AnnotationExpr annotation : annotations) {
            if (filter.io(annotation)) {
                result.add(annotation);
            }
        }
        return result.counted();
    }

    Iterable<AnnotationExpr> getAnnotations();
}
