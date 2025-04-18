/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.minecraft.block.*;
import net.minecraft.item.ItemConvertible;
import net.minecraft.util.math.Direction;

@Mixin(Block.class)
public abstract class BlockMixin extends AbstractBlock implements ItemConvertible {
    public BlockMixin(Settings settings) {
        super(settings);
    }

    @ModifyReturnValue(method = "shouldDrawSide", at = @At("RETURN"))
    private static boolean onShouldDrawSide(boolean original, BlockState state, BlockState otherState, Direction side) {
        Xray xray = Modules.get().get(Xray.class);

        if (xray.isActive()) {
            return xray.modifyDrawSide(state, otherState, side, original);
        }

        return original;
    }
}
