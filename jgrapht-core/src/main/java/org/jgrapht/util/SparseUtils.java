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
package org.jgrapht.util;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jgrapht.Graph;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.EdgeReversedGraph;

/**
 * Utility class for the representation of a graph using a sparse adjacency or a sparse incidence
 * matrix.
 * 
 * <p>
 * This is a low-level representation of a graph assuming that vertices are numbered in $[0,n)$ and
 * edges in $[0,m)$ using compressed-sparse-rows (CSR). This class is very helpful in conjunction
 * with {@link SetToIntegerMapping} in order to convert a graph into a more cache-friendly
 * representation.
 * 
 * <p>
 * This representation keeps only the operations which are usually called "navigation" on a graph,
 * meaning that given a vertex $v$ a user can find out only information about the neighborhood of
 * $v$. More general representations should be implemented using classes which implement the
 * {@link Graph} interface and should be considered out-of-scope in this class.
 * 
 * @author Dimitrios Michail
 */
public class SparseUtils
{
    /**
     * An adjacency matrix.
     */
    public interface AdjacencyMatrix
    {
        /**
         * Get number of vertices.
         * 
         * @return the number of vertices
         */
        int numberOfVertices();

        /**
         * Get a stream of adjacent vertices of a given vertex.
         * 
         * @param v the vertex
         * @return a stream of adjacent vertices of a given vertex.
         */
        IntStream adjacentVertices(int v);

        /**
         * Get the degree of a vertex.
         * 
         * @param v the vertex
         * @return the degree of the vertex
         */
        int degree(int v);
    }

    /**
     * An incidence matrix.
     */
    public interface IncidenceMatrix
    {
        /**
         * Get the number of vertices.
         * 
         * @return the number of vertices
         */
        int numberOfVertices();

        /**
         * Get the number of edges.
         * 
         * @return the number of edges
         */
        int numberOfEdges();

        /**
         * Get an integer stream of all edges incident to a vertex.
         * 
         * @param v the input vertex
         * @return the integer stream will all incident edges to v
         */
        IntStream incidentEdges(int v);

        /**
         * The degree of vertex v
         * 
         * @param v the input vertex
         * @return the degree of vertex v
         */
        int degree(int v);

        /**
         * Get the source vertex of an edge
         * 
         * @param e the input edge
         * @return the source vertex of e
         */
        int edgeSource(int e);

        /**
         * Get the target vertex of an edge
         * 
         * @param e the input edge
         * @return the target vertex of e
         */
        int edgeTarget(int e);
    }

    /**
     * Create a sparse adjacency matrix representation of a graph.
     * 
     * <p>
     * The resulting adjacency matrix depends on the type of the graph. If the graph contains
     * self-loops a vertex $v$ might be adjacent to itself. Similarly if the graph contains multiple
     * edges, then a vertex $v$ might be returned multiple times as adjacent to a single vertex $u$.
     * 
     * <p>
     * In case of directed graphs only the outgoing edges are considered, meaning that methods
     * {@link AdjacencyMatrix#adjacentVertices(int)} and {@link AdjacencyMatrix#degree(int)} return
     * information concerning only the outgoing incident edges. This is done on purpose, as the
     * majority of algorithms on graphs require either the outgoing or the incoming edges, but not
     * both. If this is not the case, an adjacency matrix which uses the incoming edges can be
     * easily constructed by calling this method using the {@link EdgeReversedGraph} view.
     * 
     * @param graph the input graph
     * @return a sparse adjacency matrix representation
     */
    public static <V, E> AdjacencyMatrix createSparseAdjacencyMatrix(Graph<V, E> graph)
    {
        return createSparseAdjacencyMatrix(graph, new SetToIntegerMapping<V>(graph.vertexSet()));
    }

    /**
     * Create a sparse adjacency matrix representation of a graph given a predefined vertex to
     * integer mapping.
     * 
     * <p>
     * The resulting adjacency matrix depends on the type of the graph. If the graph contains
     * self-loops a vertex $v$ might be adjacent to itself. Similarly if the graph contains multiple
     * edges, then a vertex $v$ might be returned multiple times as adjacent to a single vertex $u$.
     * 
     * <p>
     * In case of directed graphs only the outgoing edges are considered, meaning that methods
     * {@link AdjacencyMatrix#adjacentVertices(int)} and {@link AdjacencyMatrix#degree(int)} return
     * information concerning only the outgoing incident edges. This is done on purpose, as the
     * majority of algorithms on graphs require either the outgoing or the incoming edges, but not
     * both. If this is not the case, an adjacency matrix which uses the incoming edges can be
     * easily constructed by calling this method using the {@link EdgeReversedGraph} view.
     * 
     * @param graph the input graph
     * @param vertexToIntegerMapping the vertex to integer mapping to use
     * @return a sparse adjacency matrix representation
     */
    public static <V, E> AdjacencyMatrix createSparseAdjacencyMatrix(
        Graph<V, E> graph, SetToIntegerMapping<V> vertexToIntegerMapping)
    {
        final boolean isUndirected = graph.getType().isUndirected();

        int vertices = graph.vertexSet().size();
        int edges = graph.edgeSet().size();
        int[] colIndex = new int[edges * (isUndirected ? 2 : 1)];
        int[] rowPointer = new int[vertices + 1];

        Map<V, Integer> vertexMap = vertexToIntegerMapping.getElementMap();

        int[] idx = { 0 };

        Stream<Pair<Integer, Integer>> edgeStream;
        if (isUndirected) {
            edgeStream = graph
                .edgeSet().stream().flatMap(
                    e -> Arrays
                        .asList(
                            Pair
                                .of(
                                    vertexMap.get(graph.getEdgeSource(e)),
                                    vertexMap.get(graph.getEdgeTarget(e))),
                            Pair
                                .of(
                                    vertexMap.get(graph.getEdgeTarget(e)),
                                    vertexMap.get(graph.getEdgeSource(e))))
                        .stream());
        } else {
            edgeStream = graph
                .edgeSet().stream().map(
                    e -> Pair
                        .of(
                            vertexMap.get(graph.getEdgeSource(e)),
                            vertexMap.get(graph.getEdgeTarget(e))));

        }

        edgeStream.sorted((a, b) -> Integer.compare(a.getFirst(), b.getFirst())).forEach(e -> {
            int i = idx[0];
            colIndex[i] = e.getSecond();
            rowPointer[e.getFirst() + 1]++;
            idx[0]++;
        });

        Arrays.parallelPrefix(rowPointer, (x, y) -> x + y);

        return new SparseAdjacencyMatrixImpl(vertices, colIndex, rowPointer);
    }

    /**
     * Create a sparse incidence matrix representation of a graph.
     * 
     * <p>
     * In case of directed graphs only the outgoing edges are considered, meaning that methods
     * {@link IncidenceMatrix#incidentEdges(int)} and {@link IncidenceMatrix#degree(int)} return
     * information concerning only the outgoing incident edges. This is done on purpose, as the
     * majority of algorithms on graphs require either the outgoing or the incoming edges, but not
     * both. If this is not the case, an incidence matrix which uses the incoming edges can be
     * easily constructed by calling this method using the {@link EdgeReversedGraph} view.
     * 
     * @param graph the input graph
     * @return a sparse incidence matrix representation
     */
    public static <V, E> IncidenceMatrix createSparseIncidenceMatrix(Graph<V, E> graph)
    {
        return createSparseIncidenceMatrix(
            graph, new SetToIntegerMapping<V>(graph.vertexSet()),
            new SetToIntegerMapping<E>(graph.edgeSet()));
    }

    /**
     * Create a sparse incidence matrix representation of a graph a predefined vertex to integer
     * mapping and a predefined edge to integer mapping.
     * 
     * <p>
     * In case of directed graphs only the outgoing edges are considered, meaning that methods
     * {@link IncidenceMatrix#incidentEdges(int)} and {@link IncidenceMatrix#degree(int)} return
     * information concerning only the outgoing incident edges. This is done on purpose, as the
     * majority of algorithms on graphs require either the outgoing or the incoming edges, but not
     * both. If this is not the case, an incidence matrix which uses the incoming edges can be
     * easily constructed by calling this method using the {@link EdgeReversedGraph} view.
     * 
     * @param graph the input graph
     * @param vertexToIntegerMapping the vertex to integer mapping
     * @param edgeToIntegerMapping the edge to integer mapping
     * @return a sparse incidence matrix representation
     */
    public static <V, E> IncidenceMatrix createSparseIncidenceMatrix(
        Graph<V, E> graph, SetToIntegerMapping<V> vertexToIntegerMapping,
        SetToIntegerMapping<E> edgeToIntegerMapping)
    {
        final boolean isUndirected = graph.getType().isUndirected();

        int vertices = graph.vertexSet().size();
        int edges = graph.edgeSet().size();

        int[] source = new int[edges];
        int[] target = new int[edges];
        int[] colIndex = new int[edges * (isUndirected ? 2 : 1)];
        int[] rowPointer = new int[vertices + 1];

        Map<V, Integer> vertexMap = vertexToIntegerMapping.getElementMap();
        Map<E, Integer> edgeMap = edgeToIntegerMapping.getElementMap();

        int[] idx = { 0 };
        Stream<Pair<Integer, Integer>> edgeStream;
        if (isUndirected) {
            edgeStream = graph.edgeSet().stream().flatMap(e -> {
                int eIndex = edgeMap.get(e);
                int s = vertexMap.get(graph.getEdgeSource(e));
                int t = vertexMap.get(graph.getEdgeTarget(e));
                source[eIndex] = s;
                target[eIndex] = t;
                return Arrays.asList(Pair.of(s, eIndex), Pair.of(t, eIndex)).stream();
            });
        } else {
            edgeStream = graph.edgeSet().stream().map(e -> {
                int eIndex = edgeMap.get(e);
                int s = vertexMap.get(graph.getEdgeSource(e));
                int t = vertexMap.get(graph.getEdgeTarget(e));
                source[eIndex] = s;
                target[eIndex] = t;
                return Pair.of(s, eIndex);
            });

        }

        edgeStream.sorted((a, b) -> Integer.compare(a.getFirst(), b.getFirst())).forEach(e -> {
            int i = idx[0];
            colIndex[i] = e.getSecond();
            rowPointer[i + 1]++;
            idx[0]++;
        });

        Arrays.parallelPrefix(rowPointer, (x, y) -> x + y);

        return new SparseIncidenceMatrixImpl(vertices, source, target, colIndex, rowPointer);
    }

    private static class SparseAdjacencyMatrixImpl
        implements
        AdjacencyMatrix
    {
        private final int nodes;
        private final int[] colIndex;
        private final int[] rowPointer;

        public SparseAdjacencyMatrixImpl(int nodes, int[] colIndex, int[] rowPointer)
        {
            this.nodes = nodes;
            this.colIndex = colIndex;
            this.rowPointer = rowPointer;
        }

        @Override
        public int numberOfVertices()
        {
            return nodes;
        }

        @Override
        public IntStream adjacentVertices(int v)
        {
            return IntStream.range(rowPointer[v], rowPointer[v + 1]).map(i -> colIndex[i]);
        }

        @Override
        public int degree(int v)
        {
            return rowPointer[v + 1] - rowPointer[v];
        }

    }

    private static class SparseIncidenceMatrixImpl
        implements
        IncidenceMatrix
    {
        private final int nodes;
        private final int[] source;
        private final int[] target;
        private final int[] colIndex;
        private final int[] rowPointer;

        public SparseIncidenceMatrixImpl(
            int nodes, int[] source, int[] target, int[] colIndex, int[] rowPointer)
        {
            this.nodes = nodes;
            this.source = source;
            this.target = target;
            this.colIndex = colIndex;
            this.rowPointer = rowPointer;
        }

        @Override
        public int numberOfEdges()
        {
            return source.length;
        }

        @Override
        public IntStream incidentEdges(int v)
        {
            return IntStream.range(rowPointer[v], rowPointer[v + 1]).map(i -> colIndex[i]);
        }

        @Override
        public int edgeSource(int e)
        {
            return source[e];
        }

        @Override
        public int edgeTarget(int e)
        {
            return target[e];
        }

        @Override
        public int numberOfVertices()
        {
            return nodes;
        }

        @Override
        public int degree(int v)
        {
            return rowPointer[v + 1] - rowPointer[v];
        }

    }

}
