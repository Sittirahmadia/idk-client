package dev.nova.client.module.modules.movement;

import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.ReceivePacketEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;

public final class Velocity extends Module {
    private final NumberSetting horizontal = register(new NumberSetting("Horizontal", "Horizontal mult %",  0,   0,150,1));
    private final NumberSetting vertical   = register(new NumberSetting("Vertical",   "Vertical mult %",    100, 0,200,1));
    private final BoolSetting   explosion  = register(new BoolSetting  ("Explosion",  "Cancel explosion kb",true));

    public Velocity() { super("Velocity", "Modifies knockback", Category.MOVEMENT, -1); }

    @EventHandler
    public void onPacket(ReceivePacketEvent e) {
        if (mc.player == null) return;
        if (e.packet instanceof EntityVelocityUpdateS2CPacket p && p.getEntityId() == mc.player.getId()) {
            e.cancel();
            double h = horizontal.getValue() / 100.0;
            double v = vertical.getValue()   / 100.0;
            mc.player.setVelocity(
                p.getVelocityX() / 8000.0 * h,
                p.getVelocityY() / 8000.0 * v,
                p.getVelocityZ() / 8000.0 * h
            );
        }
        if (explosion.getValue() && e.packet instanceof ExplosionS2CPacket) {
            e.cancel();
        }
    }
}
