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
package org.jgrapht.opt.graph.sparse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.jgrapht.Graph;
import org.jgrapht.GraphType;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.AbstractGraph;
import org.jgrapht.graph.DefaultGraphType;

import com.google.common.graph.EndpointPair;

/**
 * Sparse undirected graph.
 *
 * <p>
 * Assuming the graph has $n$ vertices, the vertices are numbered from $0$ to $n-1$. Similarly,
 * edges are numbered from $0$ to $m-1$ where $m$ is the total number of edges.
 *
 * <p>
 * It stores the boolean incidence matrix of the graph (rows are vertices and columns are edges) as
 * Compressed Sparse Rows (CSR). In order to also support constant time source and target lookups
 * from an edge identifier we also store the transposed of the incidence matrix again in compressed
 * sparse rows format. This is a classic format for write-once read-many use cases. Thus, the graph
 * is unmodifiable.
 *
 *
 * <p>
 * The question of whether a sparse or dense representation is more appropriate is highly dependent
 * on various factors such as the graph, the machine running the algorithm and the algorithm itself.
 * Wilkinson defined a matrix as "sparse" if it has enough zeros that it pays to take advantage of
 * them. For more details see
 * <ul>
 * <li>Wilkinson, J. H. 1971. Linear algebra; part II: the algebraic eigenvalue problem. In Handbook
 * for Automatic Computation, J. H. Wilkinson and C. Reinsch, Eds. Vol. 2. Springer-Verlag, Berlin,
 * New York.</li>
 * </ul>
 *
 * Additional information about sparse representations can be found in the
 * <a href="https://en.wikipedia.org/wiki/Sparse_matrix">wikipedia</a>.
 *
 * @author Dimitrios Michail
 */
public class SparseIntUndirectedGraph
    extends
    AbstractGraph<Integer, Integer>
{
    protected static final String UNMODIFIABLE = "this graph is unmodifiable";

    protected CSRBooleanMatrix incidenceMatrix;
    protected CSRBooleanMatrix incidenceMatrixT;

    /**
     * Create a new graph from an edge list
     *
     * @param numVertices number of vertices
     * @param edges edge list
     */
	public SparseIntUndirectedGraph(final int numVertices, final int m, final Iterable<EndpointPair<Integer>> edges)
    {
        final List<Pair<Integer, Integer>> nonZeros = new ArrayList<>(m);
        final List<Pair<Integer, Integer>> nonZerosTranspose = new ArrayList<>(m);

        int eIndex = 0;
		for (final EndpointPair<Integer> e : edges) {
			nonZeros.add(Pair.of(e.nodeU(), eIndex));
			nonZerosTranspose.add(Pair.of(eIndex, e.nodeU()));
			nonZeros.add(Pair.of(e.nodeV(), eIndex));
			nonZerosTranspose.add(Pair.of(eIndex, e.nodeV()));
            eIndex++;
        }
        incidenceMatrix = new CSRBooleanMatrix(numVertices, m, nonZeros);
		incidenceMatrixT = new CSRBooleanMatrix(m, numVertices, nonZerosTranspose);
    }

    @Override
    public Supplier<Integer> getVertexSupplier()
    {
        return null;
    }

    @Override
    public Supplier<Integer> getEdgeSupplier()
    {
        return null;
    }

    @Override
    public Integer addEdge(final Integer sourceVertex, final Integer targetVertex)
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public boolean addEdge(final Integer sourceVertex, final Integer targetVertex, final Integer e)
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public Integer addVertex()
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public boolean addVertex(final Integer v)
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public boolean containsEdge(final Integer e)
    {
        return e >= 0 && e < incidenceMatrix.columns();
    }

    @Override
    public boolean containsVertex(final Integer v)
    {
        return v >= 0 && v < incidenceMatrix.rows();
    }

    @Override
    public Set<Integer> edgeSet()
    {
        return new CompleteIntegerSet(incidenceMatrix.columns());
    }

    @Override
    public int degreeOf(final Integer vertex)
    {
        assertVertexExist(vertex);
        return incidenceMatrix.nonZeros(vertex);
    }

    @Override
    public Set<Integer> edgesOf(final Integer vertex)
    {
        assertVertexExist(vertex);
        return incidenceMatrix.nonZerosSet(vertex);
    }

    @Override
    public int inDegreeOf(final Integer vertex)
    {
        assertVertexExist(vertex);
        return incidenceMatrix.nonZeros(vertex);
    }

    @Override
    public Set<Integer> incomingEdgesOf(final Integer vertex)
    {
        assertVertexExist(vertex);
        return incidenceMatrix.nonZerosSet(vertex);
    }

    @Override
    public int outDegreeOf(final Integer vertex)
    {
        assertVertexExist(vertex);
        return incidenceMatrix.nonZeros(vertex);
    }

    @Override
    public Set<Integer> outgoingEdgesOf(final Integer vertex)
    {
        assertVertexExist(vertex);
        return incidenceMatrix.nonZerosSet(vertex);
    }

    @Override
    public Integer removeEdge(final Integer sourceVertex, final Integer targetVertex)
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public boolean removeEdge(final Integer e)
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public boolean removeVertex(final Integer v)
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public Set<Integer> vertexSet()
    {
        return new CompleteIntegerSet(incidenceMatrix.rows());
    }

    @Override
    public GraphType getType()
    {
        return new DefaultGraphType.Builder()
            .undirected().weighted(false).modifiable(false).allowMultipleEdges(true)
            .allowSelfLoops(true).build();
    }

    @Override
    public double getEdgeWeight(final Integer e)
    {
        return Graph.DEFAULT_EDGE_WEIGHT;
    }

    @Override
    public void setEdgeWeight(final Integer e, final double weight)
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public Integer getEdgeSource(final Integer e)
    {
        assertEdgeExist(e);
        return incidenceMatrixT.nonZerosPositionIterator(e).next();
    }

    @Override
    public Integer getEdgeTarget(final Integer e)
    {
        assertEdgeExist(e);
        final Iterator<Integer> it = incidenceMatrixT.nonZerosPositionIterator(e);
        it.next();
        return it.next();
    }

    /**
     * {@inheritDoc}
     *
     * This operation costs $O(d)$ where $d$ is the degree of the source vertex.
     */
    @Override
    public Integer getEdge(final Integer sourceVertex, final Integer targetVertex)
    {
        if (sourceVertex < 0 || sourceVertex >= incidenceMatrix.rows()) {
            return null;
        }
        if (targetVertex < 0 || targetVertex >= incidenceMatrix.rows()) {
            return null;
        }

        final Iterator<Integer> it = incidenceMatrix.nonZerosPositionIterator(sourceVertex);
        while (it.hasNext()) {
            final int eId = it.next();

            final int v = getEdgeSource(eId);
            final int u = getEdgeTarget(eId);

            if (v == sourceVertex && u == targetVertex || v == targetVertex && u == sourceVertex) {
                return eId;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * This operation costs $O(d)$ where $d$ is the degree of the source vertex.
     */
    @Override
    public Set<Integer> getAllEdges(final Integer sourceVertex, final Integer targetVertex)
    {
        if (sourceVertex < 0 || sourceVertex >= incidenceMatrix.rows()) {
            return null;
        }
        if (targetVertex < 0 || targetVertex >= incidenceMatrix.rows()) {
            return null;
        }

        final Set<Integer> result = new LinkedHashSet<>();
        final Iterator<Integer> it = incidenceMatrix.nonZerosPositionIterator(sourceVertex);
        while (it.hasNext()) {
            final int eId = it.next();

            final int v = getEdgeSource(eId);
            final int u = getEdgeTarget(eId);

            if (v == sourceVertex && u == targetVertex || v == targetVertex && u == sourceVertex) {
                result.add(eId);
            }
        }
        return result;
    }

    /**
     * Ensures that the specified vertex exists in this graph, or else throws exception.
     *
     * @param v vertex
     * @return <code>true</code> if this assertion holds.
     * @throws IllegalArgumentException if specified vertex does not exist in this graph.
     */
    @Override
	protected boolean assertVertexExist(final Integer v)
    {
        if (v >= 0 && v < incidenceMatrix.rows()) {
            return true;
        } else {
            throw new IllegalArgumentException("no such vertex in graph: " + v.toString());
        }
    }

    /**
     * Ensures that the specified edge exists in this graph, or else throws exception.
     *
     * @param e edge
     * @return <code>true</code> if this assertion holds.
     * @throws IllegalArgumentException if specified edge does not exist in this graph.
     */
    protected boolean assertEdgeExist(final Integer e)
    {
        if (e >= 0 && e < incidenceMatrix.columns()) {
            return true;
        } else {
            throw new IllegalArgumentException("no such edge in graph: " + e.toString());
        }
    }

}
