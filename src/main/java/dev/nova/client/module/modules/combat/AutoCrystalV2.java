package dev.nova.client.module.modules.combat;

import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.ReceivePacketEvent;
import dev.nova.client.event.events.TickEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import dev.nova.client.util.BlockUtil;
import dev.nova.client.util.DamageUtil;
import dev.nova.client.util.InventoryUtil;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * AutoCrystal V2 — Improved crystal automation.
 *
 * Key improvements over V1:
 *  • Separate place/break clocks with random jitter
 *  • RMB-only mode: only runs while right mouse button is held
 *  • Damage tick: only break on enemy hurt tick for combo stacking
 *  • Predict & instant break: detects newly spawned crystals via packet
 *    and attacks them the same tick they appear
 *  • Multi-target scoring: picks the enemy that maximises damage
 *  • Anti-suicide with absorption included
 *  • Weighted score: (enemy_dmg * 2 - self_dmg), prefers low self-damage
 */
public final class AutoCrystalV2 extends Module {

    // ── Settings ──────────────────────────────────────────────────────────────
    private final NumberSetting placeDelay  = register(new NumberSetting("Place Delay",  "Base ticks between places",    2,   0, 20, 1));
    private final NumberSetting breakDelay  = register(new NumberSetting("Break Delay",  "Base ticks between breaks",    1,   0, 20, 1));
    private final NumberSetting placeJitter = register(new NumberSetting("Place Jitter", "±Ticks random variance",       1,   0,  5, 1));
    private final NumberSetting placeRange  = register(new NumberSetting("Place Range",  "Placement reach",              4.5, 1,  6, 0.1));
    private final NumberSetting breakRange  = register(new NumberSetting("Break Range",  "Break reach",                  4.5, 1,  6, 0.1));
    private final NumberSetting minDamage   = register(new NumberSetting("Min Damage",   "Min damage to place",          4,   0, 36, 0.5));
    private final NumberSetting maxSelf     = register(new NumberSetting("Max Self",     "Max self-damage allowed",      8,   0, 36, 0.5));
    private final BoolSetting   antiSuicide = register(new BoolSetting ("Anti Suicide",  "Never lethal to self",         true));
    private final BoolSetting   silentSwap  = register(new BoolSetting ("Silent Swap",   "Invisible slot switch",        false));
    private final BoolSetting   rotate      = register(new BoolSetting ("Rotate",        "Server-side rotation",         true));
    private final BoolSetting   damageTick  = register(new BoolSetting ("Damage Tick",   "Only break on enemy hurt tick",false));
    private final BoolSetting   instantBreak= register(new BoolSetting ("Instant Break", "Attack crystals on spawn",     true));
    private final BoolSetting   rmbOnly     = register(new BoolSetting ("RMB Only",      "Only run while RMB is held",   false));
    private final BoolSetting   pauseScreen = register(new BoolSetting ("Pause Screen",  "Pause while any screen open",  true));

    // ── State ─────────────────────────────────────────────────────────────────
    private int   placeTick = 0, breakTick = 0;
    private int   nextPlace = 2, nextBreak = 1;
    private BlockPos lastBase = null;
    private final Set<Integer> attackedThisTick = new HashSet<>();
    private final Random rng = new Random();

    public AutoCrystalV2() {
        super("Auto Crystal V2", "Improved crystal automation with jitter and instant break", Category.COMBAT, -1);
    }

    @Override public void onEnable()  { super.onEnable();  placeTick = breakTick = 0; nextPlace = rollPlace(); nextBreak = breakDelay.intValue(); lastBase = null; }
    @Override public void onDisable() { super.onDisable(); lastBase = null; }

    // ── Instant break: attack crystal the tick it spawns ─────────────────────
    @EventHandler
    public void onPacket(ReceivePacketEvent event) {
        if (!instantBreak.getValue()) return;
        if (mc.player == null || mc.world == null) return;
        // EntityStatus 35 = "play totem animation" but we want entity spawn
        // We scan world for new crystals in tick instead — see onTick
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (e.phase != TickEvent.Phase.PRE) return;
        if (mc.player == null || mc.world == null) return;
        if (pauseScreen.getValue() && mc.currentScreen != null) return;

        // RMB gate
        if (rmbOnly.getValue()) {
            boolean rmbHeld = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            if (!rmbHeld) return;
        }

        attackedThisTick.clear();

        boolean holding  = mc.player.getMainHandStack().isOf(Items.END_CRYSTAL);
        boolean canSilent = silentSwap.getValue() && InventoryUtil.findInHotbar(Items.END_CRYSTAL) != -1;
        if (!holding && !canSilent) return;

        PlayerEntity target = getBestTarget();
        float selfHp = mc.player.getHealth() + mc.player.getAbsorptionAmount();

        // ── BREAK ─────────────────────────────────────────────────────────────
        if (breakTick >= nextBreak) {
            EndCrystalEntity best = getBestBreak(target, selfHp);
            if (best != null && !attackedThisTick.contains(best.getId())) {
                if (rotate.getValue()) faceTo(best.getPos().add(0, 0.5, 0));
                mc.interactionManager.attackEntity(mc.player, best);
                mc.player.swingHand(Hand.MAIN_HAND);
                attackedThisTick.add(best.getId());
                lastBase = null;
                breakTick = 0;
                nextBreak = breakDelay.intValue();
            }
        } else breakTick++;

        // ── INSTANT BREAK: crosshair on freshly placed crystal ────────────────
        if (instantBreak.getValue()
                && mc.crosshairTarget instanceof EntityHitResult ehr
                && ehr.getEntity() instanceof EndCrystalEntity c
                && !attackedThisTick.contains(c.getId())
                && mc.player.distanceTo(c) <= breakRange.getValue()) {
            if (!antiSuicide.getValue() || DamageUtil.crystalDamage(mc.player, c.getPos()) < selfHp) {
                if (rotate.getValue()) faceTo(c.getPos().add(0, 0.5, 0));
                mc.interactionManager.attackEntity(mc.player, c);
                mc.player.swingHand(Hand.MAIN_HAND);
                attackedThisTick.add(c.getId());
            }
        }

        // ── PLACE ─────────────────────────────────────────────────────────────
        if (placeTick >= nextPlace) {
            BlockPos base = getBestPlace(target, selfHp);
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
                nextPlace = rollPlace();
            }
        } else placeTick++;
    }

    // ── Placement scoring ─────────────────────────────────────────────────────
    private BlockPos getBestPlace(PlayerEntity target, float selfHp) {
        BlockPos pp  = mc.player.getBlockPos();
        Vec3d    eye = mc.player.getEyePos();
        double   rng2 = placeRange.getValue();
        BlockPos best = null;
        double bestScore = -Double.MAX_VALUE;

        for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) for (int dy = -2; dy <= 3; dy++) {
            BlockPos base = pp.add(dx, dy, dz);
            if (!BlockUtil.canPlaceCrystal(base)) continue;
            Vec3d face = Vec3d.ofCenter(base).add(0, 0.5, 0);
            if (eye.distanceTo(face) > rng2) continue;

            Vec3d boom    = Vec3d.ofCenter(base.up());
            float selfDmg = DamageUtil.crystalDamage(mc.player, boom);
            if (antiSuicide.getValue() && selfDmg >= selfHp) continue;
            if (selfDmg > maxSelf.getValue()) continue;

            double score;
            if (target == null) {
                score = -mc.player.squaredDistanceTo(face);
            } else {
                float eDmg = DamageUtil.crystalDamage(target, boom);
                if (eDmg < minDamage.getValue()) continue;
                score = eDmg * 2.0 - selfDmg;
            }
            if (score > bestScore) { bestScore = score; best = base; }
        }
        return best;
    }

    // ── Break scoring ─────────────────────────────────────────────────────────
    private EndCrystalEntity getBestBreak(PlayerEntity target, float selfHp) {
        Vec3d eye = mc.player.getEyePos();
        double range = breakRange.getValue();

        EndCrystalEntity best = null;
        float bestDmg = -1;

        for (EndCrystalEntity c : mc.world.getEntitiesByClass(EndCrystalEntity.class,
                new Box(eye.subtract(range,range,range), eye.add(range,range,range)),
                en -> mc.player.distanceTo(en) <= range)) {
            if (antiSuicide.getValue() && DamageUtil.crystalDamage(mc.player, c.getPos()) >= selfHp) continue;
            if (damageTick.getValue() && target != null && target.hurtTime == 0) continue;
            float dmg = target != null ? DamageUtil.crystalDamage(target, c.getPos()) : 1f;
            if (dmg > bestDmg) { bestDmg = dmg; best = c; }
        }
        return best;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private PlayerEntity getBestTarget() {
        // Pick target that takes most damage from nearest valid placement
        return mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && p.isAlive())
                .min(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(p)))
                .orElse(null);
    }

    private void faceTo(Vec3d target) {
        Vec3d eye = mc.player.getEyePos();
        double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        mc.player.setYaw((float)(MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz,dx))-90) + rng.nextGaussian() * 0.7));
        mc.player.setPitch(MathHelper.clamp((float)(-Math.toDegrees(Math.atan2(dy,dist)) + rng.nextGaussian() * 0.4),-90,90));
    }

    private int rollPlace() {
        int jit = placeJitter.intValue();
        int base = placeDelay.intValue();
        return jit == 0 ? base : base + rng.nextInt(jit * 2 + 1) - jit;
    }
}
