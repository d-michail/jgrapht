/*
 * (C) Copyright 2020-2020 Dimitrios Michail and Contributors.
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
package org.jgrapht.alg.isomorphism;

import java.util.Iterator;
import java.util.Objects;

import org.jgrapht.Graph;
import org.jgrapht.GraphMapping;
import org.jgrapht.GraphType;
import org.jgrapht.alg.interfaces.VertexLabelingAlgorithm.Labeling;
import org.jgrapht.alg.labeling.WeisfeilerLehmanLabeling;

/**
 * The 1-dimensional Weisfeiler-Lehman isomorphism test.
 * 
 * <p>
 * The algorithm iteratively computes the {@link WeisfeilerLehmanLabeling} of each graph. If the
 * labeling are identical (same labels with same frequencies) in all iterations, the two graphs are
 * declared isomorphic.
 * 
 * <p>
 * Note that this algorithm may <b>not</b> be able to determine that the two input graphs are not
 * isomorphic. See the following paper
 * <ul>
 * <li>J.-Y. Cai, M. Furer, and N. Immerman. An optimal lower bound on the number of variables for
 * graph identification. Combinatorica, 12(4):389–410, 1992.</li>
 * </ul>
 * for examples of graphs that cannot be distinguished by this algorithm or its higher dimensional
 * variants. On the other hand, see
 * <ul>
 * <li>L. Babai and L. Kucera. Canonical labelling of graphs in linear average time. In Proceedings
 * Symposium on Foundations of Computer Science, pages 39–46, 1979.</li>
 * </ul>
 * which shows that the 1-dimensional Weisfeiler-Lehman is a valid isomorphism test for almost all
 * graphs.
 * 
 * <p>
 * The algorithm only decides (sometimes erroneously) on whether two graphs are isomorphic, but
 * cannot compute an actual isomorphism. Therefore, method {@link #getMappings()} always throws an
 * {@link UnsupportedOperationException}.
 * 
 * @author Dimitrios Michail
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 */
public class WeisfeilerLehmanIsomorphismInspector<V, E>
    implements
    IsomorphismInspector<V, E>
{
    private Graph<V, E> graph1, graph2;
    private int iterations;

    /**
     * Constructor.
     *
     * @param graph1 the first graph
     * @param graph2 the second graph
     * @param iterations number of iterations of the algorithm
     */
    public WeisfeilerLehmanIsomorphismInspector(
        Graph<V, E> graph1, Graph<V, E> graph2, int iterations)
    {
        this.graph1 = Objects.requireNonNull(graph1);
        this.graph2 = Objects.requireNonNull(graph2);

        GraphType type1 = graph1.getType();
        GraphType type2 = graph2.getType();

        if (type1.isMixed() || type2.isMixed()) {
            throw new IllegalArgumentException("mixed graphs not supported");
        }

        if (type1.isUndirected() && type2.isDirected()
            || type1.isDirected() && type2.isUndirected())
        {
            throw new IllegalArgumentException(
                "can not match directed with " + "undirected graphs");
        }

        if (iterations < 1) {
            throw new IllegalArgumentException("Iterations must be positive");
        }
        this.iterations = iterations;
    }

    @Override
    public Iterator<GraphMapping<V, E>> getMappings()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isomorphismExists()
    {
        Iterator<Labeling<V>> it1 =
            new WeisfeilerLehmanLabeling<>(graph1, Integer.MAX_VALUE).iterator();
        Iterator<Labeling<V>> it2 =
            new WeisfeilerLehmanLabeling<>(graph2, Integer.MAX_VALUE).iterator();

        int i = 0;
        while (i++ < iterations) {
            Labeling<V> label1 = it1.next();
            Labeling<V> label2 = it2.next();

            if (!WeisfeilerLehmanLabeling.isTheSameLabeling(label1, label2)) {
                return false;
            }
        }

        return true;
    }

}
