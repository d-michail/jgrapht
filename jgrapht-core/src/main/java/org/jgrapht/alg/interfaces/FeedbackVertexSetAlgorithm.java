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
package org.jgrapht.alg.interfaces;

import java.util.Set;
import java.util.function.Function;

import org.jgrapht.util.WeightedUnmodifiableSet;

/**
 * Computes a (weighted) <a href="https://en.wikipedia.org/wiki/Feedback_vertex_set">feedback vertex
 * set</a> of a directed or undirected graph.
 *
 * <p>
 * A feedback vertex set is a set of vertices whose removal leaves the graph without cycles. In
 * other words, a feedback vertex set contains at least one vertex of any cycle in the graph. In
 * artificial intelligence the problem is also called a loop cutset.
 *
 * <p>
 * The feedback vertex set problem is NP-hard.
 *
 * @param <V> the graph vertex type
 */
public interface FeedbackVertexSetAlgorithm<V>
{
    /**
     * Compute a weighted feedback vertex set.
     *
     * @param vertexWeights function for the vertex weights
     * @return a feedback vertex set
     */
    FeedbackVertexSet<V> getFeedbackVertexSet(Function<V, Double> vertexWeights);

    /**
     * Compute a feedback vertex set.
     *
     * @return a feedback vertex set
     */
    default FeedbackVertexSet<V> getFeedbackVertexSet()
    {
        return getFeedbackVertexSet(v -> 1.0);
    }

    /**
     * A feedback vertex set
     *
     * @param <V> the graph vertex type
     * @param <E> the graph edge type
     */
    interface FeedbackVertexSet<V>
        extends
        Set<V>
    {
        /**
         * Returns the weight of the feedback vertex set.
         *
         * @return the weight of the feedback vertex set
         */
        double getWeight();
    }

    /**
     * Default implementation of a (weighted) feedback vertex set
     *
     * @param <V> the vertex type
     */
    class FeedbackVertexSetImpl<V>
        extends
        WeightedUnmodifiableSet<V>
        implements
        FeedbackVertexSet<V>
    {
        private static final long serialVersionUID = 6907759744551524082L;

        public FeedbackVertexSetImpl(Set<V> feedbackVertexSet)
        {
            super(feedbackVertexSet);
        }

        public FeedbackVertexSetImpl(Set<V> feedbackVertexSet, double weight)
        {
            super(feedbackVertexSet, weight);
        }
    }

}
