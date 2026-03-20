package dev.nova.client.module.modules.combat;

import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.ReceivePacketEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.text.Text;
import java.util.HashMap;
import java.util.Map;

public final class PopCounter extends Module {
    private final BoolSetting chat      = register(new BoolSetting ("Chat",       "Announce pops in chat",   true));
    private final BoolSetting selfPops  = register(new BoolSetting ("Self Pops",  "Count own pops too",      false));
    private final BoolSetting totalMsg  = register(new BoolSetting ("Total",      "Show total pop count",     true));

    private final Map<String, Integer> popCounts = new HashMap<>();

    public PopCounter() { super("Pop Counter", "Tracks totem pops", Category.COMBAT, -1); }

    @Override public void onEnable()  { super.onEnable();  popCounts.clear(); }

    @EventHandler
    public void onPacket(ReceivePacketEvent e) {
        if (!(e.packet instanceof EntityStatusS2CPacket pkt)) return;
        if (mc.world == null || mc.player == null || pkt.getStatus() != 35) return;
        var entity = pkt.getEntity(mc.world);
        if (!(entity instanceof PlayerEntity player)) return;
        if (player == mc.player && !selfPops.getValue()) return;

        String uuid  = player.getUuidAsString();
        String name  = player.getName().getString();
        int count    = popCounts.merge(uuid, 1, Integer::sum);

        if (chat.getValue() && mc.inGameHud != null) {
            String total = totalMsg.getValue() ? " §8(§c" + count + "x total§8)" : "";
            mc.inGameHud.getChatHud().addMessage(
                Text.literal("§c[Nova] §f" + name + " §7popped!" + total));
        }
    }

    public int getPopCount(PlayerEntity p) { return popCounts.getOrDefault(p.getUuidAsString(), 0); }
    public void reset() { popCounts.clear(); }
}
