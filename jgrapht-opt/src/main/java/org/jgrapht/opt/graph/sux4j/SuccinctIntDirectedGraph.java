/*
 * (C) Copyright 2020, by Sebastiano Vigna.
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

package org.jgrapht.opt.graph.sux4j;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jgrapht.Graph;
import org.jgrapht.GraphIterables;
import org.jgrapht.GraphType;
import org.jgrapht.Graphs;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.AbstractGraph;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.opt.graph.sparse.SparseIntDirectedGraph;

import com.google.common.collect.Iterables;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.longs.LongBigListIterator;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.sux4j.util.EliasFanoIndexedMonotoneLongBigList;
import it.unimi.dsi.sux4j.util.EliasFanoMonotoneLongBigList;

/**
 * An immutable directed graph represented using quasi-succinct data structures.
 *
 * <p>
 * The graph representation of this implementation is similar to that of
 * {@link SparseIntDirectedGraph}: nodes and edges are initial intervals of the natural numbers.
 * Under the hood, however, this class uses the {@linkplain EliasFanoMonotoneLongBigList
 * Elias&ndash;Fano representation of monotone sequences} to represent the positions of the ones
 * elements in the (linearized) adjacency matrix of the graph.
 *
 * <p>
 * If the vertex set is compact (i.e., vertices are numbered from 0 consecutively), space usage will
 * be close to the information-theoretical lower bound (typically, a few times smaller than a
 * {@link SparseIntDirectedGraph}).
 *
 * <p>
 * Note that {@linkplain #containsEdge(Integer, Integer) adjacency checks} will be performed
 * essentially in constant time.
 *
 * <p>
 * The {@linkplain #SuccinctIntDirectedGraph(Graph) constructor} takes an existing graph: the
 * resulting object can be serialized and reused.
 *
 * <p>
 * This class is thread-safe.
 *
 * @author Sebastiano Vigna
 */

public class SuccinctIntDirectedGraph
    extends
    AbstractGraph<Integer, Integer>
    implements
    Serializable
{
    private static final long serialVersionUID = 0L;
    protected static final String UNMODIFIABLE = "this graph is unmodifiable";

    /**
     * Turns all lists of successors into a single monotone sequence by representing an arc
     * <var>x</var>&nbsp;&rarr;&nbsp;<var>y</var> as <var>x</var><var>n</var> + <var>y</var>.
     *
     * @param <E> the graph edge type
     */
    private final static class CumulativeSuccessors<E>
        implements
        LongIterator
    {
        private final Graph<Integer, E> graph;
        private final long n;
        private final Function<Integer, Iterable<E>> succ;
        private final boolean strict;

        private int x = -1, d, i, e;
        private long next = -1;
        private int[] s = IntArrays.EMPTY_ARRAY;

        public CumulativeSuccessors(
            final Graph<Integer, E> graph, final Function<Integer, Iterable<E>> succ,
            final boolean strict)
        {
            this.n = graph.iterables().vertexCount();
            this.graph = graph;
            this.succ = succ;
            this.strict = strict;
        }

        @Override
        public boolean hasNext()
        {
            if (next != -1)
                return true;
            if (x == n)
                return false;
            while (i == d) {
                if (++x == n)
                    return false;
                int d = 0;
                for (final E e : succ.apply(x)) {
                    s = IntArrays.grow(s, d + 1);
                    s[d++] = Graphs.getOppositeVertex(graph, e, x);
                }
                Arrays.sort(s, 0, d);
                this.d = d;
                i = 0;
            }
            // The predecessor list will not be indexed, so we can gain a few bits of space by
            // subtracting the edge position in the list
            next = s[i] + x * n - (strict ? 0 : e++);
            i++;
            return true;
        }

        @Override
        public long nextLong()
        {
            if (!hasNext())
                throw new NoSuchElementException();
            final long result = next;
            next = -1;
            return result;
        }
    }

    /**
     * Iterates over the cumulative degrees (starts with a zero).
     */
    private final static class CumulativeDegrees
        implements
        LongIterator
    {
        private final Function<Integer, Integer> degreeOf;
        private final int n;
        private int i = -1;
        private long cumul = 0;

        public CumulativeDegrees(final int n, final Function<Integer, Integer> degreeOf)
        {
            this.n = n;
            this.degreeOf = degreeOf;
        }

        @Override
        public boolean hasNext()
        {
            return i < n;
        }

        @Override
        public long nextLong()
        {
            if (!hasNext())
                throw new NoSuchElementException();
            if (i == -1)
                return ++i;
            return cumul += degreeOf.apply(i++);
        }
    }

    /** The number of vertices in the graph. */
    private final int n;
    /** The number of edges in the graph. */
    private final int m;
    /** The cumulative list of outdegrees. */
    private final EliasFanoIndexedMonotoneLongBigList cumulativeOutdegrees;
    /** The cumulative list of indegrees. */
    private final EliasFanoMonotoneLongBigList cumulativeIndegrees;
    /** The cumulative list of successor lists. */
    private final EliasFanoIndexedMonotoneLongBigList successors;
    /** The cumulative list of predecessor lists. */
    private final EliasFanoMonotoneLongBigList predecessors;

    /**
     * Creates a new immutable succinct directed graph from a given directed graph.
     *
     * @param graph a directed graph: for good results, vertices should be numbered consecutively
     *        starting from 0.
     * @param <E> the graph edge type
     */
    public <E> SuccinctIntDirectedGraph(final Graph<Integer, E> graph)
    {

        if (graph.getType().isUndirected())
            throw new IllegalArgumentException("This class supports directed graphs only");
        assert graph.getType().isDirected();
        final GraphIterables<Integer, E> iterables = graph.iterables();
        if (iterables.vertexCount() > Integer.MAX_VALUE)
            throw new IllegalArgumentException(
                "The number of nodes (" + iterables.vertexCount() + ") is greater than "
                    + Integer.MAX_VALUE);
        if (iterables.edgeCount() > Integer.MAX_VALUE)
            throw new IllegalArgumentException(
                "The number of edges (" + iterables.edgeCount() + ") is greater than "
                    + Integer.MAX_VALUE);

        n = (int) iterables.vertexCount();
        m = (int) iterables.edgeCount();

        cumulativeOutdegrees = new EliasFanoIndexedMonotoneLongBigList(
            n + 1, m, new CumulativeDegrees(n, graph::outDegreeOf));
        cumulativeIndegrees =
            new EliasFanoMonotoneLongBigList(n + 1, m, new CumulativeDegrees(n, graph::inDegreeOf));
        assert cumulativeOutdegrees.getLong(cumulativeOutdegrees.size64() - 1) == m;
        assert cumulativeIndegrees.getLong(cumulativeIndegrees.size64() - 1) == m;

        successors = new EliasFanoIndexedMonotoneLongBigList(
            m, (long) n * n, new CumulativeSuccessors<>(graph, iterables::outgoingEdgesOf, true));
        predecessors = new EliasFanoIndexedMonotoneLongBigList(
            m, (long) n * n - m,
            new CumulativeSuccessors<>(graph, iterables::incomingEdgesOf, false));
    }

    /**
     * Creates a new immutable succinct directed graph from an edge list.
     *
     * <p>
     * This constructor just builds a {@link SparseIntDirectedGraph} and delegates to the
     * {@linkplain #SuccinctIntDirectedGraph(Graph) main constructor}.
     *
     * @param numVertices the number of vertices.
     * @param edges the edge list.
     * @see #SuccinctIntDirectedGraph(Graph)
     */

    public SuccinctIntDirectedGraph(final int numVertices, final List<Pair<Integer, Integer>> edges)
    {
        this(new SparseIntDirectedGraph(numVertices, edges));
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
        return e >= 0 && e < m;
    }

    @Override
    public boolean containsVertex(final Integer v)
    {
        return v >= 0 && v < n;
    }

    @Override
    public Set<Integer> edgeSet()
    {
        return IntSets.fromTo(0, m);
    }

    @Override
    public int degreeOf(final Integer vertex)
    {
        return inDegreeOf(vertex) + outDegreeOf(vertex);
    }

    @Override
    public IntSet edgesOf(final Integer vertex)
    {
        final IntSet result = outgoingEdgesOf(vertex);
        result.addAll(incomingEdgesOf(vertex));
        return result;
    }

    @Override
    public int inDegreeOf(final Integer vertex)
    {
        assertVertexExist(vertex);
        return (int) cumulativeIndegrees.getDelta(vertex);
    }

    @Override
    public IntSet incomingEdgesOf(final Integer target)
    {
        assertVertexExist(target);
        final int t = target;
        final long[] result = new long[2];
        cumulativeIndegrees.get(t, result);
        final int d = (int) (result[1] - result[0]);
        final LongBigListIterator iterator = predecessors.listIterator(result[0]);

        final IntOpenHashSet s = new IntOpenHashSet();
        long base = (long) n * t - result[0];

        for (int i = d; i-- != 0;) {
            final long source = iterator.nextLong() - base--;
            final int e = (int) (successors.successorIndexUnsafe(source * n + t));
            assert getEdgeSource(e).longValue() == source;
            assert getEdgeTarget(e).longValue() == target;
            s.add(e);
        }

        return s;
    }

    @Override
    public int outDegreeOf(final Integer vertex)
    {
        assertVertexExist(vertex);
        return (int) cumulativeOutdegrees.getDelta(vertex);
    }

    @Override
    public IntSet outgoingEdgesOf(final Integer vertex)
    {
        assertVertexExist(vertex);
        final long[] result = new long[2];
        cumulativeOutdegrees.get(vertex, result);
        return IntSets.fromTo((int) result[0], (int) result[1]);
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
        return IntSets.fromTo(0, n);
    }

    @Override
    public Integer getEdgeSource(final Integer e)
    {
        assertEdgeExist(e);
        return (int) (successors.getLong(e) / n);
    }

    @Override
    public Integer getEdgeTarget(final Integer e)
    {
        assertEdgeExist(e);
        return (int) (successors.getLong(e) % n);
    }

    @Override
    public GraphType getType()
    {
        return new DefaultGraphType.Builder()
            .directed().weighted(false).modifiable(false).allowMultipleEdges(false)
            .allowSelfLoops(true).build();
    }

    @Override
    public double getEdgeWeight(final Integer e)
    {
        return 1.0;
    }

    @Override
    public void setEdgeWeight(final Integer e, final double weight)
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public Integer getEdge(final Integer sourceVertex, final Integer targetVertex)
    {
        final long index = successors.indexOfUnsafe(sourceVertex * (long) n + targetVertex);
        return index != -1 ? (int) index : null;
    }

    @Override
    public boolean containsEdge(final Integer sourceVertex, final Integer targetVertex)
    {
        return successors.indexOfUnsafe(sourceVertex * (long) n + targetVertex) != -1;
    }

    @Override
    public Set<Integer> getAllEdges(final Integer sourceVertex, final Integer targetVertex)
    {
        final Integer edge = getEdge(sourceVertex, targetVertex);
        return edge == null ? IntSets.EMPTY_SET : IntSets.singleton(edge);
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
        if (v < 0 || v >= n)
            throw new IllegalArgumentException();
        return true;
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
        if (e < 0 || e >= m)
            throw new IllegalArgumentException();
        return true;

    }

    private final static class SuccinctGraphIterables
        implements
        GraphIterables<Integer, Integer>,
        Serializable
    {
        private static final long serialVersionUID = 0L;
        private final SuccinctIntDirectedGraph graph;

        private SuccinctGraphIterables()
        {
            graph = null;
        }

        private SuccinctGraphIterables(final SuccinctIntDirectedGraph graph)
        {
            this.graph = graph;
        }

        @Override
        public Graph<Integer, Integer> getGraph()
        {
            return graph;
        }

        @Override
        public long vertexCount()
        {
            return graph.n;
        }

        @Override
        public long edgeCount()
        {
            return graph.m;
        }

        @Override
        public Iterable<Integer> edgesOf(final Integer source)
        {
            return Iterables.concat(outgoingEdgesOf(source), incomingEdgesOf(source, true));
        }

        private Iterable<Integer> incomingEdgesOf(final int target, final boolean skipLoops)
        {
            final SuccinctIntDirectedGraph graph = this.graph;
            final long[] result = new long[2];
            graph.cumulativeIndegrees.get(target, result);
            final int d = (int) (result[1] - result[0]);
            final EliasFanoIndexedMonotoneLongBigList successors = graph.successors;
            final LongBigListIterator iterator = graph.predecessors.listIterator(result[0]);

            return () -> new IntIterator()
            {
                int i = d;
                int edge = -1;
                long n = graph.n;
                long base = target * n - result[0];

                @Override
                public boolean hasNext()
                {
                    if (edge == -1 && i > 0) {
                        i--;
                        final long source = iterator.nextLong() - base--;
                        if (skipLoops && source == target && i-- != 0)
                            return false;
                        final long v = source * n + target;
                        assert v == successors.successor(v) : v + " != " + successors.successor(v);
                        edge = (int) successors.successorIndexUnsafe(v);
                        assert graph.getEdgeSource(edge).longValue() == source;
                        assert graph.getEdgeTarget(edge).longValue() == target;
                    }
                    return edge != -1;
                }

                @Override
                public int nextInt()
                {
                    if (!hasNext())
                        throw new NoSuchElementException();
                    final int result = edge;
                    edge = -1;
                    return result;
                }
            };
        }

        @Override
        public Iterable<Integer> incomingEdgesOf(final Integer vertex)
        {
            return incomingEdgesOf(vertex, false);
        }
    }

    private final GraphIterables<Integer, Integer> ITERABLES = new SuccinctGraphIterables(this);

    @Override
    public GraphIterables<Integer, Integer> iterables()
    {
        return ITERABLES;
    }
}
