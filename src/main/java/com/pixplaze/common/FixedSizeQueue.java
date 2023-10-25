package com.pixplaze.common;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * Represents a queue that contain fixed count of elements.
 * If the queue is full, removes the oldest element and adds
 * new one in the end.
 * @param <T> the type of elements held in this queue
 * @see java.util.AbstractQueue
 */
@SuppressWarnings("unchecked")
public class FixedSizeQueue<T> extends AbstractQueue<T> {

    private final int capacity;
    private final boolean allowNulls;
    private final boolean allowOverload;
    private final Object[] items;
    private int size = 0;

    public FixedSizeQueue(final int capacity) {
        this(capacity, false, false);
    }

    public FixedSizeQueue(final int capacity, final boolean allowNulls, final boolean allowOverload) {
        this.capacity = capacity;
        this.items = new Object[capacity];
        this.allowNulls = allowNulls;
        this.allowOverload = allowOverload;
    }

    @Override
    public @NotNull Iterator<T> iterator() {
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public T next() {
                return (T) items[index++];
            }
        };
    }

    @Override
    public int size() {
        return size;
    }


    /**
     * If the queue is full, removes the oldest element and adds
     * new one in the end
     * @param item the element to add
     * @return true if provided element added
     * @throws IllegalArgumentException if this queue do not allow to contain nulls
     * and provided item is null
     */
    @Override
    public boolean offer(T item) throws IllegalArgumentException {
        if (!this.allowNulls && item == null) {
            throw new IllegalArgumentException(
                    "%s cannot accept null items!"
                            .formatted(getClass().getSimpleName()));
        }

        if (size >= capacity) {
            shift();
        }

        items[size++] = item;

        return true;
    }

    @Override
    public T poll() {
        return shift();
    }

    @Override
    public T peek() {
        return (T) this.items[0];
    }

    /**
     * Clears all elements from the queue
     */
    @Override
    public void clear() {
        Arrays.fill(this.items, 0, this.size, null);
        this.size = 0;
    }

    /**
     * Adds all collection items in the queue.
     * If provided collection is greater than capacity of this queue,
     * queue items will be overwritten.
     * @param items collection containing elements to be added to this queue
     * @return true if this queue changed as a result of the call
     * @throws IllegalArgumentException if provided collection size significant larger than
     * queue capacity.
     */
    @Override
    public boolean addAll(Collection<? extends T> items) throws IllegalArgumentException {
        if (!this.allowOverload && this.capacity * 1.5 > items.size()) {
            throw new IllegalArgumentException("""
                    The provided collection overloads the queue elements,
                    this can have a significant impact on performance.
                    To disable this warning set 'allowOverloads' in constructor.
                    """); // TODO: can be rewritten by optimal way
        }
        return super.addAll(items);
    }

    private T shift() {
        var buff = (T) this.items[0];

        for (int i = 0; i < size - 1; i++) {
            this.items[i] = this.items[i + 1];
        }

        this.items[size-- - 1] = null;

        return buff;
    }
}
