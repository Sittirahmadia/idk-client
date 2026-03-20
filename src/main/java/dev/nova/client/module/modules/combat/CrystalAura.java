package dev.nova.client.module.modules.combat;

import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.TickEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import dev.nova.client.util.DamageUtil;
import dev.nova.client.util.InventoryUtil;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.Comparator;
import java.util.Random;

public final class CrystalAura extends Module {
    private final NumberSetting range    = register(new NumberSetting("Range",      "Break range",       4.5, 1, 6, 0.1));
    private final NumberSetting delay    = register(new NumberSetting("Delay",      "Ticks between hits",1,   0,20, 1));
    private final NumberSetting minDmg   = register(new NumberSetting("Min Damage", "Min damage to break",2, 0,36, 0.5));
    private final BoolSetting   antiSui  = register(new BoolSetting("Anti Suicide", "Skip if self-lethal",true));
    private final BoolSetting   rotate   = register(new BoolSetting("Rotate",       "Face crystal",      true));
    private int clock = 0;
    private final Random rng = new Random();

    public CrystalAura() { super("Crystal Aura","Breaks nearby crystals dealing damage to enemies",Category.COMBAT,-1); }

    @EventHandler
    public void onTick(TickEvent e) {
        if (e.phase != TickEvent.Phase.PRE || mc.player == null || mc.world == null) return;
        if (clock++ < delay.intValue()) return;
        clock = 0;
        float selfHp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        PlayerEntity target = mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && p.isAlive())
                .min(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(p))).orElse(null);
        Vec3d eye = mc.player.getEyePos();
        double r = range.getValue();
        EndCrystalEntity best = null; float bestDmg = -1;
        for (EndCrystalEntity c : mc.world.getEntitiesByClass(EndCrystalEntity.class,
                new Box(eye.subtract(r,r,r), eye.add(r,r,r)), en -> mc.player.distanceTo(en) <= r)) {
            if (antiSui.getValue() && DamageUtil.crystalDamage(mc.player, c.getPos()) >= selfHp) continue;
            float dmg = target != null ? DamageUtil.crystalDamage(target, c.getPos()) : 1f;
            if (dmg < minDmg.getValue()) continue;
            if (dmg > bestDmg) { bestDmg = dmg; best = c; }
        }
        if (best == null) return;
        if (rotate.getValue()) {
            Vec3d p = best.getPos();
            double dx = p.x - eye.x, dy = p.y + 0.5 - eye.y, dz = p.z - eye.z;
            mc.player.setYaw((float)(Math.toDegrees(Math.atan2(dz,dx))-90+rng.nextGaussian()));
            mc.player.setPitch((float)(-Math.toDegrees(Math.atan2(dy,Math.sqrt(dx*dx+dz*dz)))+rng.nextGaussian()));
        }
        mc.interactionManager.attackEntity(mc.player, best);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
