package dev.nova.client.module.modules.combat;

import dev.nova.client.NovaClient;
import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.TickEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import dev.nova.client.util.BlockUtil;
import dev.nova.client.util.DamageUtil;
import dev.nova.client.util.InventoryUtil;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

import java.util.*;

public final class AutoCrystal extends Module {

    private final NumberSetting placeDelay  = register(new NumberSetting("Place Delay",  "Ticks between placements", 2,   0, 20, 1));
    private final NumberSetting breakDelay  = register(new NumberSetting("Break Delay",  "Ticks between breaks",     1,   0, 20, 1));
    private final NumberSetting placeRange  = register(new NumberSetting("Place Range",  "Placement reach",          4.5, 1,  6, 0.1));
    private final NumberSetting breakRange  = register(new NumberSetting("Break Range",  "Break reach",              4.5, 1,  6, 0.1));
    private final NumberSetting minDamage   = register(new NumberSetting("Min Damage",   "Min enemy damage",         4,   0, 36, 0.5));
    private final NumberSetting maxSelf     = register(new NumberSetting("Max Self Dmg", "Max self damage",          8,   0, 36, 0.5));
    private final BoolSetting   antiSuicide = register(new BoolSetting ("Anti Suicide",  "Skip lethal placements",   true));
    private final BoolSetting   silentSwap  = register(new BoolSetting ("Silent Swap",   "Invisible item switch",    false));
    private final BoolSetting   rotate      = register(new BoolSetting ("Rotate",        "Server-side rotation",     true));
    private final BoolSetting   noFriends   = register(new BoolSetting ("No Friends",    "Skip friend players",      true));
    private final BoolSetting   antiPop     = register(new BoolSetting ("Anti Pop",      "Pause after enemy pops",   false));

    private int placeTick = 0, breakTick = 0;
    private BlockPos lastBase = null;
    private final Random rng = new Random();

    public AutoCrystal() {
        super("Auto Crystal", "Places and explodes end crystals automatically", Category.COMBAT, -1);
    }

    @Override public void onEnable()  { super.onEnable();  placeTick = breakTick = 0; lastBase = null; }
    @Override public void onDisable() { super.onDisable(); lastBase = null; }

    @EventHandler
    public void onTick(TickEvent e) {
        if (e.phase != TickEvent.Phase.PRE || mc.player == null || mc.world == null) return;

        boolean holding   = mc.player.getMainHandStack().isOf(Items.END_CRYSTAL);
        boolean canSilent = silentSwap.getValue() && InventoryUtil.findInHotbar(Items.END_CRYSTAL) != -1;
        if (!holding && !canSilent) return;

        PlayerEntity target = nearestEnemy();
        float selfHp = mc.player.getHealth() + mc.player.getAbsorptionAmount();

        // BREAK
        if (breakTick >= breakDelay.intValue()) {
            EndCrystalEntity best = bestCrystal(target, selfHp);
            if (best != null) {
                if (rotate.getValue()) faceTo(best.getPos().add(0, 0.5, 0));
                mc.interactionManager.attackEntity(mc.player, best);
                mc.player.swingHand(Hand.MAIN_HAND);
                lastBase = null; breakTick = 0;
            }
        } else breakTick++;

        // PLACE
        if (placeTick >= placeDelay.intValue()) {
            BlockPos base = bestPlacement(target, selfHp);
            if (base != null) {
                Vec3d face = Vec3d.ofCenter(base).add(0, 0.5, 0);
                if (rotate.getValue()) faceTo(face);
                int saved = mc.player.getInventory().selectedSlot;
                if (canSilent && !holding) InventoryUtil.switchToItem(Items.END_CRYSTAL);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                        new BlockHitResult(face, Direction.UP, base, false));
                mc.player.swingHand(Hand.MAIN_HAND);
                lastBase = base;
                if (canSilent && !holding) InventoryUtil.switchTo(saved);
                placeTick = 0;
            }
        } else placeTick++;
    }

    private BlockPos bestPlacement(PlayerEntity target, float selfHp) {
        BlockPos pp = mc.player.getBlockPos(); Vec3d eye = mc.player.getEyePos();
        double rng2 = placeRange.getValue();
        BlockPos best = null; double bestScore = -Double.MAX_VALUE;
        for (int dx=-5;dx<=5;dx++) for (int dz=-5;dz<=5;dz++) for (int dy=-2;dy<=3;dy++) {
            BlockPos base = pp.add(dx,dy,dz);
            if (!BlockUtil.canPlaceCrystal(base)) continue;
            Vec3d face = Vec3d.ofCenter(base).add(0,0.5,0);
            if (eye.distanceTo(face) > rng2) continue;
            Vec3d boom = Vec3d.ofCenter(base.up());
            float selfDmg = DamageUtil.crystalDamage(mc.player, boom);
            if (antiSuicide.getValue() && selfDmg >= selfHp) continue;
            if (selfDmg > maxSelf.getValue()) continue;
            double score;
            if (target == null) { score = -mc.player.squaredDistanceTo(face); }
            else {
                float eDmg = DamageUtil.crystalDamage(target, boom);
                if (eDmg < minDamage.getValue()) continue;
                score = eDmg * 2.0 - selfDmg;
            }
            if (score > bestScore) { bestScore = score; best = base; }
        }
        return best;
    }

    private EndCrystalEntity bestCrystal(PlayerEntity target, float selfHp) {
        Vec3d eye = mc.player.getEyePos(); double range = breakRange.getValue();
        EndCrystalEntity best = null; float bestDmg = -1;
        for (EndCrystalEntity c : mc.world.getEntitiesByClass(EndCrystalEntity.class,
                new Box(eye.subtract(range,range,range), eye.add(range,range,range)),
                en -> mc.player.distanceTo(en) <= range)) {
            if (antiSuicide.getValue() && DamageUtil.crystalDamage(mc.player, c.getPos()) >= selfHp) continue;
            float dmg = target != null ? DamageUtil.crystalDamage(target, c.getPos()) : 1f;
            if (dmg > bestDmg) { bestDmg = dmg; best = c; }
        }
        return best;
    }

    private void faceTo(Vec3d target) {
        Vec3d eye = mc.player.getEyePos();
        double dx=target.x-eye.x, dy=target.y-eye.y, dz=target.z-eye.z, dist=Math.sqrt(dx*dx+dz*dz);
        mc.player.setYaw((float)(MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz,dx))-90)+rng.nextGaussian()*0.8));
        mc.player.setPitch(MathHelper.clamp((float)(-Math.toDegrees(Math.atan2(dy,dist))+rng.nextGaussian()*0.5),-90,90));
    }

    private PlayerEntity nearestEnemy() {
        return mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && p.isAlive())
                .filter(p -> !noFriends.getValue() || !NovaClient.INSTANCE.moduleManager.get(AntiBot.class).isBot(p))
                .min(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(p)))
                .orElse(null);
    }
}
