module org.jgrapht.opt
{
    exports org.jgrapht.opt.graph.fastutil;
    exports org.jgrapht.opt.graph.sparse;
	exports org.jgrapht.opt.graph.webgraph;

    requires transitive org.jgrapht.core;
    requires transitive it.unimi.dsi.fastutil;
	requires transitive webgraph;
	requires transitive dsiutils;
	requires transitive webgraph.big;
	requires transitive com.google.common;
}
