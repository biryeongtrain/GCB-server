package kim.biryeong.gcbserver.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.VirtualElement;
import kim.biryeong.gcbserver.packet.s2c.BulkDisplayS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Optional Polymer integration at the shared holder boundary, including existing subclasses. */
@Pseudo
@Mixin(targets = "eu.pb4.polymer.virtualentity.api.ElementHolder", remap = false)
public abstract class PolymerElementHolderMixin {
    @Shadow public abstract List<VirtualElement> getElements();
    @Shadow public abstract Collection<ServerGamePacketListenerImpl> getWatchingPlayers();

    @Unique private final Map<Integer, DisplayElement> gcb$displays = new HashMap<>();
    @Unique private final Map<ServerGamePacketListenerImpl, List<BulkDisplayS2CPacket.Entry>> gcb$pending = new HashMap<>();
    @Unique private boolean gcb$batching;

    @WrapMethod(method = "tick")
    private void gcb$batchTick(Operation<Void> original) {
        for (var element : getElements()) {
            if (element instanceof DisplayElement display) gcb$displays.put(display.getEntityId(), display);
        }
        gcb$batching = true;
        try {
            original.call();
        } finally {
            gcb$batching = false;
            gcb$displays.clear();
            gcb$flush();
        }
    }

    @WrapMethod(method = "sendPacket(Lnet/minecraft/network/protocol/Packet;)V")
    private void gcb$proxyPacket(Packet<? extends ClientGamePacketListener> packet, Operation<Void> original) {
        if (!gcb$tryBatch(packet, player -> true)) original.call(packet);
    }

    @WrapMethod(method = "sendPacket(Lnet/minecraft/network/protocol/Packet;Ljava/util/function/Predicate;)V")
    private void gcb$proxyFilteredPacket(Packet<? extends ClientGamePacketListener> packet,
                                       Predicate<ServerPlayer> predicate, Operation<Void> original) {
        if (!gcb$tryBatch(packet, predicate)) original.call(packet, predicate);
    }

    @Unique
    private boolean gcb$tryBatch(Packet<? extends ClientGamePacketListener> packet, Predicate<ServerPlayer> predicate) {
        var display = packet instanceof ClientboundSetEntityDataPacket data ? gcb$displays.get(data.id()) : null;
        var entry = gcb$batching && display != null && packet instanceof ClientboundSetEntityDataPacket data
                ? BulkDisplayS2CPacket.fromVanilla(data) : null;
        if (entry == null) {
            // Preserve ordering around movement, spawn/removal, or unsupported metadata.
            gcb$flush();
            return false;
        }
        for (var connection : getWatchingPlayers()) {
            var player = connection.getPlayer();
            if (!predicate.test(player) || !display.getVisibilityPredicate().test(player)) continue;
            if (!ServerPlayNetworking.canSend(player, BulkDisplayS2CPacket.TYPE)) {
                connection.send(packet);
                continue;
            }
            var entries = gcb$pending.computeIfAbsent(connection, ignored -> new ArrayList<>());
            entries.add(entry);
            if (entries.size() == BulkDisplayS2CPacket.MAX_ENTRIES) {
                ServerPlayNetworking.send(player, new BulkDisplayS2CPacket(entries));
                entries.clear();
            }
        }
        return true;
    }

    @Inject(method = {
            "startWatching(Lnet/minecraft/server/network/ServerGamePacketListenerImpl;)Z",
            "stopWatching(Lnet/minecraft/server/network/ServerGamePacketListenerImpl;)Z"
    }, at = @At("HEAD"))
    private void gcb$flushBeforeTrackingChange(ServerGamePacketListenerImpl player, CallbackInfoReturnable<Boolean> cir) {
        gcb$flush();
    }

    @Unique
    private void gcb$flush() {
        for (var entry : gcb$pending.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                ServerPlayNetworking.send(entry.getKey().getPlayer(), new BulkDisplayS2CPacket(entry.getValue()));
            }
        }
        gcb$pending.clear();
    }
}
