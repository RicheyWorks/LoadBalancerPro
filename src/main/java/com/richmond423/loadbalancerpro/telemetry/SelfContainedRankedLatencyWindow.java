package com.richmond423.loadbalancerpro.telemetry;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Exact sliding latency window backed by a size-augmented AVL tree.
 *
 * <p>Samples use a sequence number as a tie-breaker, so duplicate latency
 * values remain independently removable in FIFO order. Recording and
 * percentile selection are both {@code O(log n)}.</p>
 */
public final class SelfContainedRankedLatencyWindow implements RankedLatencyWindow {
    private final int capacity;
    private final Deque<SampleKey> insertionOrder;

    private Node root;
    private long nextSequence;

    public SelfContainedRankedLatencyWindow(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.insertionOrder = new ArrayDeque<>(capacity);
    }

    @Override
    public synchronized void record(long latencyNanos) {
        if (latencyNanos < 0) {
            throw new IllegalArgumentException("latencyNanos must be non-negative");
        }
        if (insertionOrder.size() == capacity) {
            root = remove(root, insertionOrder.removeFirst());
        }

        SampleKey key = new SampleKey(latencyNanos, nextSequence++);
        root = insert(root, key);
        insertionOrder.addLast(key);
    }

    @Override
    public synchronized long percentileNanos(double percentile) {
        if (!Double.isFinite(percentile) || percentile < 0.0 || percentile > 100.0) {
            throw new IllegalArgumentException("percentile must be finite and in [0, 100]");
        }
        int sampleCount = insertionOrder.size();
        if (sampleCount == 0) {
            return 0;
        }
        int rank = Math.max(0, (int) Math.ceil(percentile / 100.0 * sampleCount) - 1);
        return select(root, rank).latencyNanos();
    }

    @Override
    public synchronized int size() {
        return insertionOrder.size();
    }

    @Override
    public synchronized void clear() {
        root = null;
        insertionOrder.clear();
        nextSequence = 0;
    }

    private static Node insert(Node node, SampleKey key) {
        if (node == null) {
            return new Node(key);
        }
        if (key.compareTo(node.key) < 0) {
            node.left = insert(node.left, key);
        } else {
            node.right = insert(node.right, key);
        }
        return rebalance(node);
    }

    private static Node remove(Node node, SampleKey key) {
        if (node == null) {
            throw new IllegalStateException("latency window index is inconsistent");
        }

        int comparison = key.compareTo(node.key);
        if (comparison < 0) {
            node.left = remove(node.left, key);
        } else if (comparison > 0) {
            node.right = remove(node.right, key);
        } else if (node.left == null) {
            return node.right;
        } else if (node.right == null) {
            return node.left;
        } else {
            Node successor = minimum(node.right);
            node.key = successor.key;
            node.right = remove(node.right, successor.key);
        }
        return rebalance(node);
    }

    private static SampleKey select(Node node, int rank) {
        Node current = node;
        int remainingRank = rank;
        while (current != null) {
            int leftSize = size(current.left);
            if (remainingRank < leftSize) {
                current = current.left;
            } else if (remainingRank == leftSize) {
                return current.key;
            } else {
                remainingRank -= leftSize + 1;
                current = current.right;
            }
        }
        throw new IllegalStateException("latency window rank is inconsistent");
    }

    private static Node minimum(Node node) {
        Node current = node;
        while (current.left != null) {
            current = current.left;
        }
        return current;
    }

    private static Node rebalance(Node node) {
        update(node);
        int balance = height(node.left) - height(node.right);
        if (balance > 1) {
            if (height(node.left.left) < height(node.left.right)) {
                node.left = rotateLeft(node.left);
            }
            return rotateRight(node);
        }
        if (balance < -1) {
            if (height(node.right.right) < height(node.right.left)) {
                node.right = rotateRight(node.right);
            }
            return rotateLeft(node);
        }
        return node;
    }

    private static Node rotateLeft(Node node) {
        Node replacement = node.right;
        node.right = replacement.left;
        replacement.left = node;
        update(node);
        update(replacement);
        return replacement;
    }

    private static Node rotateRight(Node node) {
        Node replacement = node.left;
        node.left = replacement.right;
        replacement.right = node;
        update(node);
        update(replacement);
        return replacement;
    }

    private static void update(Node node) {
        node.height = 1 + Math.max(height(node.left), height(node.right));
        node.size = 1 + size(node.left) + size(node.right);
    }

    private static int height(Node node) {
        return node == null ? 0 : node.height;
    }

    private static int size(Node node) {
        return node == null ? 0 : node.size;
    }

    private record SampleKey(long latencyNanos, long sequence) implements Comparable<SampleKey> {
        @Override
        public int compareTo(SampleKey other) {
            int latencyComparison = Long.compare(latencyNanos, other.latencyNanos);
            return latencyComparison != 0
                    ? latencyComparison
                    : Long.compare(sequence, other.sequence);
        }
    }

    private static final class Node {
        private SampleKey key;
        private Node left;
        private Node right;
        private int height = 1;
        private int size = 1;

        private Node(SampleKey key) {
            this.key = key;
        }
    }
}
