/*******************************************************************************
 * Copyright 2019 grondag
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.canvas.draw;

import java.util.ArrayDeque;
import java.util.function.Consumer;

import com.google.common.collect.ComparisonChain;

import grondag.canvas.apiimpl.RenderConditionImpl;
import grondag.canvas.apiimpl.RenderPipelineImpl;
import grondag.canvas.pipeline.PipelineManager;
import it.unimi.dsi.fastutil.Arrays;
import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.AbstractIntComparator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Accumulates and renders delegates in pipeline, buffer order.<p>
 * 
 * Note there is no translucent version of this, because translucent
 * must always be rendered in quad-sort order and thus we don't accumulate
 * multiple chunks or models into a single collection.
 */
public class SolidRenderList implements Consumer<ObjectArrayList<DrawableDelegate>> {
    private static final ArrayDeque<SolidRenderList> POOL = new ArrayDeque<>();
    
    public static SolidRenderList claim() {
        SolidRenderList result = POOL.poll();
        if (result == null)
            result = new SolidRenderList();
        return result;
    }
    
    @SuppressWarnings("serial")
    private static class BufferSorter extends AbstractIntComparator implements Swapper {
        Object[] delegates;

        @Override
        public int compare(int aIndex, int bIndex) {
            DrawableDelegate a = (DrawableDelegate) delegates[aIndex];
            DrawableDelegate b = (DrawableDelegate) delegates[bIndex];
            return ComparisonChain.start()
                    .compare(a.getPipeline().index, b.getPipeline().index)
                    .compare(a.bufferId(), b.bufferId())
                    .result();
        }

        @Override
        public void swap(int a, int b) {
            Object swap = delegates[a];
            delegates[a] = delegates[b];
            delegates[b] = swap;
        }
    };
    
    private static final ThreadLocal<BufferSorter> SORTERS = ThreadLocal.withInitial(BufferSorter::new);

    private final ObjectArrayList<DrawableDelegate> delegates = new ObjectArrayList<>();

    private SolidRenderList() {
    }

    @Override
    public void accept(ObjectArrayList<DrawableDelegate> delegatesIn) {
        final int limit = delegatesIn.size();
        for (int i = 0; i < limit; i++) {
            delegates.add(delegatesIn.get(i));
        }
    }

    /**
     * Renders delegates in buffer order to minimize bind calls. 
     * Assumes all delegates in the list share the same pipeline.
     */
    public void draw() {
        final int limit = delegates.size();

        if (limit == 0)
            return;

        final Object[] draws = delegates.elements();

        final BufferSorter sorter = SORTERS.get();
        sorter.delegates = draws;
        Arrays.quickSort(0, limit, sorter, sorter);

        ((DrawableDelegate) draws[0]).getPipeline().pipeline.activate(true);
        
        RenderPipelineImpl lastPipeline = null;
        int lastBufferId = -1;
        final int frameIndex = PipelineManager.INSTANCE.frameIndex();

        for (int i = 0; i < limit; i++) {
            final DrawableDelegate b = (DrawableDelegate) draws[i];
            final RenderConditionImpl condition = b.getPipeline().condition;
            
            if(!condition.affectBlocks || condition.compute(frameIndex)) {
                final RenderPipelineImpl thisPipeline = b.getPipeline().pipeline;
                if(thisPipeline != lastPipeline) {
                    thisPipeline.activate(true);
                    lastPipeline = thisPipeline;
                }
                lastBufferId = b.bind(lastBufferId);
                b.draw();
            }
        }
        delegates.clear();
    }
    
    public void release() {
        POOL.offer(this);
    }
    
    public void drawAndRelease() {
        draw();
        release();
    }
}