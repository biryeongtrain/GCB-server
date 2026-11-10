package kim.biryeong.gcbserver.packet.s2c;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GCBParticleS2CPacketTest {
    @Test
    void stripsMinecraftNamespaceWithoutMovingTheVectorField() {
        String[] fields = packet("minecraft:end_rod", "").encode().split(":");

        assertEquals("PARTICLE", fields[0]);
        assertEquals("end_rod", fields[1]);
        assertEquals("0.0,0.0,0.0", fields[2]);
        assertEquals("sphere", fields[7]);
    }

    @Test
    void stripsMinecraftNamespaceFromBlockParticleData() {
        String[] fields = packet("minecraft:block", "minecraft:stone").encode().split(":");

        assertEquals("block", fields[1]);
        assertEquals("stone", fields[6]);
        assertEquals("sphere", fields[7]);
    }

    @Test
    void rejectsTokensThatWouldCorruptTheColonSeparatedPacket() {
        assertThrows(IllegalArgumentException.class, () -> packet("example:particle", ""));
        assertThrows(IllegalArgumentException.class, () -> packet("block", "example:block"));
    }

    @Test
    void replacesUnsafeTrailPeriodBeforeEncoding() {
        var trail = new GCBParticleS2CPacket.Trail(
                0.0,
                GCBParticleS2CPacket.ShapeOptions.DEFAULT,
                GCBParticleS2CPacket.Vec.ZERO,
                GCBParticleS2CPacket.Vec.UNIT_X
        );

        assertTrue(trail.encode().startsWith("trail:0.1:"));
    }

    private static GCBParticleS2CPacket packet(String particle, String data) {
        return new GCBParticleS2CPacket(
                particle,
                GCBParticleS2CPacket.Vec.ZERO,
                0,
                0.0d,
                false,
                data,
                new GCBParticleS2CPacket.Sphere(
                        GCBParticleS2CPacket.Vec.UNIT_Y,
                        8,
                        GCBParticleS2CPacket.ShapeOptions.DEFAULT,
                        GCBParticleS2CPacket.Vec.ZERO
                )
        );
    }
}
