package dev.nova.client.module.modules.combat;

import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.TickEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import net.minecraft.entity.player.PlayerEntity;
import java.util.HashSet;
import java.util.Set;

public final class AntiBot extends Module {
    private final BoolSetting pingCheck  = register(new BoolSetting ("Ping Check",  "Flag 0-ping players",     true));
    private final BoolSetting selfCheck  = register(new BoolSetting ("Self Check",  "Flag if not in tab list", true));
    private final BoolSetting nameCheck  = register(new BoolSetting ("Name Check",  "Flag invalid names",      false));
    private final NumberSetting maxPing  = register(new NumberSetting("Max Ping",   "Flag above this ping ms", 0,   0,10,1));

    public final Set<Integer> botIds = new HashSet<>();

    public AntiBot() { super("Anti Bot", "Detects and flags bot entities", Category.COMBAT, -1); }

    @EventHandler
    public void onTick(TickEvent e) {
        if (e.phase != TickEvent.Phase.PRE || mc.world == null || mc.getNetworkHandler() == null) return;
        botIds.clear();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            var entry = mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
            if (selfCheck.getValue() && entry == null) { botIds.add(p.getId()); continue; }
            if (entry == null) continue;
            if (pingCheck.getValue() && entry.getLatency() <= maxPing.intValue()) botIds.add(p.getId());
            if (nameCheck.getValue()) {
                String name = p.getName().getString();
                if (!name.matches("[A-Za-z0-9_]{3,16}")) botIds.add(p.getId());
            }
        }
    }
    public boolean isBot(PlayerEntity p) { return botIds.contains(p.getId()); }
}
