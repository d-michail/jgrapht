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
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jgrapht.Graph;
import org.jgrapht.GraphIterables;
import org.jgrapht.GraphType;
import org.jgrapht.Graphs;
import org.jgrapht.graph.AbstractGraph;
import org.jgrapht.graph.DefaultGraphIterables;
import org.jgrapht.graph.DefaultGraphType;

import com.google.common.collect.Iterables;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongBigListIterator;
import it.unimi.dsi.sux4j.util.EliasFanoIndexedMonotoneLongBigList;
import it.unimi.dsi.sux4j.util.EliasFanoMonotoneLongBigList;

/**
 * An immutable undirected graph represented using quasi-succinct data structures.
 *
 * <p>
 * This class is the undirected counterpart of {@link SuccintIntDirectedGraph}: the same comments apply.
 *
 * @author Sebastiano Vigna
 */

public class SuccinctIntUndirectedGraph extends AbstractGraph<Integer, Integer> implements Serializable
{
	private static final long serialVersionUID = 0L;
	protected static final String UNMODIFIABLE = "this graph is unmodifiable";

	/**
	 * Turns all lists of successors into a single monotone sequence, bumping by one the value after
	 * each list (the resulting list starts with a zero). Depending on the value of {@code sorted},
	 * only edges with source less than or equal to the target (or vice versa) are included.
	 */
	private final static class CumulativeSuccessors<E> implements LongIterator {
		private final Graph<Integer, E> graph;
		private final int n;
		private final Function<Integer, Iterable<E>> succ;
		private final boolean sorted;

		int x = -1, d, i;
		long next, last, cumul;
		int[] s = IntArrays.EMPTY_ARRAY;

		public CumulativeSuccessors(final Graph<Integer, E> graph, final boolean sorted, final Function<Integer, Iterable<E>> succ) {
			this.n = (int)graph.iterables().vertexCount();
			this.graph = graph;
			this.sorted = sorted;
			this.succ = succ;
			last = sorted ? 1 : 0;
		}

		@Override
		public boolean hasNext() {
			if (next != -1) return true;
			if (x == n) return false;
			while (i == d) {
				cumul += last;
				last = 0;
				if (++x == n) return false;
				int d = 0;
				for (final E e : succ.apply(x)) {
					final int y = Graphs.getOppositeVertex(graph, e, x);
					if (sorted) {
						if (x <= y) {
							s = IntArrays.grow(s, d + 1);
							s[d++] = y;
						}
					} else {
						if (x >= y) {
							s = IntArrays.grow(s, d + 1);
							s[d++] = y;
						}
					}
				}
				Arrays.sort(s, 0, d);
				this.d = d;
				i = 0;
			}
			next = cumul + s[i] - (sorted ? 0 : i);
			last = s[i] + (sorted ? 1 : -i);
			i++;
			return true;
		}

		@Override
		public long nextLong() {
			if (!hasNext()) throw new NoSuchElementException();
			final long result = next;
			next = -1;
			return result;
		}
	}

	/**
	 * Iterates over the cumulative degrees (starts with a zero). Depending on the value of {@code sorted},
	 * only edges with source less than or equal to the target (or vice versa) are included.
	 */
	private final static class CumulativeDegrees<E> implements LongIterator {
		private final int n;
		private int x = -1;
		private long cumul = 0;
		private final Function<Integer, Iterable<E>> succ;
		private final boolean sorted;
		private final Graph<Integer, E> graph;

		public CumulativeDegrees(final Graph<Integer, E> graph, final boolean sorted, final Function<Integer, Iterable<E>> succ) {
			this.n = (int)graph.iterables().vertexCount();
			this.graph = graph;
			this.succ = succ;
			this.sorted = sorted;
		}

		@Override
		public boolean hasNext() {
			return x < n;
		}

		@Override
		public long nextLong() {
			if (!hasNext()) throw new NoSuchElementException();
			if (x == -1) return ++x;
			int d = 0;
			if (sorted) {
				for (final E e : succ.apply(x)) if (x <= Graphs.getOppositeVertex(graph, e, x)) d++;
			} else {
				for (final E e : succ.apply(x)) if (x >= Graphs.getOppositeVertex(graph, e, x)) d++;
			}
			x++;
			return cumul += d;
		}
	}

	/** The number of vertices in the graph. */
	private final int n;
	/** The number of edges in the graph. */
	private final int m;
	/** The cumulative list of outdegrees (number of edges in sorted order, including loops). */
	private final EliasFanoIndexedMonotoneLongBigList cumulativeOutdegrees;
	/** The cumulative list of indegrees (number of edges in reversed order, including loops). */
	private final EliasFanoMonotoneLongBigList cumulativeIndegrees;
	/** The cumulative list of successor (edges in sorted order, including loops) lists. */
	private final EliasFanoIndexedMonotoneLongBigList successors;
	/** The cumulative list of predecessor (edges in reversed order, including loops) lists. */
	private final EliasFanoMonotoneLongBigList predecessors;

    /**
	 * Creates a new immutable succinct undirected graph from a given undirected graph.
	 *
	 * @param graph an undirected graph: for good results, vertices should be numbered consecutively
	 *            starting from 0.
	 */
	public <E> SuccinctIntUndirectedGraph(final Graph<Integer, E> graph)
    {
		if (graph.getType().isDirected()) throw new IllegalArgumentException("This class supports directed graphs only");
		assert graph.getType().isUndirected();
		final GraphIterables<Integer, E> iterables = graph.iterables();
		if (iterables.vertexCount() > Integer.MAX_VALUE) throw new IllegalArgumentException("The number of nodes (" + iterables.vertexCount() + ") is greater than " + Integer.MAX_VALUE);
		if (iterables.edgeCount() > Integer.MAX_VALUE) throw new IllegalArgumentException("The number of edges (" + iterables.edgeCount() + ") is greater than " + Integer.MAX_VALUE);
		n = (int)iterables.vertexCount();
		m = (int)iterables.edgeCount();

		long forwardUpperBound = 0, backwardUpperBound = 0;

		for (int x = 0; x < n; x++) {
			int maxSucc = -1;
			for (final E e : iterables.outgoingEdgesOf(x)) {
				final int y = Graphs.getOppositeVertex(graph, e, x);
				if (y >= x) maxSucc = Math.max(maxSucc, y);
			}
			if (maxSucc != -1) forwardUpperBound += maxSucc + 1;

			int maxPred = -1, d = 0;
			for (final E e : iterables.incomingEdgesOf(x)) {
				final int y = Graphs.getOppositeVertex(graph, e, x);
				if (y <= x) {
					maxPred = Math.max(maxPred, y);
					d++;
				}
			}
			if (maxPred != -1) backwardUpperBound += maxPred - d + 1;
		}

		cumulativeOutdegrees = new EliasFanoIndexedMonotoneLongBigList(n + 1, m, new CumulativeDegrees<>(graph, true, iterables::edgesOf));
		cumulativeIndegrees = new EliasFanoMonotoneLongBigList(n + 1, m, new CumulativeDegrees<>(graph, false, iterables::edgesOf));
		assert cumulativeOutdegrees.getLong(cumulativeOutdegrees.size64() - 1) == m;
		assert cumulativeIndegrees.getLong(cumulativeIndegrees.size64() - 1) == m;

		successors = new EliasFanoIndexedMonotoneLongBigList(m + 1, forwardUpperBound, new CumulativeSuccessors<>(graph, true, iterables::outgoingEdgesOf));
		predecessors = new EliasFanoIndexedMonotoneLongBigList(m + 1, backwardUpperBound, new CumulativeSuccessors<>(graph, false, iterables::incomingEdgesOf));
		assert successors.getLong(successors.size64() - 1) == forwardUpperBound;
		assert predecessors.getLong(predecessors.size64() - 1) == backwardUpperBound;
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
		return (int)cumulativeIndegrees.getDelta(vertex) + (int)cumulativeOutdegrees.getDelta(vertex);
	}

    @Override
	public IntSet edgesOf(final Integer vertex)
    {
		final long[] result = new long[2];
		cumulativeOutdegrees.get(vertex, result);
		final IntSet s = new IntOpenHashSet(IntSets.fromTo((int)result[0], (int)result[1]));
		for (final int e : ITERABLES.reverseSortedEdgesOfNoLoops(vertex)) s.add(e);
		return s;
	}

    @Override
    public int inDegreeOf(final Integer vertex)
    {
		return degreeOf(vertex);
	}

    @Override
	public IntSet incomingEdgesOf(final Integer vertex)
    {
		return edgesOf(vertex);
    }

    @Override
    public int outDegreeOf(final Integer vertex)
    {
		return degreeOf(vertex);
	}

    @Override
	public IntSet outgoingEdgesOf(final Integer vertex)
    {
		return edgesOf(vertex);
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
		return (int)cumulativeOutdegrees.weakPredecessorIndex(e);
    }

    @Override
    public Integer getEdgeTarget(final Integer e)
    {
		assertEdgeExist(e);
		final long cumul = cumulativeOutdegrees.weakPredecessor(e);
		return (int)(successors.getLong(e + 1) - successors.getLong(cumul) - 1);
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
		int x = sourceVertex;
		int y = targetVertex;
		if (x > y) {
			final int t = x;
			x = y;
			y = t;
		}
		final long[] result = new long[2];
		cumulativeOutdegrees.get(x, result);
		final long v = successors.getLong(result[0]) + y + 1;
		final long index = successors.indexOf(v);
		return index != -1 && index <= result[1] ? (int)index - 1 : null;
    }

	@Override
	public boolean containsEdge(final Integer sourceVertex, final Integer targetVertex) {
		int x = sourceVertex;
		int y = targetVertex;
		if (x > y) {
			final int t = x;
			x = y;
			y = t;
		}
		final long[] result = new long[2];
		cumulativeOutdegrees.get(x, result);
		final long v = successors.getLong(result[0]) + y + 1;
		final long index = successors.indexOf(v);
		return index != -1 && index <= result[1];
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
		if (v < 0 || v >= n) throw new IllegalArgumentException();
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
		if (e < 0 || e >= m) throw new IllegalArgumentException();
		return true;

    }

	// This kluge is necessary as DefaultGraphIterables does not have a no-arg constructor
	private static class KlugeGraphIterables extends DefaultGraphIterables<Integer, Integer> {
		protected KlugeGraphIterables() {
			super(null);
		}

		protected KlugeGraphIterables(final SuccinctIntUndirectedGraph graph) {
			super(graph);
		}
	}

	private final static class SuccinctGraphIterables implements GraphIterables<Integer, Integer>, Serializable {
		private static final long serialVersionUID = 0L;
		private final SuccinctIntUndirectedGraph graph;

		private SuccinctGraphIterables() {
			graph = null;
		}

		private SuccinctGraphIterables(final SuccinctIntUndirectedGraph graph) {
			this.graph = graph;
		}

		@Override
		public Graph<Integer, Integer> getGraph() {
			return graph;
		}

		@Override
		public long vertexCount() {
			return graph.n;
		}

		@Override
		public long edgeCount() {
			return graph.m;
		}

		@Override
		public Iterable<Integer> edgesOf(final Integer source) {
			final long[] result = new long[2];
			graph.cumulativeOutdegrees.get(source, result);
			return Iterables.concat(IntSets.fromTo((int)result[0], (int)result[1]), reverseSortedEdgesOfNoLoops(source));
		}

		private Iterable<Integer> reverseSortedEdgesOfNoLoops(final int target) {
			final SuccinctIntUndirectedGraph graph = this.graph;
			final long[] result = new long[2];
			graph.cumulativeIndegrees.get(target, result);
			final int d = (int)(result[1] - result[0]);
			final EliasFanoIndexedMonotoneLongBigList successors = graph.successors;
			final LongBigListIterator iterator = graph.predecessors.listIterator(result[0]);

			return () -> new IntIterator() {
				int i = 0;
				int edge = -1;
				long base = iterator.nextLong();

				@Override
				public boolean hasNext() {
					if (edge == -1 && i < d) {
						final long source = iterator.nextLong() - ++base;
						if (source == target && ++i == d) return false;
						final long v = successors.getLong(graph.cumulativeOutdegrees.getLong(source)) + target + 1;
						assert v == successors.successor(v) : v + " != " + successors.successor(v);
						edge = (int)successors.successorIndex(v) - 1;
						assert graph.getEdgeSource(edge).longValue() == source;
						assert graph.getEdgeTarget(edge).longValue() == target;
						i++;
					}
					return edge != -1;
				}

				@Override
				public int nextInt() {
					if (! hasNext()) throw new NoSuchElementException();
					final int result = edge;
					edge = -1;
					return result;
				}
			};
		}

		@Override
		public Iterable<Integer> incomingEdgesOf(final Integer vertex) {
			return edgesOf(vertex);
		}

		@Override
		public Iterable<Integer> outgoingEdgesOf(final Integer vertex) {
			return edgesOf(vertex);
		}
	}

	private final SuccinctGraphIterables ITERABLES = new SuccinctGraphIterables(this);

	@Override
	public GraphIterables<Integer, Integer> iterables() {
		return ITERABLES;
	}
}
