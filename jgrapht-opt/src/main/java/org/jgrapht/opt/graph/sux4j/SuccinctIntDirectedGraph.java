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
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jgrapht.DefaultGraphIterables;
import org.jgrapht.Graph;
import org.jgrapht.GraphIterables;
import org.jgrapht.GraphType;
import org.jgrapht.Graphs;
import org.jgrapht.graph.AbstractGraph;
import org.jgrapht.graph.DefaultGraphType;

import com.google.common.collect.Iterables;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterators;
import it.unimi.dsi.lang.FlyweightPrototype;

/**
 * A directed graph represented using quasi-succinct data structures.
 *
 * @author Sebastiano Vigna
 */

public class SuccinctIntDirectedGraph extends AbstractGraph<Integer, Integer> implements Serializable, FlyweightPrototype<SuccinctIntDirectedGraph>
{
	private static final long serialVersionUID = 0L;
	protected static final String UNMODIFIABLE = "this graph is unmodifiable";

	private final static class CumulativeSuccessors<E> implements LongIterator {
		private final Graph<Integer, E> graph;
		int x = -1;
		long next = 0, last, cumul = 0;
		Iterator<E> successors = ObjectIterators.emptyIterator();
		private final Function<Integer, Iterable<E>> succ;
		private final int n;

		public CumulativeSuccessors(final Graph<Integer, E> graph, final Function<Integer, Iterable<E>> succ) {
			this.n = (int)graph.iterables().vertexCount();
			this.graph = graph;
			this.succ = succ;
		}

		@Override
		public boolean hasNext() {
			if (next != -1) return true;
			if (x == n) return false;
			while (!successors.hasNext()) {
				cumul += last;
				last = 0;
				if (++x == n) return false;
				successors = succ.apply(x).iterator();
			}
			next = (last = Graphs.getOppositeVertex(graph, successors.next(), x)) + cumul + x + 1;
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

	private final static class CumulativeDegrees implements LongIterator {
		private final Function<Integer, Integer> degreeOf;
		private final int n;
		private int i = -1;
		private long cumul = 0;

		public CumulativeDegrees(final int n, final Function<Integer, Integer> degreeOf) {
			this.n = n;
			this.degreeOf = degreeOf;
		}

		@Override
		public boolean hasNext() {
			return i < n;
		}

		@Override
		public long nextLong() {
			if (!hasNext()) throw new NoSuchElementException();
			if (i == -1) return ++i;
			return cumul += degreeOf.apply(i++);
		}
	}

	/** The number of vertices in the graph. */
	private final int n;
	/** The number of egdges in the graph. */
	private final int m;
	/** The cumulative list of outdegrees. */
	private final EliasFanoIndexedMonotoneLongBigList cumulativeOutdegrees;
	/** The cumulative list of indegrees. */
	private final EliasFanoIndexedMonotoneLongBigList cumulativeIndegrees;
	/** The cumulative list of successor lists. */
	private final EliasFanoIndexedMonotoneLongBigList successors;
	/** The cumulative list of predecessor lists. */
	private final EliasFanoIndexedMonotoneLongBigList predecessors;

	protected SuccinctIntDirectedGraph(final int n, final int m, final EliasFanoIndexedMonotoneLongBigList cumulativeOutdegrees, final EliasFanoIndexedMonotoneLongBigList cumulativeIndegrees, final EliasFanoIndexedMonotoneLongBigList successors, final EliasFanoIndexedMonotoneLongBigList predecessors) {
		this.cumulativeOutdegrees = cumulativeOutdegrees;
		this.cumulativeIndegrees = cumulativeIndegrees;
		this.successors = successors;
		this.predecessors = predecessors;
		this.n = n;
		this.m = m;
	}

    /**
	 * Create a new succinct directed graph from a given graph.
	 *
	 * @param graph a directed graph.
	 */
	public <E> SuccinctIntDirectedGraph(final Graph<Integer, E> graph)
    {

		if (graph.getType().isUndirected()) throw new IllegalArgumentException("This class supports directed graphs only");
		final GraphIterables<Integer, E> iterables = graph.iterables();
		n = (int)iterables.vertexCount();
		m = (int)iterables.edgeCount();
		long forwardUpperBound = 0;
		long backwardUpperBound = 0;
		for (int x = 0; x < n; x++) {
			int maxSucc = 0;
			for (final E e : iterables.outgoingEdgesOf(x)) maxSucc = Math.max(maxSucc, Graphs.getOppositeVertex(graph, e, x));
			forwardUpperBound += maxSucc;
			int maxPred = 0;
			for (final E e : iterables.incomingEdgesOf(x)) maxPred = Math.max(maxPred, Graphs.getOppositeVertex(graph, e, x));
			backwardUpperBound += maxPred;
		}

		cumulativeOutdegrees = new EliasFanoIndexedMonotoneLongBigList(n + 1, m, new CumulativeDegrees(n, graph::outDegreeOf));
		cumulativeIndegrees = new EliasFanoIndexedMonotoneLongBigList(n + 1, m, new CumulativeDegrees(n, graph::inDegreeOf));

		successors = new EliasFanoIndexedMonotoneLongBigList(m + 1, forwardUpperBound + n, new CumulativeSuccessors<>(graph, iterables::outgoingEdgesOf));
		predecessors = new EliasFanoIndexedMonotoneLongBigList(m + 1, backwardUpperBound + n, new CumulativeSuccessors<>(graph, iterables::incomingEdgesOf));

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
		final long[] result = new long[2];
		cumulativeIndegrees.get(vertex, result);
		return (int)(result[1] - result[0]);
	}

    @Override
	public IntSet incomingEdgesOf(final Integer vertex)
    {
        assertVertexExist(vertex);
		final long[] result = new long[2];
		cumulativeIndegrees.get(vertex, result);
		final int d = (int)(result[1] - result[0]);
		final long pred[] = new long[d + 1];
		predecessors.get(result[0], pred, 0, d + 1);

		final IntOpenHashSet s = new IntOpenHashSet();
		final long base = pred[0] + 1;
		for (int i = 1; i <= d; i++) {
			successors.successor(successors.getLong(cumulativeOutdegrees.getLong(pred[i] - base)) + vertex + 1);
			final int e = (int)successors.index() - 1;
			assert getEdgeSource(e).longValue() == pred[i] - base;
			assert getEdgeTarget(e).longValue() == vertex;
			s.add(e);
		}

		return s;
    }

    @Override
    public int outDegreeOf(final Integer vertex)
    {
        assertVertexExist(vertex);
		final long[] result = new long[2];
		cumulativeOutdegrees.get(vertex, result);
		return (int)(result[1] - result[0]);
	}

    @Override
	public IntSet outgoingEdgesOf(final Integer vertex)
    {
        assertVertexExist(vertex);
		final long[] result = new long[2];
		cumulativeOutdegrees.get(vertex, result);
		return IntSets.fromTo((int)result[0], (int)result[1]);
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
		cumulativeOutdegrees.weakPredecessor(e);
		return (int)cumulativeOutdegrees.index();
    }

    @Override
    public Integer getEdgeTarget(final Integer e)
    {
		assertEdgeExist(e);
		final long cumul = cumulativeOutdegrees.weakPredecessor(e);
		final long base = successors.getLong(cumul) + 1;
		return (int)(successors.getLong(e + 1) - base);
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
		final long[] result = new long[2];
		cumulativeOutdegrees.get(sourceVertex, result);
		final long v = successors.getLong(result[0]) + targetVertex + 1;
		return successors.successor(v) == v && successors.index() <= result[1] ? (int)successors.index() - 1 : null;
    }

	@Override
	public boolean containsEdge(final Integer sourceVertex, final Integer targetVertex) {
		final long[] result = new long[2];
		cumulativeOutdegrees.get(sourceVertex, result);
		final long v = successors.getLong(result[0]) + targetVertex + 1;
		return successors.successor(v) == v && successors.index() <= result[1];
	}

    @Override
    public Set<Integer> getAllEdges(final Integer sourceVertex, final Integer targetVertex)
    {
		return IntSets.singleton(getEdge(sourceVertex, targetVertex));
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

	private final static class DefaultGraphIterablesExtension extends DefaultGraphIterables<Integer, Integer> implements Serializable {
		private static final long serialVersionUID = 0L;
		private final SuccinctIntDirectedGraph graph;
		private DefaultGraphIterablesExtension(final SuccinctIntDirectedGraph graph) {
			super(graph);
			this.graph = graph;
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
			// TODO: eliminate duplicate loop
			return Iterables.concat(outgoingEdgesOf(source), incomingEdgesOf(source));
		}

		@Override
		public Iterable<Integer> incomingEdgesOf(final Integer vertex) {
			final SuccinctIntDirectedGraph graph = this.graph;
			graph.assertVertexExist(vertex);
			final long[] result = new long[2];
			graph.cumulativeIndegrees.get(vertex, result);
			final int d = (int)(result[1] - result[0]);
			final long pred[] = new long[d + 1];
			graph.predecessors.get(result[0], pred, 0, d + 1);

			final long base = pred[0] + 1;
			return () -> new IntIterator() {
				int i = 1;

				@Override
				public boolean hasNext() {
					return i <= d;
				}

				@Override
				public int nextInt() {
					final EliasFanoIndexedMonotoneLongBigList successors = graph.successors;
					successors.successor(successors.getLong(graph.cumulativeOutdegrees.getLong(pred[i] - base)) + vertex + 1);
					final int e = (int)successors.index() - 1;
					assert graph.getEdgeSource(e).longValue() == pred[i] - base;
					assert graph.getEdgeTarget(e).longValue() == vertex;
					i++;
					return e;
				}
			};
		}
	}

	private final GraphIterables<Integer, Integer> ITERABLES = new DefaultGraphIterablesExtension(this);

	@Override
	public GraphIterables<Integer, Integer> iterables() {
		return ITERABLES;
	}

	@Override
	public SuccinctIntDirectedGraph copy() {
		return new SuccinctIntDirectedGraph(n, m, cumulativeOutdegrees, cumulativeIndegrees, successors, predecessors);
	}
}
