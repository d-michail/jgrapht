package org.jgrapht.alg.clustering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.interfaces.ClusteringAlgorithm;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jgrapht.util.SupplierUtil;

/**
 * Overlapping Community Detection by Collective Friendship Inference.
 * 
 * @author Dimitrios Michail
 */
public class CollectiveFriendshipGroupInference<V, E> implements ClusteringAlgorithm<V> {

	private Graph<V, E> graph;

	public CollectiveFriendshipGroupInference(Graph<V, E> graph) {
		this.graph = Objects.requireNonNull(graph);
	}

	@Override
	public Clustering<V> getClustering() {
		// create all friend groups
		List<Set<V>> groups = buildFriendshipGroups();

		// remove proper subsets
		int n = groups.size();
		boolean[] keep = new boolean[n];
		Arrays.fill(keep, true);
		for (int i = 0; i < n; i++) {
			if (keep[i] == false) {
				continue;
			}
			Set<V> cur = groups.get(i);
			for (int j = 0; j < n; j++) {
				if (i != j && keep[j]) {
					Set<V> other = groups.get(j);
					if (cur.size() <= other.size() && isProperSubset(cur, other)) {
						keep[i] = false;
						break;
					}
				}
			}
		}

		List<Set<V>> curGroups = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			if (keep[i]) {
				curGroups.add(groups.get(i));
			}
		}

		// merge close by
		boolean mergeOccured = false;
		do {
			n = curGroups.size();
			keep = new boolean[n];
			Arrays.fill(keep, true);

			for (int i = 0; i < n; i++) {
				if (keep[i] == false) {
					continue;
				}
				Set<V> cur = curGroups.get(i);
				for (int j = 0; j < n; j++) {
					if (i != j && keep[j]) {
						Set<V> other = curGroups.get(j);
						if (areCloseBy(cur, other)) {
							// merge
							cur.addAll(other);
							keep[j] = false;
							mergeOccured = true;
						}
					}
				}
			}

			List<Set<V>> newGroups = new ArrayList<>();
			for (int i = 0; i < n; i++) {
				if (keep[i]) {
					newGroups.add(curGroups.get(i));
				}
			}
			curGroups = newGroups;
		} while (mergeOccured);

		return new ClusteringImpl<>(curGroups);
	}

	private List<Set<V>> buildFriendshipGroups() {
		List<Set<V>> fgroups = new ArrayList<>();

		for (V n : graph.vertexSet()) {
			Graph<V, DefaultEdge> gf = GraphTypeBuilder.forGraphType(graph.getType())
					.edgeSupplier(SupplierUtil.createDefaultEdgeSupplier()).vertexSupplier(graph.getVertexSupplier())
					.buildGraph();

			Set<V> friends = new LinkedHashSet<>();
			for (E e : graph.edgesOf(n)) {
				V v = Graphs.getOppositeVertex(graph, e, n);
				if (friends.add(v)) {
					gf.addVertex(v);
				}
			}

			for (V v : friends) {
				for (E e : graph.outgoingEdgesOf(v)) {
					V u = Graphs.getOppositeVertex(graph, e, v);
					if (friends.contains(u)) {
						gf.addEdge(v, u);
					}
				}
			}

			for (Set<V> fgroup : new ConnectivityInspector<>(gf).connectedSets()) {
				fgroup.add(n);
				fgroups.add(fgroup);
			}
		}
		return fgroups;
	}

	private boolean isProperSubset(Set<V> a, Set<V> b) {
		for (V element : a) {
			if (!b.contains(element)) {
				return false;
			}
		}
		return true;
	}

	private boolean areCloseBy(Set<V> a, Set<V> b) {
		// swap to make sure a is smaller than b
		if (a.size() > b.size()) {
			Set<V> tmp = a;
			a = b;
			b = tmp;
		}

		// check if two or more element of a belong to b
		boolean atLeastOneNotFound = false;
		for (V element : a) {
			if (!b.contains(element)) {
				if (atLeastOneNotFound) {
					// just found a second
					return false;
				}
				// found the first
				atLeastOneNotFound = true;
			}
		}
		return true;
	}

}
