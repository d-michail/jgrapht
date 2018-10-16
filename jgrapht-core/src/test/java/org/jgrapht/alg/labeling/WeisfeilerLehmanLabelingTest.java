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
package org.jgrapht.alg.labeling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.VertexLabelingAlgorithm.Labeling;
import org.jgrapht.generate.GridGraphGenerator;
import org.jgrapht.generate.RingGraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jgrapht.util.SupplierUtil;
import org.junit.Test;

/**
 * Tests for {@link WeisfeilerLehmanLabeling}.
 */
public class WeisfeilerLehmanLabelingTest
{

    @Test
    public void test1()
    {
        SimpleGraph<String, DefaultEdge> g1 = new SimpleGraph<>(DefaultEdge.class);

        String v1 = "v1", v2 = "v2", v3 = "v3";

        g1.addVertex(v1);
        g1.addVertex(v2);
        g1.addVertex(v3);

        g1.addEdge(v1, v2);
        g1.addEdge(v2, v3);
        g1.addEdge(v3, v1);

        WeisfeilerLehmanLabeling<String, DefaultEdge> alg = new WeisfeilerLehmanLabeling<>(g1, 3);
        Map<String, String> label = alg.getLabeling().getLabels();
        assertEquals(label.get(v1), "1");
        assertEquals(label.get(v2), "1");
        assertEquals(label.get(v3), "1");
    }

    @Test
    public void test2()
    {
        SimpleGraph<String, DefaultEdge> g1 = new SimpleGraph<>(DefaultEdge.class);

        g1.addVertex("1");
        g1.addVertex("2");
        g1.addVertex("3");
        g1.addVertex("4");
        g1.addVertex("5");
        g1.addVertex("6");

        g1.addEdge("1", "2");
        g1.addEdge("3", "4");
        g1.addEdge("5", "6");
        g1.addEdge("1", "3");
        g1.addEdge("2", "4");
        g1.addEdge("3", "5");
        g1.addEdge("4", "6");

        WeisfeilerLehmanLabeling<String, DefaultEdge> alg = new WeisfeilerLehmanLabeling<>(g1, 5);
        Map<String, String> label = alg.getLabeling().getLabels();
        assertEquals(label.get("1"), "1");
        assertEquals(label.get("2"), "1");
        assertEquals(label.get("3"), "2");
        assertEquals(label.get("4"), "2");
        assertEquals(label.get("5"), "1");
        assertEquals(label.get("6"), "1");
    }

    @Test
    public void testDirected1()
    {
        Graph<Integer,
            DefaultEdge> g = GraphTypeBuilder
                .directed().allowingMultipleEdges(true).allowingSelfLoops(true)
                .vertexSupplier(SupplierUtil.createIntegerSupplier()).edgeClass(DefaultEdge.class)
                .buildGraph();

        for (int i = 1; i < 10; i++) {
            g.addVertex(i);
        }
        g.addEdge(1, 2);
        g.addEdge(2, 3);
        g.addEdge(4, 3);
        g.addEdge(1, 4);
        g.addEdge(5, 1);
        g.addEdge(5, 4);
        g.addEdge(5, 7);
        g.addEdge(5, 6);
        g.addEdge(6, 8);
        g.addEdge(8, 9);

        WeisfeilerLehmanLabeling<Integer, DefaultEdge> alg = new WeisfeilerLehmanLabeling<>(g, 100);
        Map<Integer, String> label = alg.getLabeling().getLabels();

        assertEquals(label.get(3), "1");
        assertEquals(label.get(7), "1");
        assertEquals(label.get(9), "1");

        assertEquals(label.get(2), "2");
        assertEquals(label.get(4), "2");
        assertEquals(label.get(8), "2");

        assertEquals(label.get(6), "3");

        assertEquals(label.get(1), "4");

        assertEquals(label.get(5), "5");
    }

    @Test
    public void testRingGraph()
    {
        Graph<Integer,
            DefaultEdge> g = GraphTypeBuilder
                .undirected().allowingMultipleEdges(true).allowingSelfLoops(true)
                .vertexSupplier(SupplierUtil.createIntegerSupplier()).edgeClass(DefaultEdge.class)
                .buildGraph();

        new RingGraphGenerator<Integer, DefaultEdge>(200).generateGraph(g);

        WeisfeilerLehmanLabeling<Integer, DefaultEdge> alg = new WeisfeilerLehmanLabeling<>(g, 150);
        Map<Integer, String> label = alg.getLabeling().getLabels();

        for (Integer v : g.vertexSet()) {
            assertEquals("1", label.get(v));
        }
    }

    @Test
    public void test3x3GridGraph()
    {
        Graph<Integer,
            DefaultEdge> g = GraphTypeBuilder
                .undirected().allowingMultipleEdges(true).allowingSelfLoops(true)
                .vertexSupplier(SupplierUtil.createIntegerSupplier()).edgeClass(DefaultEdge.class)
                .buildGraph();

        new GridGraphGenerator<Integer, DefaultEdge>(3, 3).generateGraph(g);

        WeisfeilerLehmanLabeling<Integer, DefaultEdge> alg = new WeisfeilerLehmanLabeling<>(g, 10);
        Map<Integer, String> label = alg.getLabeling().getLabels();

        //@formatter:off
        // 1 - 2 - 1
        // 2 - 3 - 2
        // 1 - 2 - 1
        //@formatter:on

        assertEquals("1", label.get(0));
        assertEquals("2", label.get(1));
        assertEquals("1", label.get(2));
        assertEquals("2", label.get(3));
        assertEquals("3", label.get(4));
        assertEquals("2", label.get(5));
        assertEquals("1", label.get(6));
        assertEquals("2", label.get(7));
        assertEquals("1", label.get(8));
    }

    @Test
    public void test5x5GridGraph()
    {
        Graph<Integer,
            DefaultEdge> g = GraphTypeBuilder
                .undirected().allowingMultipleEdges(true).allowingSelfLoops(true)
                .vertexSupplier(SupplierUtil.createIntegerSupplier()).edgeClass(DefaultEdge.class)
                .buildGraph();

        new GridGraphGenerator<Integer, DefaultEdge>(5, 5).generateGraph(g);

        WeisfeilerLehmanLabeling<Integer, DefaultEdge> alg = new WeisfeilerLehmanLabeling<>(g, 10);
        Map<Integer, String> label = alg.getLabeling().getLabels();

        //@formatter:off
        // 1 - 2 - 3 - 2 - 1
        // 2 - 4 - 5 - 4 - 2
        // 3 - 5 - 6 - 5 - 3
        // 2 - 4 - 5 - 4 - 2
        // 1 - 2 - 3 - 2 - 1
        //@formatter:on

        assertEquals("1", label.get(0));
        assertEquals("2", label.get(1));
        assertEquals("3", label.get(2));
        assertEquals("2", label.get(3));
        assertEquals("1", label.get(4));
        assertEquals("2", label.get(5));
        assertEquals("4", label.get(6));
        assertEquals("5", label.get(7));
        assertEquals("4", label.get(8));
        assertEquals("2", label.get(9));
        assertEquals("3", label.get(10));
        assertEquals("5", label.get(11));
        assertEquals("6", label.get(12));
        assertEquals("5", label.get(13));
        assertEquals("3", label.get(14));
        assertEquals("2", label.get(15));
        assertEquals("4", label.get(16));
        assertEquals("5", label.get(17));
        assertEquals("4", label.get(18));
        assertEquals("2", label.get(19));
        assertEquals("1", label.get(20));
        assertEquals("2", label.get(21));
        assertEquals("3", label.get(22));
        assertEquals("2", label.get(23));
        assertEquals("1", label.get(24));
    }

    @Test
    public void testSame()
    {
        Graph<Integer,
            DefaultEdge> g = GraphTypeBuilder
                .undirected().allowingMultipleEdges(true).allowingSelfLoops(true)
                .vertexSupplier(SupplierUtil.createIntegerSupplier()).edgeClass(DefaultEdge.class)
                .buildGraph();

        new GridGraphGenerator<Integer, DefaultEdge>(5, 5).generateGraph(g);

        WeisfeilerLehmanLabeling<Integer, DefaultEdge> alg = new WeisfeilerLehmanLabeling<>(g, 10);
        Labeling<Integer> l1 = alg.getLabeling();
        Labeling<Integer> l2 = alg.getLabeling();
        Map<Integer, String> label1 = l1.getLabels();
        Map<Integer, String> label2 = l2.getLabels();

        assertFalse(label1 == label2);
        assertFalse(l1 == l2);
        assertTrue(WeisfeilerLehmanLabeling.isTheSameLabeling(l1, l2));
    }

    @Test
    public void testIterator()
    {
        Graph<Integer,
            DefaultEdge> g = GraphTypeBuilder
                .undirected().allowingMultipleEdges(true).allowingSelfLoops(true)
                .vertexSupplier(SupplierUtil.createIntegerSupplier()).edgeClass(DefaultEdge.class)
                .buildGraph();

        new GridGraphGenerator<Integer, DefaultEdge>(5, 5).generateGraph(g);

        WeisfeilerLehmanLabeling<Integer, DefaultEdge> alg = new WeisfeilerLehmanLabeling<>(g, 2);
        Iterator<Labeling<Integer>> it = alg.iterator();

        assertTrue(it.hasNext());
        assertEquals(
            "Labeling [labels={0=1, 1=2, 2=2, 3=2, 4=1, 5=2, 6=3, 7=3, 8=3, 9=2, 10=2, 11=3, 12=3, 13=3, 14=2, 15=2, 16=3, 17=3, 18=3, 19=2, 20=1, 21=2, 22=2, 23=2, 24=1}]",
            it.next().toString());
        assertTrue(it.hasNext());
        assertEquals(
            "Labeling [labels={0=1, 1=2, 2=3, 3=2, 4=1, 5=2, 6=4, 7=5, 8=4, 9=2, 10=3, 11=5, 12=6, 13=5, 14=3, 15=2, 16=4, 17=5, 18=4, 19=2, 20=1, 21=2, 22=3, 23=2, 24=1}]",
            it.next().toString());
        assertFalse(it.hasNext());

    }

}
