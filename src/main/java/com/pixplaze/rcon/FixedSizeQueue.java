package com.pixplaze.rcon;

import java.util.LinkedList;

public class FixedSizeQueue<E> extends LinkedList<E> {

    private int maxSize = 256;

    public FixedSizeQueue(int maxSize) {
        super();
        this.maxSize = maxSize;
    }

    public FixedSizeQueue() {
        super();
    }

    public int getMaxSize() {
        return maxSize;
    }

    @Override
    public boolean add(E e) {
        if (size() >= maxSize) {
            remove(getFirst());
        }
        addLast(e);
        return true;
    }
}
