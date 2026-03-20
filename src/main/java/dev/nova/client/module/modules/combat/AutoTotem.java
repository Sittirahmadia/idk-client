package dev.nova.client.module.modules.combat;

import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.ReceivePacketEvent;
import dev.nova.client.event.events.TickEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import dev.nova.client.util.DamageUtil;
import dev.nova.client.util.InventoryUtil;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class AutoTotem extends Module {
    private final NumberSetting health  = register(new NumberSetting("Health",      "Switch below HP",      4,   1,20,0.5));
    private final BoolSetting   predict = register(new BoolSetting ("Predict",      "Predict crystal dmg",  true));
    private final NumberSetting buffer  = register(new NumberSetting("Buffer",      "Safety margin",        1.5, 0,10,0.5));
    private final BoolSetting   onPop   = register(new BoolSetting ("On Pop",       "Switch on pop detect", true));
    private final BoolSetting   offhand = register(new BoolSetting ("Check Offhand","Also check offhand",   true));

    private volatile boolean popFlag = false;

    public AutoTotem() { super("Auto Totem", "Keeps totem ready at all times", Category.COMBAT, -1); }

    @EventHandler
    public void onPacket(ReceivePacketEvent e) {
        if (!(e.packet instanceof EntityStatusS2CPacket pkt)) return;
        if (mc.player == null || mc.world == null) return;
        if (pkt.getStatus() == 35 && pkt.getEntity(mc.world) == mc.player) popFlag = true;
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (e.phase != TickEvent.Phase.PRE || mc.player == null || mc.world == null) return;
        float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();

        // On pop — switch immediately
        if (onPop.getValue() && popFlag) {
            popFlag = false;
            InventoryUtil.switchToItem(Items.TOTEM_OF_UNDYING);
            return;
        }

        // Below health threshold
        if (hp <= health.floatValue()) {
            InventoryUtil.switchToItem(Items.TOTEM_OF_UNDYING);
            return;
        }

        // Predict incoming crystal damage
        if (predict.getValue()) {
            Vec3d pp = mc.player.getPos();
            for (EndCrystalEntity c : mc.world.getEntitiesByClass(EndCrystalEntity.class,
                    new Box(pp.subtract(7,7,7), pp.add(7,7,7)), x -> true)) {
                float dmg = DamageUtil.crystalDamage(mc.player, c.getPos());
                if (dmg + buffer.floatValue() >= hp) {
                    InventoryUtil.switchToItem(Items.TOTEM_OF_UNDYING);
                    return;
                }
            }
        }
    }
}
