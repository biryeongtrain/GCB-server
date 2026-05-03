package kim.biryeong.gcbserver.packet.s2c;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public record PlayerVelocityS2CPacket(double x, double y, double z) implements GCBS2CPacket {
    @Override
    public void send(ServerPlayer player) {
        player.setDeltaMovement(new Vec3(x, y, z));
        GCBS2CPacket.super.send(player);
    }

    @Override
    public String encode() {
        return "VELOCITY:" + x + "," + y + "," + z;
    }
}
