package org.xcorpion.jdiff.util.collection;

public class Tree<T> implements TreeLike<T> {

    private T value;
    private Iterable<Tree<T>> children;

    public Tree(T value) {
        this(value, null);
        children = Iterables.empty();
    }

    public Tree(T value, Iterable<Tree<T>> children) {
        this.value = value;
        this.children = children;
    }

    @Override
    public T getNodeValue() {
        return value;
    }

    @Override
    public Iterable<Tree<T>> getChildren() {
        return children;
    }

}
