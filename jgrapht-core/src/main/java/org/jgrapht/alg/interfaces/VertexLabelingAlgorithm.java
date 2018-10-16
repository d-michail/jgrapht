/*
 * (C) Copyright 2017-2020, by Dimitrios Michail and Contributors.
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

import java.io.*;
import java.util.*;

/**
 * An algorithm which computes a graph vertex labeling.
 *
 * @param <V> the graph vertex type
 */
public interface VertexLabelingAlgorithm<V>
{

    /**
     * Computes a vertex labeling.
     *
     * @return a vertex labeling
     */
    Labeling<V> getLabeling();

    /**
     * A labeling.
     *
     * @param <V> the graph vertex type
     */
    interface Labeling<V>
    {
        /**
         * Get the label map.
         * 
         * @return the label map
         */
        Map<V, String> getLabels();

    }

    /**
     * Default implementation of the labeling interface.
     *
     * @param <V> the graph vertex type
     */
    class LabelingImpl<V>
        implements
        Labeling<V>,
        Serializable
    {
        private static final long serialVersionUID = -37185501038549415L;

        private final Map<V, String> labels;

        /**
         * Construct a new vertex labeling.
         *
         * @param labels the label map
         */
        public LabelingImpl(Map<V, String> labels)
        {
            this.labels = labels;
        }

        @Override
        public Map<V, String> getLabels()
        {
            return labels;
        }

        @Override
        public String toString()
        {
            return "Labeling [labels=" + labels + "]";
        }
    }

}
