/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.D3;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin {
    @Shadow @Final World world;

    @Inject(method = "setBlockState", at = @At("TAIL"))
    private void onSetBlockState(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> info) {
        if (world.isClient) D3.EVENT_BUS.post(BlockUpdateEvent.get(pos, info.getReturnValue(), state));
    }
}
