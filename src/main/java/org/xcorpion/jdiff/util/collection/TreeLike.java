package org.xcorpion.jdiff.util.collection;

import java.util.*;

public interface TreeLike<T> {

    T getNodeValue();

    Iterable<? extends TreeLike<T>> getChildren();

    default Iterable<T> preOrderTraversal() {

        return () -> new Iterator<T>() {
            Deque<Iterator<? extends TreeLike<T>>> stack = new ArrayDeque<>();
            {
                stack.addLast(Collections.singletonList(TreeLike.this).iterator());
            }

            @Override
            public boolean hasNext() {
                return !stack.isEmpty();
            }

            @Override
            public T next() {
                Iterator<? extends TreeLike<T>> current = stack.getLast();
                TreeLike<T> curNode = current.next();
                if (!current.hasNext()) {
                    stack.removeLast();
                }
                Iterator<? extends TreeLike<T>> childIter = curNode.getChildren().iterator();
                if (childIter.hasNext()) {
                    stack.addLast(childIter);
                }
                return curNode.getNodeValue();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("remove");
            }
        };
    }

    class PostOrderNode<T> {

        private final T value;
        private final Iterator<? extends TreeLike<T>> childrenIterator;

        PostOrderNode(TreeLike<T> node) {
            this.value = node.getNodeValue();
            this.childrenIterator = node.getChildren().iterator();
        }
    }

    default Iterable<T> postOrderTraversal() {
        return () -> new Iterator<T>() {
            Deque<PostOrderNode<T>> stack = new ArrayDeque<>();
            {
                stack.addLast(new PostOrderNode<>(TreeLike.this));
            }

            @Override
            public boolean hasNext() {
                return !stack.isEmpty();
            }

            @Override
            public T next() {
                while (!stack.isEmpty()) {
                    PostOrderNode<T> current = stack.getLast();
                    if (current.childrenIterator.hasNext()) {
                        stack.addLast(new Tree.PostOrderNode<>(current.childrenIterator.next()));
                    } else {
                        stack.removeLast();
                        return current.value;
                    }
                }
                throw new NoSuchElementException();
            }
        };
    }

}
