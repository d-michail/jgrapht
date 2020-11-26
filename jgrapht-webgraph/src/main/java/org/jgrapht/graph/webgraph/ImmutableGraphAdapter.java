/*
 * (C) Copyright 2019-2020, by Sebastiano vigna
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

package org.jgrapht.graph.webgraph;

import java.util.Arrays;
import java.util.Iterator;

import org.jgrapht.Graph;
import org.jgrapht.GraphIterables;
import org.jgrapht.Graphs;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.webgraph.AbstractLazyIntIterator;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;

public class ImmutableGraphAdapter<V, E> extends ImmutableGraph {

	private final Graph<V, E> graph;
	private final GraphIterables<V, E> iterables;
	private final V[] node2Vertex;
	private final Reference2IntOpenHashMap<V> vertex2Node;

	public ImmutableGraphAdapter(final Graph<V, E> graph) {
		this.graph = graph;
		this.iterables = graph.iterables();
		node2Vertex = (V[])graph.vertexSet().toArray();
		vertex2Node = new Reference2IntOpenHashMap<>(node2Vertex.length, Hash.FAST_LOAD_FACTOR);
		int i = 0;
		for (final V v : node2Vertex) vertex2Node.put(v, i++);
	}

	@Override
	public int numNodes() {
		return (int)iterables.vertexCount();
	}

	@Override
	public long numArcs() {
		return iterables.edgeCount();
	}

	@Override
	public boolean randomAccess() {
		return true;
	}

	@Override
	public int outdegree(final int x) {
		return graph.outDegreeOf(node2Vertex[x]);
	}

	@Override
	public boolean hasCopiableIterators() {
		// TODO Auto-generated method stub
		return super.hasCopiableIterators();
	}

	@Override
	public LazyIntIterator successors(final int x) {
		final V v = node2Vertex[x];
		final Iterator<E> outgoingEdgesOf = iterables.outgoingEdgesOf(v).iterator();
		return new AbstractLazyIntIterator() {
			@Override
			public int nextInt() {
				if (outgoingEdgesOf.hasNext()) return vertex2Node.getInt(Graphs.getOppositeVertex(graph, outgoingEdgesOf.next(), v));
				return -1;
			}
		};
	}

	@Override
	public int[] successorArray(final int x) {
		final V v = node2Vertex[x];
		final Iterator<E> outgoingEdgesOf = iterables.outgoingEdgesOf(v).iterator();
		final int d = graph.outDegreeOf(v);
		final int[] a = new int[d];
		for (int i = 0; i < d; i++) a[i] = vertex2Node.getInt(Graphs.getOppositeVertex(graph, outgoingEdgesOf.next(), v));
		Arrays.sort(a);
		return a;
	}

	@Override
	public ImmutableGraph copy() {
		return this;
	}
}
