/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import com.llamalad7.mixinextras.injector.*;

import meteordevelopment.meteorclient.D3;
import meteordevelopment.meteorclient.events.entity.player.ItemUseCrosshairTargetEvent;
import meteordevelopment.meteorclient.events.game.*;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.mixininterface.IMinecraftClient;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.*;
import meteordevelopment.starscript.Script;
import net.minecraft.client.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.*;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.profiler.Profilers;

@Mixin(value = MinecraftClient.class, priority = 1001)
public abstract class MinecraftClientMixin implements IMinecraftClient {
    @Unique private boolean doItemUseCalled;
    @Unique private boolean rightClick;
    @Unique private long lastTime;
    @Unique private boolean firstFrame;

    @Shadow public ClientWorld world;
    @Shadow @Final public Mouse mouse;
    @Shadow @Final private Window window;
    @Shadow public Screen currentScreen;
    @Shadow @Final public GameOptions options;

    @Shadow protected abstract void doItemUse();

    @Shadow
    @Nullable
    public ClientPlayerInteractionManager interactionManager;

    @Shadow
    private int itemUseCooldown;

    @Shadow
    @Nullable
    public ClientPlayerEntity player;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        D3.INSTANCE.onInitializeClient();
        firstFrame = true;
    }

    @Inject(at = @At("HEAD"), method = "tick")
    private void onPreTick(CallbackInfo info) {

        doItemUseCalled = false;

        Profilers.get().push(D3.MOD_ID + "_pre_update");
        D3.EVENT_BUS.post(TickEvent.Pre.get());
        Profilers.get().pop();

        if (rightClick && !doItemUseCalled && interactionManager != null) doItemUse();
        rightClick = false;
    }

    @Inject(at = @At("TAIL"), method = "tick")
    private void onTick(CallbackInfo info) {
        Profilers.get().push(D3.MOD_ID + "_post_update");
        D3.EVENT_BUS.post(TickEvent.Post.get());
        Profilers.get().pop();
    }

    @Inject(method = "doAttack", at = @At("HEAD"))
    private void onAttack(CallbackInfoReturnable<Boolean> cir) {
        CPSUtils.onAttack();
    }

    @Inject(method = "doItemUse", at = @At("HEAD"))
    private void onDoItemUse(CallbackInfo info) {
        doItemUseCalled = true;
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;Z)V", at = @At("HEAD"))
    private void onDisconnect(Screen screen, boolean transferring, CallbackInfo info) {
        if (world != null) {
            D3.EVENT_BUS.post(GameLeftEvent.get());
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo info) {
        if (screen instanceof WidgetScreen) screen.mouseMoved(mouse.getX() * window.getScaleFactor(), mouse.getY() * window.getScaleFactor());

        OpenScreenEvent event = OpenScreenEvent.get(screen);
        D3.EVENT_BUS.post(event);

        if (event.isCancelled()) info.cancel();
    }

    @ModifyExpressionValue(method = "doItemUse", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;crosshairTarget:Lnet/minecraft/util/hit/HitResult;", ordinal = 1))
    private HitResult doItemUseMinecraftClientCrosshairTargetProxy(HitResult original) {
        return D3.EVENT_BUS.post(ItemUseCrosshairTargetEvent.get(original)).target;
    }

    @ModifyReturnValue(method = "reloadResources(ZLnet/minecraft/client/MinecraftClient$LoadingContext;)Ljava/util/concurrent/CompletableFuture;", at = @At("RETURN"))
    private CompletableFuture<Void> onReloadResourcesNewCompletableFuture(CompletableFuture<Void> original) {
        return original.thenRun(() -> D3.EVENT_BUS.post(ResourcePacksReloadedEvent.get()));
    }

    @ModifyArg(method = "updateWindowTitle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;setTitle(Ljava/lang/String;)V"))
    private String setTitle(String original) {
        if (Config.get() == null || !Config.get().customWindowTitle.get()) return original;

        String customTitle = Config.get().customWindowTitleText.get();
        Script script = MeteorStarscript.compile(customTitle);

        if (script != null) {
            String title = MeteorStarscript.run(script);
            if (title != null) customTitle = title;
        }

        return customTitle;
    }

    @Inject(method = "onResolutionChanged", at = @At("TAIL"))
    private void onResolutionChanged(CallbackInfo info) {
        D3.EVENT_BUS.post(ResolutionChangedEvent.get());
    }

    // Time delta

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(CallbackInfo info) {
        long time = System.currentTimeMillis();

        if (firstFrame) {
            lastTime = time;
            firstFrame = false;
        }

        Utils.frameTime = (time - lastTime) / 1000.0;
        lastTime = time;
    }

    // Interface

    @Override
    public void meteor$rightClick() {
        rightClick = true;
    }
}
