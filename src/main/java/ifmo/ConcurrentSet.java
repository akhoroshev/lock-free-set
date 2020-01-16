package ifmo;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/*
 Lock-free set.
 Iterator implementation based on http://www.cs.technion.ac.il/~erez/Papers/iterators-disc13.pdf.
 */
final class ConcurrentSet<T extends Comparable<T>> implements Set<T> {
    private final Node<T> tail = new Node<>(null);
    private final Node<T> head = new Node<>(null, tail);
    private final AtomicReference<SnapCollector<T>> snapCollectorHolder = new AtomicReference<>(new SnapCollector<>(false));

    @Override
    public boolean add(T value) {
        while (true) {
            final Pair<Node<T>> place = find(value);
            final Node<T> pred = place.first;
            final Node<T> curr = place.second;
            if (curr != tail && curr.getValue().compareTo(value) == 0) {
                return false;
            }
            final Node<T> nodeToInsert = new Node<>(value, curr);
            if (pred.compareAndSet(curr, nodeToInsert, false, false)) {
                reportInsert(nodeToInsert);
                return true;
            }
        }
    }

    @Override
    public boolean remove(T value) {
        while (true) {
            final Pair<Node<T>> place = find(value);
            final Node<T> pred = place.first;
            final Node<T> curr = place.second;
            if (curr == tail || curr.getValue().compareTo(value) != 0) {
                return false;
            }
            // `curr` is not tail
            final Node<T> succ = curr.getNext();
            if (curr.compareAndSet(succ, succ, false, true)) {
                reportDelete(curr);
                // Try remove physically
                pred.compareAndSet(curr, succ, false, false);
                return true;
            }
        }
    }

    @Override
    public boolean contains(T value) {
        Node<T> curr = head.getNext();
        while (curr != tail) {
            if (curr.isDeleted()) {
                curr = curr.getNext();
            } else {
                int cmpResult = curr.getValue().compareTo(value);
                if (cmpResult == 0) {
                    return true;
                } else if (cmpResult > 0) {
                    return false;
                } else {
                    curr = curr.getNext();
                }
            }
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return !iterator().hasNext();
    }

    @Override
    public Iterator<T> iterator() {
        final SnapCollector<T> collector = acquireSnapCollector();
        collectSnapshot(collector);
        return reconstruct(collector).iterator();
    }

    private void reportDelete(Node<T> node) {
        final SnapCollector<T> collector = snapCollectorHolder.get();
        if (collector.isActive()) {
            collector.addReport(new Report<>(node, Report.ReportType.DELETED));
        }
    }

    private void reportInsert(Node<T> node) {
        final SnapCollector<T> collector = snapCollectorHolder.get();
        if (collector.isActive() && !node.isDeleted()) {
            collector.addReport(new Report<>(node, Report.ReportType.INSERTED));
        }
    }

    private SnapCollector<T> acquireSnapCollector() {
        SnapCollector<T> collector = snapCollectorHolder.get();
        if (collector.isActive())
            return collector;
        SnapCollector<T> newCollector = new SnapCollector<>(true);
        snapCollectorHolder.compareAndSet(collector, newCollector);
        return snapCollectorHolder.get();
    }

    private void collectSnapshot(SnapCollector<T> collector) {
        Node<T> curr = head.getNext();
        while (collector.isActive()) {
            if (curr != tail && !curr.isDeleted()) {
                collector.addNode(curr);
            }
            if (curr == tail) {
                collector.blockFurtherNodes();
                collector.deactivate();
                break;
            }
            curr = curr.getNext();
        }
        collector.blockFurtherReports();
    }

    private java.util.Set<T> reconstruct(SnapCollector<T> collector) {
        java.util.Set<Node<T>> nodes = new HashSet<>(collector.readNodes());
        java.util.Set<Node<T>> insertReports = new HashSet<>();
        java.util.Set<Node<T>> deleteReports = new HashSet<>();
        collector.readReports().forEach(report -> {
            switch (report.type) {
                case DELETED:
                    deleteReports.add(report.value);
                    break;
                case INSERTED:
                    insertReports.add(report.value);
                    break;
            }
        });
        nodes.addAll(insertReports);
        nodes.removeAll(deleteReports);
        return nodes.stream().map(Node::getValue).collect(Collectors.toCollection(TreeSet::new));
    }


    private Pair<Node<T>> find(T value) {
        while (true) {
            Node<T> pred = head;
            Node<T> curr = pred.getNext();
            boolean continueOuterLoop = false;
            while (curr != tail) {
                Node<T> succ = curr.getNext();
                if (curr.isDeleted()) {
                    reportDelete(curr);
                    // Try remove physically
                    if (pred.compareAndSet(curr, succ, false, false)) {
                        curr = succ;
                    } else {
                        continueOuterLoop = true;
                        break;
                    }
                } else {
                    reportInsert(curr);
                    if (curr.getValue().compareTo(value) >= 0) {
                        break;
                    } else {
                        pred = curr;
                        curr = succ;
                    }
                }
            }
            if (continueOuterLoop) {
                continue;
            }
            return new Pair<>(pred, curr);
        }
    }

    /*
    Node element
     */
    private static class Node<V> {
        /**
         * Value
         */
        private final V value;

        /**
         * Flag indicating that the current node has been deleted
         * Reference to the next node
         */
        private final AtomicMarkableReference<Node<V>> next;

        Node(V value) {
            this(value, null);
        }

        Node(V value, Node<V> next) {
            this.value = value;
            this.next = new AtomicMarkableReference<>(next, false);
        }

        boolean isDeleted() {
            return next.isMarked();
        }

        V getValue() {
            return value;
        }

        Node<V> getNext() {
            return next.getReference();
        }

        boolean compareAndSet(Node<V> expectedReference,
                              Node<V> newReference,
                              boolean expectedDeleted,
                              boolean newDeleted) {
            return next.compareAndSet(expectedReference, newReference, expectedDeleted, newDeleted);
        }
    }

    /*
    Pair
     */
    static class Pair<V> {

        final V first;

        final V second;

        Pair(V first, V second) {
            this.first = first;
            this.second = second;
        }
    }

    private static class Report<V> {
        final Node<V> value;
        final ReportType type;

        private Report(Node<V> value, ReportType type) {
            this.value = value;
            this.type = type;
        }

        enum ReportType {
            INSERTED,
            DELETED
        }
    }

    /*
    Snapshot collector
     */
    private static class SnapCollector<V> {
        private final Collection<Report<V>> reports = new ConcurrentCollection<>();
        private final Collection<Node<V>> nodes = new ConcurrentCollection<>();
        private final AtomicBoolean isActive = new AtomicBoolean(true);

        SnapCollector(boolean isActive) {
            this.isActive.set(isActive);
        }

        void deactivate() {
            isActive.set(false);
        }

        boolean isActive() {
            return isActive.get();
        }

        void addReport(Report<V> report) {
            reports.add(report);
        }

        void addNode(Node<V> node) {
            nodes.add(node);
        }

        void blockFurtherNodes() {
            nodes.blockFurtherAdd();
        }

        void blockFurtherReports() {
            reports.blockFurtherAdd();
        }

        List<Node<V>> readNodes() {
            return nodes.content();
        }

        List<Report<V>> readReports() {
            return reports.content();
        }
    }
}
