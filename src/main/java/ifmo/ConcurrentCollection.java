package ifmo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicMarkableReference;

public class ConcurrentCollection<T> implements Collection<T> {
    private final Node<T> tail = new Node<>(null, null);
    private final AtomicMarkableReference<Node<T>> head = new AtomicMarkableReference<>(tail, false);

    @Override
    public boolean add(T value) {
        while (true) {
            Node<T> curHead = head.getReference();
            Node<T> newHead = new Node<>(value, curHead);

            if (head.compareAndSet(curHead, newHead, false, false)) {
                return true;
            } else {
                if (head.isMarked()) {
                    return false;
                }
            }
        }
    }

    @Override
    public void blockFurtherAdd() {
        while (true) {
            Node<T> curHead = head.getReference();
            if (head.attemptMark(curHead, true)) {
                return;
            }
        }
    }

    @Override
    public boolean isBlocked() {
        return head.isMarked();
    }

    @Override
    public List<T> content() {
        if (!isBlocked()) {
            throw new IllegalStateException();
        }
        final List<T> content = new ArrayList<>();
        Node<T> curr = head.getReference();
        while (curr != tail) {
            content.add(curr.value);
            curr = curr.next;
        }
        return content;
    }

    /*
    Node element
     */
    private static class Node<V> {
        final V value;
        final Node<V> next;

        Node(V value, Node<V> next) {
            this.value = value;
            this.next = next;
        }
    }
}
