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

/**
 * PopCounter
 * Listens for EntityStatus 35 (totem pop) on all players and tracks / announces pops.
 */
public final class PopCounter extends Module {

    private final BoolSetting chat       = register(new BoolSetting ("Chat Announce","Send pop notification in chat",true));
    private final BoolSetting selfPops   = register(new BoolSetting ("Track Self",   "Also track own pops",           false));
    private final BoolSetting resetOnDeath=register(new BoolSetting ("Reset On Death","Reset counts when player dies", true));

    /** player UUID string → pop count */
    private final Map<String, Integer> popCounts = new HashMap<>();

    public PopCounter() {
        super("Pop Counter", "Tracks totem pops for all players", Category.COMBAT, -1);
    }

    @EventHandler
    public void onPacket(ReceivePacketEvent event) {
        if (mc.world == null || mc.player == null) return;
        if (!(event.packet instanceof EntityStatusS2CPacket pkt)) return;
        if (pkt.getStatus() != 35) return;

        var entity = pkt.getEntity(mc.world);
        if (!(entity instanceof PlayerEntity player)) return;
        if (player == mc.player && !selfPops.getValue()) return;

        String uuid  = player.getUuidAsString();
        String name  = player.getEntityName();
        int count    = popCounts.merge(uuid, 1, Integer::sum);

        if (chat.getValue() && mc.inGameHud != null) {
            String msg = "§c[Nova] §f" + name + " §7popped! §c(" + count + "x)";
            mc.inGameHud.getChatHud().addMessage(Text.literal(msg));
        }
    }

    public int getPopCount(PlayerEntity player) {
        return popCounts.getOrDefault(player.getUuidAsString(), 0);
    }

    public void reset() { popCounts.clear(); }
}
