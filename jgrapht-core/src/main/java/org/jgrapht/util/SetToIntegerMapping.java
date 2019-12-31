/*
 * (C) Copyright 2018-2020, by Alexandru Valeanu and Contributors.
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
package org.jgrapht.util;

import java.util.*;

/**
 * Helper class for building a one-to-one mapping from a set of elements to the integer range $[0,
 * n)$ where $n$ is the number of elements in the set.
 *
 * <p>
 * This class computes the mapping only once, on instantiation. It does not support live updates.
 * </p>
 *
 * @author Alexandru Valeanu
 *
 * @param <E> the element type
 */
public class SetToIntegerMapping<E>
{
    private final Map<E, Integer> elementMap;
    private final List<E> indexList;

    /**
     * Create a new mapping from a set elements.
     *
     * @param elements the input set of elements
     * @throws NullPointerException if {@code elements} is {@code null}
     */
    public SetToIntegerMapping(Set<E> elements)
    {
        Objects.requireNonNull(elements, "the input collection of elements cannot be null");

        elementMap = new HashMap<>(elements.size());
        indexList = new ArrayList<>(elements.size());

        for (E e : elements) {
            elementMap.put(e, elementMap.size());
            indexList.add(e);
        }
    }

    /**
     * Create a new mapping from a list of elements. The input list will be used as the
     * {@code indexList} so it must not be modified.
     *
     * @param elements the input list of elements
     * @throws NullPointerException if {@code elements} is {@code null}
     * @throws IllegalArgumentException if the elements are not distinct
     */
    public SetToIntegerMapping(List<E> elements)
    {
        Objects.requireNonNull(elements, "the input collection of elements cannot be null");

        elementMap = new HashMap<>(elements.size());
        indexList = elements;

        for (int i = 0; i < elements.size(); i++) {
            E e = elements.get(i);

            if (!elementMap.containsKey(e)) {
                elementMap.put(e, i);
            } else {
                throw new IllegalArgumentException("elements are not distinct");
            }
        }
    }

    /**
     * Create a new mapping from a collection of elements.
     *
     * @param elements the input collection of elements
     * @throws NullPointerException if {@code elements} is {@code null}
     * @throws IllegalArgumentException if the elements are not distinct
     */
    public SetToIntegerMapping(Collection<E> elements)
    {
        Objects.requireNonNull(elements, "the input collection of elements cannot be null");

        elementMap = new HashMap<>(elements.size());
        indexList = new ArrayList<>(elements.size());

        for (E e : elements) {
            if (!elementMap.containsKey(e)) {
                elementMap.put(e, elementMap.size());
                indexList.add(e);
            } else {
                throw new IllegalArgumentException("elements are not distinct");
            }
        }
    }

    /**
     * Get the {@code elementMap}, a mapping from elements to integers (i.e. the inverse of
     * {@code indexList}).
     *
     * @return a mapping from elements to integers
     */
    public Map<E, Integer> getElementMap()
    {
        return Collections.unmodifiableMap(elementMap);
    }

    /**
     * Get the {@code indexList}, a mapping from integers to elements (i.e. the inverse of
     * {@code map}).
     *
     * @return a mapping from integers to elements
     */
    public List<E> getIndexList()
    {
        return Collections.unmodifiableList(indexList);
    }
}
