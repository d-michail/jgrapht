/*
 * (C) Copyright 2018-2020, by Dimitrios Michail and Contributors.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jgrapht.util.SupplierUtil;
import org.junit.Test;

/**
 * Tests for {@link WeisfeilerLehmanIsomorphismInspector}.
 */
public class WeisfeilerLehmanIsomorphismInspectorTest
{

    @Test
    public void testSameGraph()
    {
        SimpleGraph<String, DefaultEdge> g1 = new SimpleGraph<>(DefaultEdge.class);

        String v1 = "v1", v2 = "v2", v3 = "v3";

        g1.addVertex(v1);
        g1.addVertex(v2);
        g1.addVertex(v3);

        g1.addEdge(v1, v2);
        g1.addEdge(v2, v3);
        g1.addEdge(v3, v1);

        SimpleGraph<String, DefaultEdge> g2 = new SimpleGraph<>(DefaultEdge.class);

        g2.addVertex(v1);
        g2.addVertex(v2);
        g2.addVertex(v3);

        g2.addEdge(v1, v2);
        g2.addEdge(v2, v3);
        g2.addEdge(v3, v1);

        WeisfeilerLehmanIsomorphismInspector<String, DefaultEdge> alg =
            new WeisfeilerLehmanIsomorphismInspector<>(g1, g2, 10);
        assertTrue(alg.isomorphismExists());
    }

    @Test
    public void testFailing()
    {
        Graph<Integer,
            DefaultEdge> g1 = GraphTypeBuilder
                .undirected().allowingMultipleEdges(false).allowingSelfLoops(false)
                .vertexSupplier(SupplierUtil.createIntegerSupplier())
                .edgeSupplier(SupplierUtil.DEFAULT_EDGE_SUPPLIER).buildGraph();

        Integer v1 = g1.addVertex();
        Integer v2 = g1.addVertex();
        Integer v3 = g1.addVertex();
        Integer v4 = g1.addVertex();
        Integer v5 = g1.addVertex();
        Integer v6 = g1.addVertex();
        g1.addEdge(v1, v2);
        g1.addEdge(v2, v3);
        g1.addEdge(v3, v1);
        g1.addEdge(v4, v5);
        g1.addEdge(v5, v6);
        g1.addEdge(v6, v4);

        Graph<Integer,
            DefaultEdge> g2 = GraphTypeBuilder
                .undirected().allowingMultipleEdges(false).allowingSelfLoops(false)
                .vertexSupplier(SupplierUtil.createIntegerSupplier())
                .edgeSupplier(SupplierUtil.DEFAULT_EDGE_SUPPLIER).buildGraph();

        Integer v7 = g2.addVertex();
        Integer v8 = g2.addVertex();
        Integer v9 = g2.addVertex();
        Integer v10 = g2.addVertex();
        Integer v11 = g2.addVertex();
        Integer v12 = g2.addVertex();
        g2.addEdge(v7, v8);
        g2.addEdge(v8, v9);
        g2.addEdge(v9, v10);
        g2.addEdge(v10, v11);
        g2.addEdge(v11, v12);
        g2.addEdge(v12, v7);

        WeisfeilerLehmanIsomorphismInspector<Integer, DefaultEdge> alg =
            new WeisfeilerLehmanIsomorphismInspector<>(g1, g2, 10);

        // known to fail to understand that the two graphs are not isomorphic
        assertTrue(alg.isomorphismExists());
    }

    @Test
    public void testGetMappingsForNotIsomorphicForests()
    {
        Graph<Integer, DefaultEdge> graph1 = new DefaultUndirectedGraph<>(DefaultEdge.class);
        Graph<Integer, DefaultEdge> graph2 = new DefaultUndirectedGraph<>(DefaultEdge.class);

        Graphs.addAllVertices(graph1, Arrays.asList(1, 2, 3, 4));
        graph1.addEdge(1, 2);
        graph1.addEdge(1, 3);
        graph1.addEdge(1, 4);

        Graphs.addAllVertices(graph2, Arrays.asList(1, 2, 3, 4));
        graph2.addEdge(1, 2);
        graph2.addEdge(1, 3);
        graph2.addEdge(2, 4);

        WeisfeilerLehmanIsomorphismInspector<Integer, DefaultEdge> alg =
            new WeisfeilerLehmanIsomorphismInspector<>(graph1, graph2, 10);

        assertFalse(alg.isomorphismExists());
    }

    @Test
    public void testTwoDiscreteGraphsNonIsomorphic()
    {
        Graph<Integer, DefaultEdge> graph1 = new DefaultUndirectedGraph<>(DefaultEdge.class);
        Graph<Integer, DefaultEdge> graph2 = new DefaultUndirectedGraph<>(DefaultEdge.class);

        Graphs.addAllVertices(graph1, Arrays.asList(1, 2, 3, 4, 5, 6));
        graph1.addEdge(1, 2);
        graph1.addEdge(2, 3);
        graph1.addEdge(3, 4);
        graph1.addEdge(3, 5);
        graph1.addEdge(4, 5);
        graph1.addEdge(5, 6);

        Graphs.addAllVertices(graph2, Arrays.asList(1, 2, 3, 4, 5, 6));
        graph2.addEdge(1, 2);
        graph2.addEdge(2, 3);
        graph2.addEdge(2, 4);
        graph2.addEdge(2, 5);
        graph2.addEdge(3, 5);
        graph2.addEdge(3, 6);
        graph2.addEdge(4, 5);

        WeisfeilerLehmanIsomorphismInspector<Integer, DefaultEdge> alg =
            new WeisfeilerLehmanIsomorphismInspector<>(graph1, graph2, 10);

        assertFalse(alg.isomorphismExists());
    }

    @Test
    public void testIsomorphicForests()
    {
        Graph<Integer, DefaultEdge> graph1 = new DefaultUndirectedGraph<>(DefaultEdge.class);
        Graph<Integer, DefaultEdge> graph2 = new DefaultUndirectedGraph<>(DefaultEdge.class);

        Graphs.addAllVertices(graph1, Arrays.asList(1, 2, 3, 4, 5, 6, 7));
        graph1.addEdge(1, 2);
        graph1.addEdge(1, 3);
        graph1.addEdge(4, 5);
        graph1.addEdge(5, 6);
        graph1.addEdge(6, 7);

        Graphs.addAllVertices(graph2, Arrays.asList(1, 2, 3, 4, 5, 6, 7));
        graph2.addEdge(1, 2);
        graph2.addEdge(1, 3);
        graph2.addEdge(3, 4);
        graph2.addEdge(5, 6);
        graph2.addEdge(6, 7);

        WeisfeilerLehmanIsomorphismInspector<Integer, DefaultEdge> alg =
            new WeisfeilerLehmanIsomorphismInspector<>(graph1, graph2, 10);

        assertTrue(alg.isomorphismExists());
    }

}
