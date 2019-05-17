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

package grondag.canvas.mixin;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.IntFunction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.apiimpl.util.ChunkRendererRegionExt;
import grondag.canvas.chunk.ChunkHack;
import grondag.canvas.chunk.ChunkHack.PaletteCopy;
import grondag.canvas.chunk.FastRenderRegion;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

@Mixin(ChunkRendererRegion.class)
public abstract class MixinChunkRendererRegion implements ChunkRendererRegionExt {
    @Shadow protected World world;
    @Shadow protected BlockState[] blockStates;
    @Shadow protected FluidState[] fluidStates;
    @Shadow protected BlockPos offset;
    @Shadow protected int xSize;
    @Shadow protected int ySize;
    @Shadow protected int zSize;

    @Shadow
    protected abstract int getIndex(int x, int y, int z);

    private static final Iterable<BlockPos> DUMMY_ITERABLE = new Iterable<BlockPos>() {
        @Override
        public Iterator<BlockPos> iterator() {
            return Collections.emptyIterator();
        }
    };
    
    private FastRenderRegion fastRegion;
    
    //PERF: could avoid some allocation by zeroing array allocation of unused block/fluid cache arrays
    
    @Redirect(method = "<init>*", require = 1, at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/util/math/BlockPos;iterate(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;)Ljava/lang/Iterable;"))
    private Iterable<BlockPos> hookIterate(BlockPos from, BlockPos to) {
        return DUMMY_ITERABLE;
    }
    
    @SuppressWarnings("unchecked")
    @Inject(at = @At("RETURN"), method = "<init>")
    public void init(World world, int cxOff, int czOff, WorldChunk[][] chunks, BlockPos posFrom, BlockPos posTo, CallbackInfo info) {
        fastRegion = FastRenderRegion.claim().prepare(world, chunks, posFrom);
    }

    private TerrainRenderContext fabric_renderer;

    @Override
    public TerrainRenderContext canvas_renderer() {
        return fabric_renderer;
    }

    @Override
    public void canvas_renderer(TerrainRenderContext renderer) {
        fabric_renderer = renderer;
    }

    //TODO: remove if not used
    @Override
    public void canvas_prepare() {
//        sections = claimSectionArray();
//
//        for(int x = 0; x < 3; x++) {
//            for(int z = 0; z < 3; z++) {
//                for(int y = 0; y < 3; y++) {
//                    final int i = x + y * 3 + z * 9;
//                    ChunkHack.captureSection(sections[i], copies[i]);
//                }
//            }
//        }
    }
    
  //UGLY: remove or rework
    @Override
    public BlockView canvas_worldHack() {
        return world;
    }

    @Overwrite
    public BlockState getBlockState(BlockPos pos) {
        return fastRegion.getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    @Overwrite
    public FluidState getFluidState(BlockPos pos) {
        return fastRegion.getBlockState(pos.getX(), pos.getY(), pos.getZ()).getFluidState();
    }
    
    @Override
    public void canvas_release() {
        if(fastRegion != null) {
            fastRegion.release();
            fastRegion = null;
        }
    }
}
