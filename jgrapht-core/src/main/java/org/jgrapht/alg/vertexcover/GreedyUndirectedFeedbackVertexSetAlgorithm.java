/*
 * (C) Copyright 2019-2020, by Dimitrios Michail and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * See the CONTRIBUTORS.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the
 * GNU Lesser General Public License v2.1 or later
 * which is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1-standalone.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR LGPL-2.1-or-later
 */
package org.jgrapht.alg.vertexcover;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jgrapht.Graph;
import org.jgrapht.GraphTests;
import org.jgrapht.alg.interfaces.FeedbackVertexSetAlgorithm;
import org.jgrapht.util.SetToIntegerMapping;
import org.jgrapht.util.SparseUtils;
import org.jgrapht.util.SparseUtils.AdjacencyMatrix;
import org.jheaps.AddressableHeap;
import org.jheaps.array.DaryArrayAddressableHeap;

/**
 * The greedy algorithm for the (weighted) feedback vertex set problem on undirected graphs.
 * 
 * <p>
 * Computes a (weighted) <a href="https://en.wikipedia.org/wiki/Feedback_vertex_set">feedback vertex
 * set</a> of an undirected graph. A feedback vertex set is a set of vertices whose removal leaves
 * the graph without cycles. In other words, a feedback vertex set contains at least one vertex of
 * any cycle in the graph. In artificial intelligence the problem is also called a loop cutset.
 *
 * <p>
 * The feedback vertex set problem is NP-hard. The greedy algorithm in undirected graphs is an
 * $\mathcal{O}(\log n)$ approximation algorithm. The running time of this implementation is
 * $\mathcal{O}(m \log n)$ where $n$ is the number of vertices and $m$ the number of edges in the
 * graph.
 * 
 * <p>
 * The algorithm is described in detail in the
 * <a href="https://doi.org/10.1016/0004-3702(95)00004-6">paper</a>:
 * <ul>
 * <li>A. Becker and D. Geiger. Optimization of Pearl's method of conditioning and greedy-like
 * approximation algorithms for the vertex feedback set problem. Artificial Intelligence, 83(1),
 * 167--188, 1996.</li>
 * </ul>
 * 
 * @author Dimitrios Michail
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 */
public class GreedyUndirectedFeedbackVertexSetAlgorithm<V, E>
    implements
    FeedbackVertexSetAlgorithm<V>
{
    private final Graph<V, E> graph;
    private final Function<V, Double> vertexWeights;
    private final Supplier<AddressableHeap<Double, Integer>> heapSupplier;
    private FeedbackVertexSet<V> result;

    /**
     * Constructor. Assumes uniform vertex weights and uses by default a 4-ary array based heap.
     * 
     * @param graph the input graph
     */
    public GreedyUndirectedFeedbackVertexSetAlgorithm(Graph<V, E> graph)
    {
        this(graph, x -> 1d, () -> new DaryArrayAddressableHeap<>(4));
    }

    /**
     * Constructor. Uses by default a 4-ary array based heap.
     * 
     * @param graph the input graph
     * @param vertexWeights the vertex weights
     */
    public GreedyUndirectedFeedbackVertexSetAlgorithm(
        Graph<V, E> graph, Function<V, Double> vertexWeights)
    {
        this(graph, vertexWeights, () -> new DaryArrayAddressableHeap<>(4));
    }

    /**
     * Constructor
     * 
     * @param graph the input graph
     * @param vertexWeights the vertex weights
     * @param heapSupplier a heap supplier
     */
    public GreedyUndirectedFeedbackVertexSetAlgorithm(
        Graph<V, E> graph, Function<V, Double> vertexWeights,
        Supplier<AddressableHeap<Double, Integer>> heapSupplier)
    {
        this.graph = GraphTests.requireUndirected(graph);
        this.vertexWeights = Objects.requireNonNull(vertexWeights);
        this.heapSupplier = Objects.requireNonNull(heapSupplier);
    }

    @Override
    public FeedbackVertexSet<V> getFeedbackVertexSet()
    {
        if (result == null) {
            result = new Algorithm().solve();
        }
        return result;
    }

    /**
     * The actual implementation.
     */
    private class Algorithm
    {
        // graph representation using integers
        private final SetToIntegerMapping<V> vertexToIntegerMapping;
        private final List<V> indexList;
        private final AdjacencyMatrix adj;

        // number of vertices
        private final int n;
        // whether a vertex still exists
        private final boolean[] exists;
        // vertex degrees
        private final int[] degree;
        // vertex weights
        private final double[] weight;

        // heap
        private final AddressableHeap<Double, Integer> heap;
        private final AddressableHeap.Handle<Double, Integer>[] handle;

        // scheduled for cleanup
        private final Deque<Integer> forRemoval;

        // result
        private Set<V> resultSet;
        private double resultWeight;

        @SuppressWarnings("unchecked")
        public Algorithm()
        {
            vertexToIntegerMapping = new SetToIntegerMapping<>(graph.vertexSet());
            indexList = vertexToIntegerMapping.getIndexList();
            adj = SparseUtils.createSparseAdjacencyMatrix(graph, vertexToIntegerMapping);

            n = adj.numberOfVertices();
            exists = new boolean[n];
            Arrays.fill(exists, true);
            degree = new int[n];
            weight = new double[n];
            forRemoval = new ArrayDeque<>();
            int i = 0;
            for (V v : indexList) {
                weight[i] = vertexWeights.apply(v);
                if ((degree[i] = adj.degree(i)) <= 1) {
                    forRemoval.addLast(i);
                }
                i++;
            }
            heap = heapSupplier.get();
            handle = (AddressableHeap.Handle<Double, Integer>[]) Array
                .newInstance(AddressableHeap.Handle.class, n);

            resultSet = new LinkedHashSet<>();
            resultWeight = 0d;
        }

        /**
         * Remove all vertices present in the {@link #forRemoval} collection and cascade by
         * repeatedly removing any vertices whose degree becomes one or zero.
         */
        private void cleanup()
        {
            while (!forRemoval.isEmpty()) {
                int u = forRemoval.removeFirst();
                exists[u] = false;

                adj.adjacentVertices(u).filter(x -> exists[x]).forEach(x -> {
                    degree[x]--;
                    if (degree[x] <= 1) {
                        // collect for removal
                        forRemoval.addFirst(x);
                    } else {
                        if (handle[x] != null) {
                            handle[x].delete();
                            handle[x] = heap.insert(weight[x] / degree[x], x);
                        }
                    }
                });
            }
        }

        /**
         * Execute the algorithm
         * 
         * @return the feedback vertex set
         */
        public FeedbackVertexSet<V> solve()
        {
            cleanup();

            for (int i = 0; i < n; i++) {
                if (!exists[i]) {
                    continue;
                }
                handle[i] = heap.insert(weight[i] / degree[i], i);
            }

            // main loop
            while (!heap.isEmpty()) {
                int v = heap.deleteMin().getValue();
                handle[v] = null;
                if (!exists[v]) {
                    continue;
                }

                // add to feedback vertex set
                resultSet.add(indexList.get(v));
                resultWeight += weight[v];

                // remove vertices
                forRemoval.addFirst(v);
                cleanup();
            }

            return new FeedbackVertexSetImpl<V>(resultSet, resultWeight);
        }

    }

}
