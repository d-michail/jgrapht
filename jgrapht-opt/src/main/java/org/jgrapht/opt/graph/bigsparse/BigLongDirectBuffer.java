/*
 * (C) Copyright 2020-2020, by Dimitrios Michail and Contributors.
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
package org.jgrapht.opt.graph.bigsparse;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;

/**
 * Implementation of a 64-bit array, outside the Java heap.
 * 
 * @author Dimitrios Michail
 */
public class BigLongDirectBuffer
{
    private final static int SEGMENT_SIZE = 0x8000000; // 1 GB

    private ByteBuffer[] buffers;
    private LongBuffer[] buffersViews;
    private long capacity;

    public BigLongDirectBuffer(long capacity)
    {
        int bufArraySize =
            (int) (capacity / SEGMENT_SIZE) + ((capacity % SEGMENT_SIZE != 0) ? 1 : 0);
        buffers = new ByteBuffer[bufArraySize];
        buffersViews = new LongBuffer[bufArraySize];
        int bufIdx = 0;
        for (long offset = 0; offset < capacity; offset += SEGMENT_SIZE) {
            long remainingFileSize = capacity - offset;
            int thisSegmentSize = SEGMENT_SIZE;
            if (remainingFileSize < SEGMENT_SIZE) {
                thisSegmentSize = (int) remainingFileSize;
            }
            ByteBuffer buffer = ByteBuffer.allocateDirect(thisSegmentSize);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffers[bufIdx] = buffer;
            buffersViews[bufIdx] = buffer.asLongBuffer();
            bufIdx++;
        }
        this.capacity = capacity;
    }

    public long capacity()
    {
        return this.capacity;
    }

    public long get(long index)
    {
        return buffersViews[(int) (index / SEGMENT_SIZE)].get((int) (index % SEGMENT_SIZE));
    }

    public void put(long index, long value)
    {
        buffersViews[(int) (index / SEGMENT_SIZE)].put((int) (index % SEGMENT_SIZE), value);
    }

}
