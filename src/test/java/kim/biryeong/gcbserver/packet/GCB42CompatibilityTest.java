package kim.biryeong.gcbserver.packet;

import io.netty.buffer.Unpooled;
import kim.biryeong.gcbserver.packet.c2s.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GCB42CompatibilityTest {
    private GCBC2SPacket decode(String raw) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            buffer.writeUtf(raw);
            var packet = (GCBC2SPacket) GCBPacket.CODEC.decode(buffer);
            assertEquals(0, buffer.readableBytes());
            return packet;
        } finally { buffer.release(); }
    }
    @Test void actual42JoinSequenceAndLegacyUseSameChannel() {
        assertEquals("minecraft:gcb", GCBPacket.TYPE.id().toString());
        for (String raw : new String[]{"4.2.0", "FEATURE:SKIN", "FEATURE:PERSPECTIVE",
                "MODS:gcbmod", "PERSPECTIVE:FIRST_PERSON", "KEYINPUT:key.keyboard.v:true",
                "KEYINPUT:key.keyboard.v:false", "1.0.1"}) {
            var packet = decode(raw);
            assertTrue(packet.accepted(), raw);
            assertEquals(raw, packet.encode());
        }
        assertInstanceOf(GCBFeatureC2SPacket.class, decode("FEATURE:SKIN"));
        assertEquals("SKIN", ((GCBFeatureC2SPacket) decode("FEATURE:SKIN")).feature());
    }
    @Test void unknownExtensionsAndMalformedKnownMessagesAreInert() {
        for (String raw : new String[]{"FUTURE:value", "FEATURE:", "FEATURE:SKIN:extra",
                "PERSPECTIVE:", "PERSPECTIVE:NEW_CAMERA", "PERSPECTIVE:FIRST_PERSON:extra",
                "KEYINPUT:", "KEYINPUT:key.attack:yes", "KEYINPUT:key.future:true",
                "KEYINPUT:key.attack:false:extra", "MODS:", "MODS:x:y", ""}) {
            var packet = decode(raw);
            assertFalse(packet.accepted(), raw);
            // Ignored packets must not read or mutate a player/context.
            assertDoesNotThrow(() -> packet.apply(null), raw);
        }
    }
    @Test void unknownMessageDoesNotDesynchronizeFollowingInput() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            buffer.writeUtf("FUTURE:extension");
            buffer.writeUtf("KEYINPUT:key.attack:true");
            buffer.writeUtf("KEYINPUT:key.attack:false");
            assertFalse(((GCBC2SPacket) GCBPacket.CODEC.decode(buffer)).accepted());
            assertTrue(((GCBKeyInputC2SPacket) GCBPacket.CODEC.decode(buffer)).pushed());
            assertFalse(((GCBKeyInputC2SPacket) GCBPacket.CODEC.decode(buffer)).pushed());
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }
}
