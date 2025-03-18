/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import static meteordevelopment.meteorclient.D3.mc;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.world.World;

@Mixin(TridentItem.class)
public abstract class TridentItemMixin {
    @Inject(method = "onStoppedUsing", at = @At("HEAD"))
    private void onStoppedUsingHead(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> info) {
        if (user == mc.player) Utils.isReleasingTrident = true;
    }

    @Inject(method = "onStoppedUsing", at = @At("TAIL"))
    private void onStoppedUsingTail(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> info) {
        if (user == mc.player) Utils.isReleasingTrident = false;
    }
}
