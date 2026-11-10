package kim.biryeong.gcbserver.packet;

import io.netty.buffer.Unpooled;
import kim.biryeong.gcbserver.packet.s2c.PlayerVelocityS2CPacket;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GCBPacketTest {
    @Test
    void vanilla26_3MotionCodecPreservesSpeedsAboveTheOldClamp() {
        var buffer = Unpooled.buffer();
        try {
            ClientboundSetEntityMotionPacket.STREAM_CODEC.encode(buffer,
                    new ClientboundSetEntityMotionPacket(42, new Vec3(20, -30, 40)));
            var motion = ClientboundSetEntityMotionPacket.STREAM_CODEC.decode(buffer);
            assertEquals(42, motion.id());
            // Vanilla's LpVec3 codec is lossy; verify high speeds survive, not exact doubles.
            assertEquals(20, motion.movement().x, 0.01);
            assertEquals(-30, motion.movement().y, 0.01);
            assertEquals(40, motion.movement().z, 0.01);
        } finally {
            buffer.release();
        }
    }

    @Test
    void preservesChannelAndUtfWireFormatOn26_3() {
        assertEquals("minecraft:gcb", GCBPacket.TYPE.id().toString());
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            GCBPacket.CODEC.encode(buffer, new PlayerVelocityS2CPacket(1, 2, 3));
            assertEquals("VELOCITY:1.0,2.0,3.0", buffer.readUtf());
            assertEquals(0, buffer.readableBytes());

            for (String message : new String[]{"1.0.1", "MODS:example", "KEYINPUT:key.attack:true", "PERSPECTIVE:THIRD_PERSON_BACK"}) {
                buffer.clear();
                buffer.writeUtf(message);
                assertEquals(message, GCBPacket.CODEC.decode(buffer).encode());
                assertEquals(0, buffer.readableBytes());
            }
        } finally {
            buffer.release();
        }
    }
}
