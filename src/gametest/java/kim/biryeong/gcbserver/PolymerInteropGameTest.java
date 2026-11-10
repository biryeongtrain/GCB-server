package kim.biryeong.gcbserver;

import com.mojang.authlib.GameProfile;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ManualAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import io.netty.channel.ChannelFutureListener;
import kim.biryeong.gcbserver.packet.s2c.BulkDisplayS2CPacket;
import kim.biryeong.gcbserver.packet.s2c.PlayerVelocityS2CPacket;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.fabricmc.fabric.impl.networking.server.ServerNetworkingImpl;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PolymerInteropGameTest {
    @GameTest
    public void batchesOnlyForCapableVisibleViewers(GameTestHelper helper) {
        verifyInterop(helper, new ElementHolder());
    }

    @GameTest
    public void existingHolderSubclassGetsBatchingWithoutChangingItsParent(GameTestHelper helper) {
        verifyInterop(helper, new ElementHolder() {
            @Override
            public void tick() {
                super.tick();
            }
        });
    }

    private static void verifyInterop(GameTestHelper helper, ElementHolder holder) {
        var capablePackets = new RecordingConnection();
        var vanillaPackets = new RecordingConnection();
        var capable = player(helper, capablePackets, "gcb_test");
        var vanilla = player(helper, vanillaPackets, "vanilla_test");
        var addon = ServerNetworkingImpl.getAddon(capable.connection);
        addon.handle(new RegistrationPayload(RegistrationPayload.REGISTER, List.of(BulkDisplayS2CPacket.TYPE.id())));
        helper.assertTrue(ServerPlayNetworking.canSend(capable, BulkDisplayS2CPacket.TYPE), "Channel registration must enable bulk");
        helper.assertTrue(!ServerPlayNetworking.canSend(vanilla, BulkDisplayS2CPacket.TYPE), "Vanilla must stay on metadata packets");

        holder.setAttachment(new ManualAttachment(holder, helper.getLevel(), () -> Vec3.ZERO));
        var elements = new ArrayList<ItemDisplayElement>();
        for (int i = 0; i < 3; i++) {
            var element = new ItemDisplayElement(Items.STONE);
            element.ignorePositionUpdates();
            element.setInterpolationDuration(1);
            if (i == 2) element.setVisibilityPredicate(p -> p == vanilla);
            elements.add(holder.addElement(element));
        }
        try {
            holder.startWatching(capable);
            holder.startWatching(vanilla);
            holder.tick();
            capablePackets.packets.clear();
            vanillaPackets.packets.clear();

            for (var element : elements) {
                element.setTranslation(new Vector3f(1, 2, 3));
                element.startInterpolation();
            }
            holder.tick();
            helper.assertTrue(capablePackets.packets.size() == 1, "Two visible bones must become one packet");
            var payload = (BulkDisplayS2CPacket) ((ClientboundCustomPayloadPacket) capablePackets.packets.getFirst()).payload();
            helper.assertTrue(payload.entries().size() == 2, "Hidden bone must not leak to bulk viewer");
            helper.assertTrue(payload.entries().stream().allMatch(e -> e.translation().equals(new Vector3f(1, 2, 3))), "Transforms must match");
            helper.assertTrue(vanillaPackets.packets.size() == 3
                    && vanillaPackets.packets.stream().allMatch(p -> p instanceof ClientboundSetEntityDataPacket), "Vanilla needs all original metadata");

            capablePackets.packets.clear();
            elements.getFirst().setTranslation(new Vector3f(2, 2, 3));
            elements.getFirst().setStartInterpolation(2);
            holder.tick();
            helper.assertTrue(capablePackets.packets.size() == 1 && capablePackets.packets.getFirst() instanceof ClientboundSetEntityDataPacket,
                    "Delayed interpolation must fall back even on GCB 4");

            addon.handle(new RegistrationPayload(RegistrationPayload.UNREGISTER, List.of(BulkDisplayS2CPacket.TYPE.id())));
            capablePackets.packets.clear();
            elements.getFirst().setTranslation(new Vector3f(3, 2, 3));
            elements.getFirst().setStartInterpolation(0);
            holder.tick();
            helper.assertTrue(capablePackets.packets.size() == 1 && capablePackets.packets.getFirst() instanceof ClientboundSetEntityDataPacket,
                    "Channel unregistration must restore vanilla delivery");

            capablePackets.packets.clear();
            new PlayerVelocityS2CPacket(20, -30, 40).send(capable);
            helper.assertTrue(capable.getDeltaMovement().equals(new Vec3(20, -30, 40)), "Server velocity must update");
            helper.assertTrue(capablePackets.packets.size() == 1
                    && capablePackets.packets.getFirst() instanceof ClientboundSetEntityMotionPacket motion
                    && motion.movement().equals(new Vec3(20, -30, 40)), "GCB 4 must receive high-speed vanilla motion, not removed VELOCITY payload");
        } finally {
            holder.destroy();
        }
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper, RecordingConnection connection, String name) {
        var profile = new GameProfile(UUID.randomUUID(), name);
        var server = helper.getLevel().getServer();
        var player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, CommonListenerCookie.createInitial(profile, false));
        return player;
    }

    private static class RecordingConnection extends Connection {
        final List<Packet<?>> packets = new ArrayList<>();

        RecordingConnection() {
            super(PacketFlow.SERVERBOUND);
        }

        @Override
        public void send(Packet<?> packet, ChannelFutureListener listener, boolean flush) {
            packets.add(packet);
        }
    }
}
