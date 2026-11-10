package kim.biryeong.gcbserver.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

/** GCB-mod 4.0.0's fp32 channel. Only transform and duration flags (0x1f) are emitted. */
public record BulkDisplayS2CPacket(List<Entry> entries) implements CustomPacketPayload {
    public static final int MAX_ENTRIES = 128;
    public static final Type<BulkDisplayS2CPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath("gcb", "bulk_data"));
    public static final StreamCodec<FriendlyByteBuf, BulkDisplayS2CPacket> CODEC =
            CustomPacketPayload.codec(BulkDisplayS2CPacket::write, BulkDisplayS2CPacket::read);

    public BulkDisplayS2CPacket {
        entries = List.copyOf(entries);
        if (entries.isEmpty() || entries.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("Expected 1..128 display updates");
        }
    }

    @Override
    public Type<BulkDisplayS2CPacket> type() {
        return TYPE;
    }

    public record Entry(int entityId, Vector3fc translation, Quaternionfc leftRotation,
                        Vector3fc scale, Quaternionfc rightRotation, Integer duration) {
        public Entry {
            translation = translation == null ? null : new Vector3f(translation);
            leftRotation = leftRotation == null ? null : new Quaternionf(leftRotation);
            scale = scale == null ? null : new Vector3f(scale);
            rightRotation = rightRotation == null ? null : new Quaternionf(rightRotation);
            if ((translation != null && !translation.isFinite()) || (scale != null && !scale.isFinite())
                    || (leftRotation != null && !leftRotation.isFinite()) || (rightRotation != null && !rightRotation.isFinite())
                    || (duration != null && duration < 0)) {
                throw new IllegalArgumentException("Invalid display transformation");
            }
            if (translation == null && leftRotation == null && scale == null && rightRotation == null && duration == null) {
                throw new IllegalArgumentException("Empty display update");
            }
        }

        public int mask() {
            return (translation != null ? 1 : 0) | (leftRotation != null ? 2 : 0)
                    | (scale != null ? 4 : 0) | (rightRotation != null ? 8 : 0) | (duration != null ? 16 : 0);
        }
    }

    /** Returns null when batching would change vanilla metadata/interpolation semantics. */
    public static Entry fromVanilla(ClientboundSetEntityDataPacket packet) {
        Vector3fc translation = null, scale = null;
        Quaternionfc left = null, right = null;
        Integer duration = null;
        boolean startsNow = false;
        for (var value : packet.packedItems()) {
            switch (value.id()) {
                // These IDs are part of GCB 4's wire contract, checked against Minecraft in tests.
                case 8 -> {
                    if (value.serializer() != EntityDataSerializers.INT || !Integer.valueOf(0).equals(value.value())) return null;
                    startsNow = true;
                }
                case 9 -> {
                    if (value.serializer() != EntityDataSerializers.INT) return null;
                    duration = (Integer) value.value();
                }
                case 11, 12 -> {
                    if (value.serializer() != EntityDataSerializers.VECTOR3) return null;
                    if (value.id() == 11) translation = (Vector3fc) value.value();
                    else scale = (Vector3fc) value.value();
                }
                case 13, 14 -> {
                    if (value.serializer() != EntityDataSerializers.QUATERNION) return null;
                    if (value.id() == 13) left = (Quaternionfc) value.value();
                    else right = (Quaternionfc) value.value();
                }
                default -> { return null; }
            }
        }
        boolean transforms = translation != null || scale != null || left != null || right != null;
        // The receiver implicitly resets interpolation on every transform, but not duration-only updates.
        if (transforms != startsNow || (!transforms && duration == null)) return null;
        try {
            return new Entry(packet.id(), translation, left, scale, right, duration);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (var entry : entries) {
            buf.writeVarInt(entry.entityId());
            buf.writeByte(entry.mask());
            if (entry.translation() != null) writeVector(buf, entry.translation());
            if (entry.leftRotation() != null) writeQuaternion(buf, entry.leftRotation());
            if (entry.scale() != null) writeVector(buf, entry.scale());
            if (entry.rightRotation() != null) writeQuaternion(buf, entry.rightRotation());
            if (entry.duration() != null) buf.writeVarInt(entry.duration());
        }
    }

    private static BulkDisplayS2CPacket read(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 1 || count > MAX_ENTRIES) throw new IllegalArgumentException("Invalid display count");
        var entries = new ArrayList<Entry>(count);
        for (int i = 0; i < count; i++) {
            int id = buf.readVarInt();
            int mask = buf.readUnsignedByte();
            if (mask == 0 || (mask & ~31) != 0) throw new IllegalArgumentException("Unsupported display flags");
            entries.add(new Entry(id, (mask & 1) != 0 ? readVector(buf) : null,
                    (mask & 2) != 0 ? readQuaternion(buf) : null, (mask & 4) != 0 ? readVector(buf) : null,
                    (mask & 8) != 0 ? readQuaternion(buf) : null, (mask & 16) != 0 ? buf.readVarInt() : null));
        }
        return new BulkDisplayS2CPacket(entries);
    }

    private static void writeVector(FriendlyByteBuf buf, Vector3fc value) {
        buf.writeFloat(value.x()).writeFloat(value.y()).writeFloat(value.z());
    }

    private static void writeQuaternion(FriendlyByteBuf buf, Quaternionfc value) {
        buf.writeFloat(value.x()).writeFloat(value.y()).writeFloat(value.z()).writeFloat(value.w());
    }

    private static Vector3f readVector(FriendlyByteBuf buf) {
        return new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    private static Quaternionf readQuaternion(FriendlyByteBuf buf) {
        return new Quaternionf(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }
}
