/*
 * (C) Copyright 2020-2020, by Dimitrios Michail and Contributors.
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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.GraphTests;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.AsSubgraph;

/**
 * A class for the verification of a feedback vertex set.
 * 
 * <p>
 * Given a graph $G$ and a set of vertices $X \subseteq V$, the algorithm verifies whether the set
 * $X$ is a feedback vertex set. The algorithm constructs the graph $G'$ with the vertices of $X$
 * removed and checks if it is cycle free. Its running time is expected linear $O(n+m)$.
 * 
 * @author Dimitrios Michail
 *
 * @param <V> the vertex type
 * @param <E> the edge type
 */
public class FeedbackVertexSetVerifier<V, E>
{
    private Graph<V, E> graph;
    private Set<V> feedbackVertexSet;

    /**
     * Constructor
     * 
     * @param graph the graph to check against
     * @param feedbackVertexSet the feedback vertex set to check
     */
    public FeedbackVertexSetVerifier(Graph<V, E> graph, Set<V> feedbackVertexSet)
    {
        this.graph = GraphTests.requireDirectedOrUndirected(graph);
        this.feedbackVertexSet = Objects.requireNonNull(feedbackVertexSet);
    }

    /**
     * Verify whether the provided set is indeed a feedback vertex set.
     * 
     * @return true iff the set is a feedback vertex set
     */
    public boolean verify()
    {
        Set<V> existing = new HashSet<>(graph.vertexSet());
        existing.removeAll(feedbackVertexSet);

        Graph<V, E> subgraph = new AsSubgraph<>(graph, existing);

        if (subgraph.getType().isDirected()) {
            return !(new CycleDetector<>(subgraph).detectCycles());
        } else {
            return GraphTests.isForest(subgraph);
        }
    }

}
