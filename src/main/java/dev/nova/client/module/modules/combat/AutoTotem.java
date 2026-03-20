package dev.nova.client.module.modules.combat;

import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.TickEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import dev.nova.client.util.DamageUtil;
import dev.nova.client.util.InventoryUtil;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class AutoTotem extends Module {
    private final NumberSetting health  = register(new NumberSetting("Health",   "Switch below this HP",4,1,20,0.5));
    private final BoolSetting   predict = register(new BoolSetting("Predict",    "Predict crystal damage",true));
    private final NumberSetting buffer  = register(new NumberSetting("Buffer",   "Damage safety margin",1.5,0,10,0.5));

    public AutoTotem(){super("Auto Totem","Keeps totem in hotbar when about to pop",Category.COMBAT,-1);}

    @EventHandler
    public void onTick(TickEvent e) {
        if (e.phase!=TickEvent.Phase.PRE||mc.player==null||mc.world==null) return;
        float hp=mc.player.getHealth()+mc.player.getAbsorptionAmount();

        if (hp<=health.getValue()) { InventoryUtil.switchToItem(Items.TOTEM_OF_UNDYING); return; }

        if (predict.getValue()) {
            Vec3d pp=mc.player.getPos();
            for (EndCrystalEntity c:mc.world.getEntitiesByClass(EndCrystalEntity.class,
                    new Box(pp.subtract(6,6,6),pp.add(6,6,6)),x->true)) {
                float dmg=DamageUtil.crystalDamage(mc.player,c.getPos());
                if (dmg+buffer.getValue()>=hp){InventoryUtil.switchToItem(Items.TOTEM_OF_UNDYING);return;}
            }
        }
    }
}
