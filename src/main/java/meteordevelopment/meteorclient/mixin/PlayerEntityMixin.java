/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import meteordevelopment.meteorclient.D3;
import meteordevelopment.meteorclient.events.entity.DropItemsEvent;
import meteordevelopment.meteorclient.events.entity.player.ClipAtLedgeEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.*;
import meteordevelopment.meteorclient.systems.modules.player.Reach;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    @Shadow
    public abstract PlayerAbilities getAbilities();

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "clipAtLedge", at = @At("HEAD"), cancellable = true)
    protected void clipAtLedge(CallbackInfoReturnable<Boolean> info) {
        if (!getWorld().isClient) return;

        ClipAtLedgeEvent event = D3.EVENT_BUS.post(ClipAtLedgeEvent.get());
        if (event.isSet()) info.setReturnValue(event.isClip());
    }

    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void onDropItem(ItemStack stack, boolean bl, boolean bl2, CallbackInfoReturnable<ItemEntity> cir) {
        if (getWorld().isClient && !stack.isEmpty()) {
            if (D3.EVENT_BUS.post(DropItemsEvent.get(stack)).isCancelled()) cir.setReturnValue(null);
        }
    }

    @ModifyReturnValue(method = "getMovementSpeed", at = @At("RETURN"))
    private float onGetMovementSpeed(float original) {
        if (!getWorld().isClient) return original;

        float walkSpeed = getAbilities().getWalkSpeed();

        if (original < walkSpeed) {
            if (isSprinting()) return (float) (walkSpeed * 1.30000001192092896);
            else return walkSpeed;
        }

        return original;
    }

    @Inject(method = "getOffGroundSpeed", at = @At("HEAD"), cancellable = true)
    private void onGetOffGroundSpeed(CallbackInfoReturnable<Float> info) {
        if (!getWorld().isClient) return;

        float speed = Modules.get().get(Flight.class).getOffGroundSpeed();
        if (speed != -1) info.setReturnValue(speed);
    }

    @WrapWithCondition(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"))
    private boolean keepSprint$setVelocity(PlayerEntity instance, Vec3d vec3d) {
        return Modules.get().get(Sprint.class).stopSprinting();
    }

    @WrapWithCondition(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V"))
    private boolean keepSprint$setSprinting(PlayerEntity instance, boolean b) {
        return Modules.get().get(Sprint.class).stopSprinting();
    }

    @ModifyReturnValue(method = "getBlockInteractionRange", at = @At("RETURN"))
    private double modifyBlockInteractionRange(double original) {
        return Math.max(0, original + Modules.get().get(Reach.class).blockReach());
    }

    @ModifyReturnValue(method = "getEntityInteractionRange", at = @At("RETURN"))
    private double modifyEntityInteractionRange(double original) {
        return Math.max(0, original + Modules.get().get(Reach.class).entityReach());
    }
}
