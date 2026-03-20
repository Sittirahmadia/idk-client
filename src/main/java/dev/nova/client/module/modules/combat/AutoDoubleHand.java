package dev.nova.client.module.modules.combat;

import dev.nova.client.NovaClient;
import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.ReceivePacketEvent;
import dev.nova.client.event.events.TickEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import dev.nova.client.util.BlockUtil;
import dev.nova.client.util.DamageUtil;
import dev.nova.client.util.InventoryUtil;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * AutoDoubleHand
 *
 * Keeps a totem in the hotbar ready for instant double-hand use by:
 *   ON POP   — EntityStatus 35 packet triggers immediate hotbar switch.
 *   ON HEALTH— switches when HP drops below the threshold.
 *   PREDICT  — scans placed crystals + obsidian placement positions; if any
 *              would deal lethal damage (HP - buffer), switches pre-emptively.
 */
public final class AutoDoubleHand extends Module {

    private final BoolSetting   onPop       = register(new BoolSetting  ("On Pop",        "Switch on totem pop",             true));
    private final BoolSetting   onHealth    = register(new BoolSetting  ("On Health",     "Switch below HP threshold",       true));
    private final NumberSetting health      = register(new NumberSetting("Health",        "HP trigger for On Health",        4,   1, 20, 0.5));
    private final BoolSetting   predict     = register(new BoolSetting  ("Predict",       "Pre-switch if crystal kills you", true));
    private final NumberSetting buffer      = register(new NumberSetting("Buffer",        "Extra damage margin",             1.5, 0, 10, 0.5));
    private final BoolSetting   predictPos  = register(new BoolSetting  ("Predict Pos",   "Also check obsidian placements",  true));
    private final NumberSetting predRange   = register(new NumberSetting("Pred Range",    "Obsidian scan radius",            6,   1, 12, 0.5));
    private final BoolSetting   checkEnemy  = register(new BoolSetting  ("Check Enemy",   "Only predict near enemies",       true));
    private final NumberSetting enemyDist   = register(new NumberSetting("Enemy Dist",    "Enemy radius for predict",        8,   1, 20, 0.5));
    private final BoolSetting   stopCrystal = register(new BoolSetting  ("Stop Crystal",  "Pause while Auto Crystal is on",  false));

    private volatile boolean popPending          = false;
    private boolean          belowHealthLatch    = false;

    private static final int TOTEM_POP = 35;

    public AutoDoubleHand() {
        super("Auto Double Hand",
              "Switches hotbar to totem on pop, low health, or predicted lethal hit",
              Category.COMBAT, -1);
    }

    @Override public void onEnable()  { super.onEnable();  popPending = false; belowHealthLatch = false; }
    @Override public void onDisable() { super.onDisable(); }

    // ── Packet: detect totem pop ──────────────────────────────────────────────
    @EventHandler
    public void onPacket(ReceivePacketEvent event) {
        if (!(event.packet instanceof EntityStatusS2CPacket pkt)) return;
        if (mc.player == null || mc.world == null) return;
        if (pkt.getStatus() == TOTEM_POP && pkt.getEntity(mc.world) == mc.player)
            popPending = true;
    }

    // ── Tick ──────────────────────────────────────────────────────────────────
    @EventHandler
    public void onTick(TickEvent event) {
        if (event.phase != TickEvent.Phase.PRE || mc.player == null || mc.world == null) return;

        // Optionally pause while AutoCrystal is active
        if (stopCrystal.getValue()) {
            AutoCrystal ac = NovaClient.INSTANCE.moduleManager.get(AutoCrystal.class);
            if (ac != null && ac.isEnabled()) return;
        }

        // ── 1. ON POP ──────────────────────────────────────────────────────
        if (onPop.getValue() && popPending) {
            popPending = false;
            doSwitch();
            return;
        }

        float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();

        // ── 2. ON HEALTH ───────────────────────────────────────────────────
        if (onHealth.getValue()) {
            if (hp <= health.floatValue() && !belowHealthLatch) {
                belowHealthLatch = true;
                doSwitch();
                return;
            }
            if (hp > health.floatValue()) belowHealthLatch = false;
        }

        // ── 3. PREDICT ─────────────────────────────────────────────────────
        if (!predict.getValue()) return;

        if (checkEnemy.getValue()) {
            double eDist = enemyDist.getValue();
            boolean near = mc.world.getPlayers().stream()
                    .anyMatch(p -> p != mc.player && mc.player.distanceTo(p) <= eDist);
            if (!near) return;
        }

        float killThreshold = hp - buffer.floatValue();
        List<Vec3d> checkPos = gatherCrystalPositions();

        for (Vec3d pos : checkPos) {
            if (DamageUtil.crystalDamage(mc.player, pos) >= killThreshold) {
                doSwitch();
                return;
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Vec3d> gatherCrystalPositions() {
        List<Vec3d> list = new ArrayList<>();
        Vec3d pp = mc.player.getPos();

        // Placed crystals in world
        mc.world.getEntitiesByClass(EndCrystalEntity.class,
                new Box(pp.subtract(8, 8, 8), pp.add(8, 8, 8)), x -> true)
                .forEach(c -> list.add(c.getPos()));

        // Predicted placements on obsidian/bedrock
        if (predictPos.getValue()) {
            BlockPos bp = mc.player.getBlockPos();
            int r = predRange.intValue();
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++)
                    for (int dy = -2; dy <= 3; dy++) {
                        BlockPos base = bp.add(dx, dy, dz);
                        if (BlockUtil.canPlaceCrystal(base))
                            list.add(Vec3d.ofCenter(base.up()));
                    }
        }

        return list;
    }

    /** Switch hotbar to a totem. AutoInventoryTotem handles replenishment from inventory. */
    private void doSwitch() {
        if (mc.player == null) return;
        if (mc.player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
        InventoryUtil.switchToItem(Items.TOTEM_OF_UNDYING);
    }
}
