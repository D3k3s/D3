/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Scaffold;
import net.minecraft.entity.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends LivingEntity {
    protected ServerPlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    public void dontJump(CallbackInfo ci) {
        if (!getWorld().isClient) return;

        if (Modules.get().get(Scaffold.class).towering()) ci.cancel();
    }
}
