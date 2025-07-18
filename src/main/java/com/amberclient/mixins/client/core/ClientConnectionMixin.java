package com.amberclient.mixins.client.core;

import com.amberclient.events.core.EventManager;
import com.amberclient.events.network.PacketEvent;
import com.amberclient.modules.player.FakeLag;
import com.amberclient.utils.module.ModuleManager;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.OffThreadException;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {

    @Shadow
    private static <T extends PacketListener> void handlePacket(Packet<T> packet, PacketListener listener) {
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        FakeLag fakeLag = (FakeLag) ModuleManager.getInstance().findModuleByName("FakeLag");
        if (fakeLag == null || FakeLag.isBypassing() || !fakeLag.isEnabled()) {
            return;
        }

        PacketEvent.Send event = new PacketEvent.Send(packet, (ClientConnection) (Object) this);
        EventManager.getInstance().firePacketSend(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true, require = 1)
    private static void onReceivePacket(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        if (packet instanceof BundleS2CPacket bundleS2CPacket) {
            ci.cancel();

            for (Packet<?> packetInBundle : bundleS2CPacket.getPackets()) {
                try {
                    handlePacket(packetInBundle, listener);
                } catch (OffThreadException ignored) { }
            }
            return;
        }

        EventManager.getInstance().firePacketReceive(packet);
    }
}