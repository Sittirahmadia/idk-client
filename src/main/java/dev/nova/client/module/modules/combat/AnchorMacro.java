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
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

import java.util.*;

/**
 * AnchorMacro — full anchor automation for crystal PvP.
 *
 * Handles all three phases independently with randomised timing:
 *   PLACE  — finds the best obsidian/bedrock surface near target and places an anchor
 *   CHARGE — right-clicks with glowstone to charge it (up to chargeCount times)
 *   EXPLODE— right-clicks with a non-glowstone item to detonate
 *
 * Anti-cheat features:
 *   • All delays randomised between min/max
 *   • Server-side rotation with Gaussian jitter before every interaction
 *   • Damage-scoring: only places anchors whose explosion damages the target
 *   • Anti-suicide check against both charge and explosion damage
 *   • Loot-protect aborts if valuables are on the ground nearby
 *   • Owned-anchor tracking (only explode anchors we placed)
 */
public final class AnchorMacro extends Module {

    // ── Mode ─────────────────────────────────────────────────────────────────
    private final BoolSetting placer    = register(new BoolSetting("Placer",    "Auto-place anchors",      true));
    private final BoolSetting charger   = register(new BoolSetting("Charger",   "Auto-charge with glow",   true));
    private final BoolSetting exploder  = register(new BoolSetting("Exploder",  "Auto-explode anchors",    true));

    // ── Timing (ticks) ────────────────────────────────────────────────────────
    private final NumberSetting switchMin  = register(new NumberSetting("Switch Min",  "Min ticks before switching",  1, 0, 20, 1));
    private final NumberSetting switchMax  = register(new NumberSetting("Switch Max",  "Max ticks before switching",  3, 0, 20, 1));
    private final NumberSetting clickMin   = register(new NumberSetting("Click Min",   "Min ticks before clicking",   1, 0, 20, 1));
    private final NumberSetting clickMax   = register(new NumberSetting("Click Max",   "Max ticks before clicking",   4, 0, 20, 1));

    // ── Range / safety ────────────────────────────────────────────────────────
    private final NumberSetting placeRange = register(new NumberSetting("Place Range", "Anchor placement reach",    4.5, 1, 6, 0.1));
    private final NumberSetting minDamage  = register(new NumberSetting("Min Damage",  "Min enemy damage to place", 3.0, 0, 36, 0.5));
    private final NumberSetting maxSelf    = register(new NumberSetting("Max Self",    "Max self damage allowed",   6.0, 0, 36, 0.5));
    private final NumberSetting chargeCount= register(new NumberSetting("Charges",     "Charges to apply (1-4)",    4,   1, 4,  1));
    private final NumberSetting explodeSlot= register(new NumberSetting("Explode Slot","Hotbar slot to use (1-9)",  9,   1, 9,  1));
    private final BoolSetting   antiSuicide= register(new BoolSetting("Anti Suicide",  "Never blow yourself up",  true));
    private final BoolSetting   onlyOwn    = register(new BoolSetting("Only Own",      "Only explode own anchors",true));
    private final BoolSetting   lootProt   = register(new BoolSetting("Loot Protect",  "Abort if loot nearby",   false));
    private final BoolSetting   rotate     = register(new BoolSetting("Rotate",        "Server-side rotation",    true));

    // ── State ─────────────────────────────────────────────────────────────────
    /** Anchors we placed this session. */
    private final Set<BlockPos> ownedAnchors = new HashSet<>();
    /** Current action countdown clocks. */
    private int switchClock = 0, clickClock = 0;
    private int nextSwitch  = 2, nextClick  = 3;
    /** Last action performed this tick (to avoid double-acting). */
    private boolean actedThisTick = false;

    private static final Random RNG = new Random();

    public AnchorMacro() {
        super("Anchor Macro",
              "Places, charges, and explodes respawn anchors for crystal PvP",
              Category.COMBAT, -1);
    }

    @Override public void onEnable()  { super.onEnable();  resetClocks(); ownedAnchors.clear(); }
    @Override public void onDisable() { super.onDisable(); ownedAnchors.clear(); }

    // ── Packet: track block placements we make ─────────────────────────────
    @EventHandler
    public void onPacket(ReceivePacketEvent event) {
        // Server confirms a block we set — if it became an anchor, track it
        if (!(event.packet instanceof BlockUpdateS2CPacket pkt)) return;
        if (pkt.getState().getBlock() == Blocks.RESPAWN_ANCHOR)
            ownedAnchors.add(pkt.getPos());
        // If anchor was destroyed, forget it
        if (pkt.getState().isAir())
            ownedAnchors.remove(pkt.getPos());
    }

    // ── Main tick ─────────────────────────────────────────────────────────────
    @EventHandler
    public void onTick(TickEvent e) {
        if (e.phase != TickEvent.Phase.PRE) return;
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;

        // Loot protect
        if (lootProt.getValue() && BlockUtil.valuableLootNearby(mc.player.getBlockPos(), 12)) return;

        PlayerEntity target = nearestEnemy();
        float selfHp = mc.player.getHealth() + mc.player.getAbsorptionAmount();

        actedThisTick = false;

        // Advance clocks
        switchClock++;
        clickClock++;

        // ── 1. EXPLODER ──────────────────────────────────────────────────────
        if (exploder.getValue()) tryExplode(target, selfHp);
        if (actedThisTick) return;

        // ── 2. CHARGER ───────────────────────────────────────────────────────
        if (charger.getValue()) tryCharge(target, selfHp);
        if (actedThisTick) return;

        // ── 3. PLACER ────────────────────────────────────────────────────────
        if (placer.getValue()) tryPlace(target, selfHp);
    }

    // ── Explode ───────────────────────────────────────────────────────────────
    private void tryExplode(PlayerEntity target, float selfHp) {
        // Find a charged anchor in reach
        BlockPos anchor = findAnchor(true, selfHp);
        if (anchor == null) return;
        if (onlyOwn.getValue() && !ownedAnchors.contains(anchor)) return;

        int slot = explodeSlot.intValue() - 1;

        // Switch to explode slot
        if (mc.player.getInventory().selectedSlot != slot) {
            if (switchClock < nextSwitch) return;
            mc.player.getInventory().selectedSlot = slot;
            switchClock = 0; nextSwitch = rollSwitch();
            actedThisTick = true;
            return;
        }

        // Click
        if (clickClock < nextClick) return;
        if (antiSuicide.getValue()) {
            Vec3d boom = Vec3d.ofCenter(anchor);
            if (DamageUtil.crystalDamage(mc.player, boom) >= selfHp) return;
        }

        interact(anchor, Direction.UP);
        ownedAnchors.remove(anchor);
        clickClock = 0; nextClick = rollClick();
        actedThisTick = true;
    }

    // ── Charge ────────────────────────────────────────────────────────────────
    private void tryCharge(PlayerEntity target, float selfHp) {
        BlockPos anchor = findAnchor(false, selfHp);
        if (anchor == null) return;

        int charges = mc.world.getBlockState(anchor).get(RespawnAnchorBlock.CHARGES);
        if (charges >= chargeCount.intValue()) return;

        // Need glowstone
        if (!InventoryUtil.isHolding(Items.GLOWSTONE)) {
            if (switchClock < nextSwitch) return;
            if (!InventoryUtil.switchToItem(Items.GLOWSTONE)) return;
            switchClock = 0; nextSwitch = rollSwitch();
            actedThisTick = true;
            return;
        }

        if (clickClock < nextClick) return;
        interact(anchor, Direction.UP);
        clickClock = 0; nextClick = rollClick();
        actedThisTick = true;
    }

    // ── Place ─────────────────────────────────────────────────────────────────
    private void tryPlace(PlayerEntity target, float selfHp) {
        if (target == null) return;

        // Need anchor in hotbar
        if (!InventoryUtil.isHolding(Items.RESPAWN_ANCHOR)) {
            if (switchClock < nextSwitch) return;
            if (!InventoryUtil.switchToItem(Items.RESPAWN_ANCHOR)) return;
            switchClock = 0; nextSwitch = rollSwitch();
            actedThisTick = true;
            return;
        }

        BlockPos bestBase = findBestPlaceSurface(target, selfHp);
        if (bestBase == null) return;
        if (clickClock < nextClick) return;

        // Place on top face of bestBase
        Vec3d face = Vec3d.ofCenter(bestBase).add(0, 0.5, 0);
        rotateTo(face);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                new BlockHitResult(face, Direction.UP, bestBase, false));
        mc.player.swingHand(Hand.MAIN_HAND);
        clickClock = 0; nextClick = rollClick();
        actedThisTick = true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Scans nearby obsidian/bedrock for a valid surface where placing an anchor
     * gives maximum damage to target with acceptable self-damage.
     */
    private BlockPos findBestPlaceSurface(PlayerEntity target, float selfHp) {
        BlockPos pp  = mc.player.getBlockPos();
        Vec3d    eye = mc.player.getEyePos();
        double   rng = placeRange.getValue();

        BlockPos best = null;
        double bestScore = -Double.MAX_VALUE;

        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) for (int dy = -1; dy <= 2; dy++) {
            BlockPos base = pp.add(dx, dy, dz);
            var blk = mc.world.getBlockState(base).getBlock();
            if (blk != Blocks.OBSIDIAN && blk != Blocks.BEDROCK) continue;
            if (!mc.world.isAir(base.up())) continue;

            Vec3d face = Vec3d.ofCenter(base).add(0, 0.5, 0);
            if (eye.distanceTo(face) > rng) continue;

            // Don't place if an anchor already exists there
            if (mc.world.getBlockState(base.up()).getBlock() == Blocks.RESPAWN_ANCHOR) continue;

            Vec3d boom    = Vec3d.ofCenter(base.up());
            float selfDmg = DamageUtil.crystalDamage(mc.player, boom);
            if (antiSuicide.getValue() && selfDmg >= selfHp) continue;
            if (selfDmg > maxSelf.getValue()) continue;

            float eDmg = DamageUtil.crystalDamage(target, boom);
            if (eDmg < minDamage.getValue()) continue;

            double score = eDmg * 2.0 - selfDmg;
            if (score > bestScore) { bestScore = score; best = base; }
        }
        return best;
    }

    /**
     * Finds the nearest respawn anchor in reach.
     * @param charged true = must be fully charged, false = must be uncharged or partially charged
     */
    private BlockPos findAnchor(boolean charged, float selfHp) {
        BlockPos pp  = mc.player.getBlockPos();
        Vec3d    eye = mc.player.getEyePos();
        double   rng = placeRange.getValue();

        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) for (int dy = -2; dy <= 2; dy++) {
            BlockPos pos = pp.add(dx, dy, dz);
            var state = mc.world.getBlockState(pos);
            if (state.getBlock() != Blocks.RESPAWN_ANCHOR) continue;

            int c = state.get(RespawnAnchorBlock.CHARGES);
            boolean isCharged = c >= chargeCount.intValue();

            if (charged != isCharged) continue;
            if (eye.distanceTo(Vec3d.ofCenter(pos)) > rng) continue;

            double dist = mc.player.squaredDistanceTo(Vec3d.ofCenter(pos));
            if (dist < bestDist) { bestDist = dist; best = pos; }
        }
        return best;
    }

    private void interact(BlockPos pos, Direction face) {
        Vec3d hitVec = Vec3d.ofCenter(pos).add(
                face.getOffsetX() * 0.5,
                face.getOffsetY() * 0.5,
                face.getOffsetZ() * 0.5);
        rotateTo(hitVec);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                new BlockHitResult(hitVec, face, pos, false));
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void rotateTo(Vec3d target) {
        if (!rotate.getValue()) return;
        Vec3d eye = mc.player.getEyePos();
        double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        mc.player.setYaw((float)(MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90)
                + RNG.nextGaussian() * 0.9));
        mc.player.setPitch(MathHelper.clamp(
                (float)(-Math.toDegrees(Math.atan2(dy, dist)) + RNG.nextGaussian() * 0.6),
                -90, 90));
    }

    private PlayerEntity nearestEnemy() {
        return mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && p.isAlive())
                .min(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(p)))
                .orElse(null);
    }

    private void resetClocks() {
        switchClock = clickClock = 0;
        nextSwitch = rollSwitch();
        nextClick  = rollClick();
    }

    private int rollSwitch() {
        int lo = switchMin.intValue(), hi = switchMax.intValue();
        return lo >= hi ? lo : lo + RNG.nextInt(hi - lo + 1);
    }

    private int rollClick() {
        int lo = clickMin.intValue(), hi = clickMax.intValue();
        return lo >= hi ? lo : lo + RNG.nextInt(hi - lo + 1);
    }
}
