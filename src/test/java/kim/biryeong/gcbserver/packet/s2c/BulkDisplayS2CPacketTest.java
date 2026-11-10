package kim.biryeong.gcbserver.packet.s2c;

import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData.DataValue;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.Display;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BulkDisplayS2CPacketTest {
    @Test
    void matchesGcb4Fp32FieldOrderAndMetadataIds() throws ReflectiveOperationException {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        String[] fields = {"DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID", "DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID",
                "DATA_TRANSLATION_ID", "DATA_SCALE_ID", "DATA_LEFT_ROTATION_ID", "DATA_RIGHT_ROTATION_ID"};
        int[] ids = {8, 9, 11, 12, 13, 14};
        for (int i = 0; i < fields.length; i++) {
            var field = Display.class.getDeclaredField(fields[i]);
            field.setAccessible(true);
            assertEquals(ids[i], ((EntityDataAccessor<?>) field.get(null)).id());
        }
        var entry = new BulkDisplayS2CPacket.Entry(300, new Vector3f(1, 2, 3), new Quaternionf(0, 0, 0, 1),
                new Vector3f(4, 5, 6), new Quaternionf(0, 1, 0, 0), 3);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            BulkDisplayS2CPacket.CODEC.encode(buffer, new BulkDisplayS2CPacket(List.of(entry)));
            assertEquals("gcb:bulk_data", BulkDisplayS2CPacket.TYPE.id().toString());
            assertEquals(1, buffer.readVarInt());
            assertEquals(300, buffer.readVarInt());
            assertEquals(0x1f, buffer.readUnsignedByte());
            // Independent read following upstream BulkReceiver: translation, left rotation, scale, right rotation.
            for (float value : new float[]{1, 2, 3, 0, 0, 0, 1, 4, 5, 6, 0, 1, 0, 0}) {
                assertEquals(value, buffer.readFloat());
            }
            assertEquals(3, buffer.readVarInt());
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void fallsBackWheneverImplicitInterpolationResetWouldChangeSemantics() {
        var translation = new DataValue<>(11, EntityDataSerializers.VECTOR3, new Vector3f(1, 2, 3));
        var start = new DataValue<>(8, EntityDataSerializers.INT, 0);
        assertNotNull(BulkDisplayS2CPacket.fromVanilla(new ClientboundSetEntityDataPacket(1, List.of(start, translation))));
        assertNull(BulkDisplayS2CPacket.fromVanilla(new ClientboundSetEntityDataPacket(1, List.of(translation))));
        assertNull(BulkDisplayS2CPacket.fromVanilla(new ClientboundSetEntityDataPacket(1,
                List.of(new DataValue<>(8, EntityDataSerializers.INT, 2), translation))));
        assertNull(BulkDisplayS2CPacket.fromVanilla(new ClientboundSetEntityDataPacket(1, List.of(start))));
        assertNull(BulkDisplayS2CPacket.fromVanilla(new ClientboundSetEntityDataPacket(1,
                List.of(start, translation, new DataValue<>(17, EntityDataSerializers.FLOAT, 1f)))));
        assertNotNull(BulkDisplayS2CPacket.fromVanilla(new ClientboundSetEntityDataPacket(1,
                List.of(new DataValue<>(9, EntityDataSerializers.INT, 3)))));
    }

    @Test
    void rejectsInvalidPayloadsAndSnapshotsMutableTransforms() {
        assertThrows(IllegalArgumentException.class, () -> new BulkDisplayS2CPacket(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new BulkDisplayS2CPacket.Entry(1,
                new Vector3f(Float.NaN, 0, 0), null, null, null, null));
        var translation = new Vector3f(1, 2, 3);
        var entry = new BulkDisplayS2CPacket.Entry(1, translation, null, null, null, null);
        translation.zero();
        assertEquals(1f, entry.translation().x());
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeVarInt(BulkDisplayS2CPacket.MAX_ENTRIES + 1);
            assertThrows(IllegalArgumentException.class, () -> BulkDisplayS2CPacket.CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
