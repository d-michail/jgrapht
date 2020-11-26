module org.jgrapht.webgraph
{
    exports org.jgrapht.graph.webgraph;

	requires transitive org.jgrapht.core;
	requires transitive it.unimi.dsi.dsiutils;
    requires transitive it.unimi.dsi.fastutil;
    requires transitive it.unimi.dsi.webgraph;
    requires transitive it.unimi.dsi.big.webgraph;
}
