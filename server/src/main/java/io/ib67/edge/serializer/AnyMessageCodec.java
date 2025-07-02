package io.ib67.edge.serializer;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class AnyMessageCodec<T> implements MessageCodec<T, T> {
    protected final Class<T> type;

    public AnyMessageCodec(Class<T> type) {
        this.type = type;
    }

    @Override
    public void encodeToWire(Buffer buffer, T t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public T decodeFromWire(int pos, Buffer buffer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public T transform(T t) {
        return t;
    }

    @Override
    public String name() {
        return type.getName();
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
}
