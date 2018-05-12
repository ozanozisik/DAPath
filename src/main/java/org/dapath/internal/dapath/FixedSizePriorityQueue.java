package org.dapath.internal.dapath;

import java.util.Comparator;
import java.util.PriorityQueue;

public class FixedSizePriorityQueue<E> extends PriorityQueue<E> {

    private static final long serialVersionUID = -2216956086524487049L;

    private final int fixedsize;
        
    public FixedSizePriorityQueue(int n) {
        super(n);
        fixedsize = n;
    }
    
    public FixedSizePriorityQueue(int n, Comparator<? super E> comparator) {
        super(n, comparator);
        fixedsize = n;
    }

    @Override
    public boolean add(E e) {
        boolean added = super.add(e);
        if (this.size() >= fixedsize) {
            super.poll();
        }
        return added;
    }
}
