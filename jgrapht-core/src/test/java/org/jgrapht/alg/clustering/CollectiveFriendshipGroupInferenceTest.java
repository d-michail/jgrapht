package org.jgrapht.alg.clustering;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.ClusteringAlgorithm.Clustering;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jgrapht.util.SupplierUtil;
import org.junit.Test;

public class CollectiveFriendshipGroupInferenceTest {

	@Test
	public void test1() {
		Graph<String, DefaultEdge> g = GraphTypeBuilder.undirected().allowingMultipleEdges(true).allowingSelfLoops(true)
				.weighted(false).edgeSupplier(SupplierUtil.DEFAULT_EDGE_SUPPLIER)
				.vertexSupplier(SupplierUtil.createStringSupplier()).buildGraph();

		g.addVertex("A");
		g.addVertex("B");
		g.addVertex("C");
		g.addVertex("D");
		g.addVertex("E");
		g.addVertex("F");
		g.addVertex("G");

		g.addEdge("A", "B");
		g.addEdge("A", "C");
		g.addEdge("B", "C");
		g.addEdge("C", "D");
		g.addEdge("B", "D");
		g.addEdge("D", "F");
		g.addEdge("D", "E");
		g.addEdge("E", "F");
		g.addEdge("E", "G");
		g.addEdge("F", "G");

		CollectiveFriendshipGroupInference<String, DefaultEdge> alg = new CollectiveFriendshipGroupInference<>(g);
		Clustering<String> clustering = alg.getClustering();

		assertEquals(clustering.getNumberClusters(), 2);
		List<Set<String>> clusters = clustering.getClusters();

		assertEquals(clusters.get(0), new HashSet<>(List.of("A", "B", "C", "D")));
		assertEquals(clusters.get(1), new HashSet<>(List.of("D", "E", "F", "G")));
	}

}
