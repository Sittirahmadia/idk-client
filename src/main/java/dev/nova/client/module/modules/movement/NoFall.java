package dev.nova.client.module.modules.movement;

import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.TickEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public final class NoFall extends Module {
    private final NumberSetting fallDist  = register(new NumberSetting("Fall Dist", "Min fall dist",   3,   1,20,0.5));
    private final BoolSetting   packet    = register(new BoolSetting  ("Packet",    "Use packet mode",  true));

    public NoFall() { super("No Fall", "Prevents fall damage", Category.MOVEMENT, -1); }

    @EventHandler
    public void onTick(TickEvent e) {
        if (e.phase != TickEvent.Phase.PRE || mc.player == null) return;
        if (mc.player.fallDistance < fallDist.floatValue()) return;
        if (packet.getValue()) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
        } else {
            mc.player.fallDistance = 0;
        }
    }
}
