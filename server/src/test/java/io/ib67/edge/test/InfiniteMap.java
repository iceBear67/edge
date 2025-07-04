package io.ib67.edge.test;

import java.util.HashMap;

public class InfiniteMap extends HashMap<Object, Object> {
    @Override
    public Object get(Object key) {
        return this;
    }

}
