/*
 * (C) Copyright 2019-2020 Dimitrios Michail and Contributors.
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
package org.jgrapht.alg.labeling;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.VertexLabelingAlgorithm;
import org.jgrapht.alg.util.Pair;

/**
 * The Weisfeiler-Lehman labeling.
 * 
 * <p>
 * This is an algorithm which computes the 1-dimensional Weisfeiler-Lehman labeling, also known as
 * the naive vertex refinement. The labeling was introduced in the paper: B. Weisfeiler and A. A.
 * Lehman. A reduction of a graph to a canonical form and an algebra arising during this reduction.
 * Nauchno-Technicheskaya Informatsia, Ser. 2, 9, 1968.
 * 
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 */
public class WeisfeilerLehmanLabeling<V, E>
    implements
    VertexLabelingAlgorithm<V>,
    Iterable<VertexLabelingAlgorithm.Labeling<V>>
{
    /**
     * The initial label used for all vertices that the caller has not provided any initial label.
     */
    public static final String DEFAULT_INITIAL_LABEL = "1";

    private final Graph<V, E> graph;
    private final int maxIterations;
    private final NeighborhoodType neighborhoodType;
    private final Map<V, String> initialLabels;

    /**
     * What kind of neighborhood to use when computing new labels.
     */
    public enum NeighborhoodType
    {
        OUTGOING,
        INCOMING,
    }

    /**
     * Construct a new labeling algorithm.
     * 
     * @param graph the input graph
     * @param iterations number of iterations of the Weisfeiler-Lehman relabeling
     * @param initialLabels initial labels of the vertices (cannot be null but can be empty)
     * @param neighborhoodType what kind of neighbors to use for each vertex in order to compute the
     *        labeling
     */
    public WeisfeilerLehmanLabeling(
        Graph<V, E> graph, int iterations, Map<V, String> initialLabels,
        NeighborhoodType neighborhoodType)
    {
        this.graph = Objects.requireNonNull(graph, "Graph cannot be null");
        if (iterations < 1) {
            throw new IllegalArgumentException("Number of iterations must be positive");
        }
        this.maxIterations = iterations;
        this.neighborhoodType = Objects.requireNonNull(neighborhoodType);
        Objects.requireNonNull(initialLabels, "Initial labels cannot be null.");

        this.initialLabels = new HashMap<>();
        for (V v : graph.vertexSet()) {
            this.initialLabels.put(v, initialLabels.getOrDefault(v, DEFAULT_INITIAL_LABEL));
        }
    }

    /**
     * Construct a new labeling algorithm.
     * 
     * @param graph the input graph
     * @param iterations number of iterations of the Weisfeiler-Lehman relabeling
     */
    public WeisfeilerLehmanLabeling(Graph<V, E> graph, int iterations)
    {
        this(graph, iterations, Collections.emptyMap(), NeighborhoodType.OUTGOING);
    }

    @Override
    public Labeling<V> getLabeling()
    {
        Labeling<V> prev = null;
        Labeling<V> cur = null;

        Iterator<Labeling<V>> it = iterator();
        while (it.hasNext()) {
            prev = cur;
            cur = it.next();

            if (prev != null && isTheSameLabeling(prev, cur)) {
                break;
            }
        }
        return cur;
    }

    /**
     * Create an iterator which returns the Weisfeiler-Lehman in iterations.
     * 
     * @return an iterator which returns the Weisfeiler-Lehman in iterations
     */
    public Iterator<Labeling<V>> iterator()
    {
        return new LabelsIterator();
    }

    /**
     * The actual implementation as an iterator.
     * 
     * @author Dimitrios Michail
     */
    private class LabelsIterator
        implements
        Iterator<Labeling<V>>
    {
        private int curIteration;
        private Map<V, String> labels;
        private final int n;

        public LabelsIterator()
        {
            this.curIteration = 0;
            this.labels = new HashMap<>(initialLabels);
            this.n = graph.vertexSet().size();
        }

        @Override
        public boolean hasNext()
        {
            return curIteration < maxIterations;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Labeling<V> next()
        {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            /*
             * Generate new labels. Use an array so we can sort fast later on.
             */
            Pair<LexTuple, V>[] newLabels = (Pair<LexTuple, V>[]) Array.newInstance(Pair.class, n);
            int cur = 0;
            for (V v : graph.vertexSet()) {
                newLabels[cur++] = Pair.of(computeNewLabel(v, labels), v);
            }

            /*
             * Sort
             */
            Arrays.parallelSort(newLabels, Comparator.comparing(Pair::getFirst));

            /*
             * Compress
             */
            for (int i = 0, nextLabel = 1; i < n; i++) {
                LexTuple curLabel = newLabels[i].getFirst();
                if (i > 0) {
                    LexTuple prevLabel = newLabels[i - 1].getFirst();
                    if (curLabel.compareTo(prevLabel) != 0) {
                        nextLabel++;
                    }
                }
                V curVertex = newLabels[i].getSecond();
                labels.put(curVertex, String.valueOf(nextLabel));
            }

            curIteration++;

            /*
             * Return result, but make sure that the collection is a copy, to avoid confusing the
             * user.
             */
            return new LabelingImpl<V>(new HashMap<>(labels));
        }

    }

    /**
     * Compute a new label for a particular vertex.
     * 
     * @param v the vertex to compute
     * @param labels the current labels
     * @return the new label
     */
    private LexTuple computeNewLabel(V v, Map<V, String> labels)
    {
        /*
         * Get sorted list of neighborhood labels
         */
        Set<E> incidentEdges = getIncidentEdges(v);
        String[] multiset = (String[]) Array.newInstance(String.class, incidentEdges.size());
        int i = 0;
        for (E e : incidentEdges) {
            multiset[i++] = labels.get(Graphs.getOppositeVertex(graph, e, v));
        }
        Arrays.parallelSort(multiset);

        /*
         * Prepend own label and append sorted neighbors
         */
        LexTuple lt = new LexTuple(labels.get(v));
        for (String s : multiset) {
            lt.value.add(s);
        }

        return lt;
    }

    /**
     * Get all incident edges of a vertex.
     * 
     * @param v the vertex
     * @return the neighbors of a vertex
     */
    private Set<E> getIncidentEdges(V v)
    {
        switch (neighborhoodType) {
        case INCOMING:
            return graph.incomingEdgesOf(v);
        default:
            return graph.outgoingEdgesOf(v);
        }
    }

    /**
     * A custom tuple which can be compared lexicographically.
     */
    private static class LexTuple
        implements
        Comparable<LexTuple>
    {
        public List<String> value;

        public LexTuple(String v)
        {
            value = new ArrayList<>(Arrays.asList(v));
        }

        @Override
        public int compareTo(LexTuple o)
        {
            Iterator<String> it1 = value.iterator();
            Iterator<String> it2 = o.value.iterator();
            while (it1.hasNext() && it2.hasNext()) {
                String v1 = it1.next();
                String v2 = it2.next();
                int c = v1.compareTo(v2);
                if (c < 0) {
                    return -1;
                } else if (c > 0) {
                    return 1;
                }
            }
            if (it2.hasNext()) {
                return -1;
            }
            if (it1.hasNext()) {
                return 1;
            }
            return 0;
        }

    }

    /**
     * Check if the two labeling(s) are the same (modulo isomorphism).
     * 
     * @param <V> the graph vertex type
     * 
     * @param a the first labeling
     * @param b the same labeling
     * @return true if the same (modulo isomorphism), false otherwise
     */
    public static <V> boolean isTheSameLabeling(Labeling<V> a, Labeling<V> b)
    {
        Map<String, Integer> labelCounts = new HashMap<>();
        a.getLabels().values().stream().forEach(label -> {
            labelCounts.put(label, labelCounts.getOrDefault(label, 0) + 1);
        });
        for (String label : b.getLabels().values()) {
            Integer count = labelCounts.get(label);
            if (count == null || count <= 0) {
                return false;
            }
            labelCounts.put(label, count - 1);
        }
        return true;
    }

}
