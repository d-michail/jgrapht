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
package org.jgrapht.alg.vertexcover;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import org.jgrapht.Graph;
import org.jgrapht.SlowTests;
import org.jgrapht.alg.interfaces.FeedbackVertexSetAlgorithm.FeedbackVertexSet;
import org.jgrapht.generate.BarabasiAlbertGraphGenerator;
import org.jgrapht.generate.GnpRandomGraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jgrapht.util.SupplierUtil;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests
 * 
 * @author Dimitrios Michail
 */
public class GreedyFeedbackVertexSetAlgorithmTest
{
    /**
     * Small graph of 4 nodes.
     */
    @Test
    public void testSmallGraph()
    {
        Graph<String, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);

        graph.addVertex("A");
        graph.addVertex("B");
        graph.addVertex("C");
        graph.addVertex("D");

        graph.addEdge("A", "B");
        graph.addEdge("B", "C");
        graph.addEdge("C", "D");
        graph.addEdge("D", "A");

        GreedyUndirectedFeedbackVertexSetAlgorithm<String, DefaultEdge> alg =
            new GreedyUndirectedFeedbackVertexSetAlgorithm<>(graph);

        FeedbackVertexSet<String> fvs = alg.getFeedbackVertexSet();

        assertEquals(fvs.getWeight(), 1d, 1e-9);
        assertTrue(fvs.size() == 1);
        assertTrue(fvs.contains("A"));

        assertTrue(new FeedbackVertexSetVerifier<>(graph, fvs).verify());
    }

    /**
     * Small graph of 11 nodes.
     */
    @Test
    public void testSmallGraph2()
    {
        Graph<String, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);

        IntStream.range(0, 11).forEach(i -> {
            graph.addVertex(Integer.toString(i));
        });
        graph.addEdge("0", "1");
        graph.addEdge("0", "5");
        graph.addEdge("1", "2");
        graph.addEdge("2", "7");
        graph.addEdge("2", "3");
        graph.addEdge("3", "7");
        graph.addEdge("3", "4");
        graph.addEdge("4", "5");
        graph.addEdge("5", "6");
        graph.addEdge("6", "8");
        graph.addEdge("7", "9");
        graph.addEdge("8", "10");
        graph.addEdge("9", "10");

        GreedyUndirectedFeedbackVertexSetAlgorithm<String, DefaultEdge> alg =
            new GreedyUndirectedFeedbackVertexSetAlgorithm<>(graph);

        FeedbackVertexSet<String> fvs = alg.getFeedbackVertexSet();

        assertEquals(fvs.getWeight(), 2d, 1e-9);
        assertTrue(fvs.contains("2"));
        assertTrue(fvs.contains("3"));
        assertTrue(fvs.size() == 2);

        assertTrue(new FeedbackVertexSetVerifier<>(graph, fvs).verify());
    }

    /**
     * Small graph of 11 nodes.
     */
    @Test
    public void testSmallGraphWeighted()
    {
        Graph<String, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);

        IntStream.range(0, 11).forEach(i -> {
            graph.addVertex(Integer.toString(i));
        });
        graph.addEdge("0", "1");
        graph.addEdge("0", "5");
        graph.addEdge("1", "2");
        graph.addEdge("2", "7");
        graph.addEdge("2", "3");
        graph.addEdge("3", "7");
        graph.addEdge("3", "4");
        graph.addEdge("4", "5");
        graph.addEdge("5", "6");
        graph.addEdge("6", "8");
        graph.addEdge("7", "9");
        graph.addEdge("8", "10");
        graph.addEdge("9", "10");

        Map<String, Double> weights = new HashMap<>();
        weights.put("6", 10d);
        weights.put("4", 10d);

        GreedyUndirectedFeedbackVertexSetAlgorithm<String, DefaultEdge> alg =
            new GreedyUndirectedFeedbackVertexSetAlgorithm<>(
                graph, x -> weights.getOrDefault(x, 1d));

        FeedbackVertexSet<String> fvs = alg.getFeedbackVertexSet();

        assertEquals(fvs.getWeight(), 2d, 1e-9);
        assertTrue(fvs.contains("2"));
        assertTrue(fvs.contains("3"));
        assertTrue(fvs.size() == 2);
        assertTrue(new FeedbackVertexSetVerifier<>(graph, fvs).verify());
    }

    /**
     * Small star shaped graph
     */
    @Test
    public void testStarShapedGraphWeighted()
    {
        Graph<String, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);

        IntStream.range(0, 12).forEach(i -> {
            graph.addVertex(Integer.toString(i));
        });
        graph.addEdge("0", "1");
        graph.addEdge("0", "3");
        graph.addEdge("0", "4");
        graph.addEdge("1", "2");
        graph.addEdge("1", "6");
        graph.addEdge("2", "3");
        graph.addEdge("2", "8");
        graph.addEdge("3", "10");
        graph.addEdge("4", "5");
        graph.addEdge("6", "7");
        graph.addEdge("8", "9");
        graph.addEdge("10", "11");

        Map<String, Double> weights = new HashMap<>();
        weights.put("0", 20d);
        weights.put("1", 15d);
        weights.put("2", 10d);
        weights.put("3", 5d);

        GreedyUndirectedFeedbackVertexSetAlgorithm<String, DefaultEdge> alg =
            new GreedyUndirectedFeedbackVertexSetAlgorithm<>(
                graph, x -> weights.getOrDefault(x, 1d));

        FeedbackVertexSet<String> fvs = alg.getFeedbackVertexSet();

        assertEquals(fvs.getWeight(), 5d, 1e-9);
        assertTrue(fvs.contains("3"));
        assertTrue(fvs.size() == 1);
        assertTrue(new FeedbackVertexSetVerifier<>(graph, fvs).verify());
    }

    /**
     * Random
     */
    @Test
    public void testBarabasiAlbertFast()
    {
        testBarabasiAlbert(2, 100);
    }

    /**
     * Random
     */
    @Test
    @Category(SlowTests.class)
    public void testBarabasiAlbertSlow()
    {
        testBarabasiAlbert(5, 5000);
    }

    /**
     * Random
     */
    @Test
    public void testGnpFast()
    {
        testGnp(2, 100, 0.1);
        testGnp(2, 100, 0.1);
    }

    /**
     * Random
     */
    @Test
    @Category(SlowTests.class)
    public void testGnpSlow()
    {
        testGnp(5, 1000, 0.1);
        testGnp(2, 100, 0.1);
    }

    private void testBarabasiAlbert(int iterations, int n)
    {
        IntStream.range(0, iterations).forEach(i -> {
            Graph<Integer,
                DefaultEdge> graph = GraphTypeBuilder
                    .undirected().allowingMultipleEdges(true).allowingSelfLoops(true)
                    .edgeSupplier(SupplierUtil.DEFAULT_EDGE_SUPPLIER)
                    .vertexSupplier(SupplierUtil.createIntegerSupplier()).buildGraph();

            BarabasiAlbertGraphGenerator<Integer, DefaultEdge> gen =
                new BarabasiAlbertGraphGenerator<>(10, 10, n);
            gen.generateGraph(graph);

            GreedyUndirectedFeedbackVertexSetAlgorithm<Integer, DefaultEdge> alg =
                new GreedyUndirectedFeedbackVertexSetAlgorithm<>(graph);

            FeedbackVertexSet<Integer> fvs = alg.getFeedbackVertexSet();
            assertTrue(new FeedbackVertexSetVerifier<>(graph, fvs).verify());
        });
    }

    private void testGnp(int iterations, int n, double prob)
    {
        IntStream.range(0, iterations).forEach(i -> {
            Graph<Integer, DefaultEdge> graph;
            graph = GraphTypeBuilder
                .undirected().allowingMultipleEdges(true).allowingSelfLoops(true)
                .edgeSupplier(SupplierUtil.DEFAULT_EDGE_SUPPLIER)
                .vertexSupplier(SupplierUtil.createIntegerSupplier()).buildGraph();

            GnpRandomGraphGenerator<Integer, DefaultEdge> gen =
                new GnpRandomGraphGenerator<>(n, prob);
            gen.generateGraph(graph);

            GreedyUndirectedFeedbackVertexSetAlgorithm<Integer, DefaultEdge> alg =
                new GreedyUndirectedFeedbackVertexSetAlgorithm<>(graph);

            FeedbackVertexSet<Integer> fvs = alg.getFeedbackVertexSet();
            assertTrue(new FeedbackVertexSetVerifier<>(graph, fvs).verify());
        });
    }

}
