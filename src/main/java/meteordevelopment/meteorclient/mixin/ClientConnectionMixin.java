/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import java.net.InetSocketAddress;
import java.util.Iterator;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import io.netty.channel.*;
import io.netty.handler.proxy.*;
import io.netty.handler.timeout.TimeoutException;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.ServerConnectEndEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AntiPacketKick;
import meteordevelopment.meteorclient.systems.proxies.*;
import net.minecraft.network.*;
import net.minecraft.network.handler.*;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {
    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;handlePacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;)V", shift = At.Shift.BEFORE), cancellable = true)
    private void onHandlePacket(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof BundleS2CPacket bundle) {
            for (Iterator<Packet<? super ClientPlayPacketListener>> it = bundle.getPackets().iterator(); it.hasNext(); ) {
                if (MeteorClient.EVENT_BUS.post(new PacketEvent.Receive(it.next(), (ClientConnection) (Object) this)).isCancelled()) it.remove();
            }
        } else if (MeteorClient.EVENT_BUS.post(new PacketEvent.Receive(packet, (ClientConnection) (Object) this)).isCancelled()) ci.cancel();
    }

    @Inject(method = "connect(Ljava/net/InetSocketAddress;ZLnet/minecraft/network/ClientConnection;)Lio/netty/channel/ChannelFuture;", at = @At("HEAD"))
    private static void onConnect(InetSocketAddress address, boolean useEpoll, ClientConnection connection, CallbackInfoReturnable<?> cir) {
        MeteorClient.EVENT_BUS.post(ServerConnectEndEvent.get(address));
    }

    @Inject(at = @At("HEAD"), method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V", cancellable = true)
    private void onSendPacketHead(Packet<?> packet, PacketCallbacks callbacks, CallbackInfo ci) {
        if (MeteorClient.EVENT_BUS.post(new PacketEvent.Send(packet, (ClientConnection) (Object) this)).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V", at = @At("TAIL"))
    private void onSendPacketTail(Packet<?> packet, @Nullable PacketCallbacks callbacks, CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(new PacketEvent.Sent(packet, (ClientConnection) (Object) this));
    }

    @Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true)
    private void exceptionCaught(ChannelHandlerContext context, Throwable throwable, CallbackInfo ci) {
        AntiPacketKick apk = Modules.get().get(AntiPacketKick.class);
        if (!(throwable instanceof TimeoutException) && !(throwable instanceof PacketEncoderException) && apk.catchExceptions()) {
            if (apk.logExceptions.get()) apk.warning("Caught exception: %s", throwable);
            ci.cancel();
        }
    }

    @Inject(method = "addHandlers", at = @At("RETURN"))
    private static void onAddHandlers(ChannelPipeline pipeline, NetworkSide side, boolean local, PacketSizeLogger packetSizeLogger, CallbackInfo ci) {
        if (side != NetworkSide.CLIENTBOUND) return;

        Proxy proxy = Proxies.get().getEnabled();
        if (proxy == null) return;

        switch (proxy.type.get()) {
            case Socks4 -> pipeline.addFirst(new Socks4ProxyHandler(new InetSocketAddress(proxy.address.get(), proxy.port.get()), proxy.username.get()));
            case Socks5 -> pipeline.addFirst(new Socks5ProxyHandler(new InetSocketAddress(proxy.address.get(), proxy.port.get()), proxy.username.get(), proxy.password.get()));
        }
    }
}
