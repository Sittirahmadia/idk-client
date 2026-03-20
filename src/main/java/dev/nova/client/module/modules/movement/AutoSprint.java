package dev.nova.client.module.modules.movement;

import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.TickEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;

public final class AutoSprint extends Module {
    private final BoolSetting omni     = register(new BoolSetting("Omni-Sprint",  "Sprint in all directions", true));
    private final BoolSetting inAir    = register(new BoolSetting("In Air",       "Sprint while airborne",    true));
    private final BoolSetting ignoreFwd= register(new BoolSetting("Ignore Fwd",   "Sprint even not pressing W",false));

    public AutoSprint() { super("Auto Sprint", "Always sprint", Category.MOVEMENT, -1); }

    @EventHandler
    public void onTick(TickEvent e) {
        if (e.phase != TickEvent.Phase.PRE || mc.player == null) return;
        if (!inAir.getValue() && !mc.player.isOnGround()) return;
        boolean moving = mc.player.forwardSpeed != 0 || (omni.getValue() && mc.player.sidewaysSpeed != 0);
        if (moving || ignoreFwd.getValue()) mc.player.setSprinting(true);
    }
}
