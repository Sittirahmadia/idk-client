package dev.nova.client.module.modules.misc;

import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.ReceivePacketEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;

public final class AntiCrash extends Module {
    public AntiCrash() { super("Anti Crash", "Blocks crash-exploit packets", Category.MISC, -1); }

    @EventHandler
    public void onPacket(ReceivePacketEvent e) {
        if (e.packet instanceof ExplosionS2CPacket pkt) {
            // Block abnormally large or NaN explosion positions
            double x = pkt.getX(), y = pkt.getY(), z = pkt.getZ();
            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)
                    || Double.isInfinite(x) || Double.isInfinite(y) || Double.isInfinite(z)
                    || Math.abs(x) > 3e7 || Math.abs(z) > 3e7) {
                e.cancel();
            }
        }
    }
}
