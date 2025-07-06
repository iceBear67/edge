package io.ib67.edge.mixin;

import io.vertx.core.Future;
import org.graalvm.polyglot.Value;

/**
 * this interface will be mixed into {@link io.vertx.core.Future}
 */
public interface Thenable {
    @SuppressWarnings("rawtypes")
    private Future $() {
        return (Future) this;
    }

    @SuppressWarnings("unchecked")
    default void then(Value onResolve, Value onReject) {
        $().onSuccess(onResolve::executeVoid).onFailure(onReject::executeVoid);
    }
}
