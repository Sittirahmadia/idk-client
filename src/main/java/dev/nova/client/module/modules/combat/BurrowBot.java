package dev.nova.client.module.modules.combat;

import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.TickEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import dev.nova.client.util.BlockUtil;
import dev.nova.client.util.InventoryUtil;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * BurrowBot
 * Finds the nearest safe bedrock/obsidian hole and moves the player into it,
 * then places obsidian to seal the surrounding walls.
 */
public final class BurrowBot extends Module {

    private final NumberSetting searchRange = register(new NumberSetting("Search Range","Hole search radius",  6,  1,16,0.5));
    private final NumberSetting delay       = register(new NumberSetting("Delay",       "Ticks between actions",2, 0,10, 1));
    private final BoolSetting   seal        = register(new BoolSetting ("Seal",         "Place obsidian walls",true));
    private final BoolSetting   autoDisable = register(new BoolSetting ("Auto Disable", "Disable when in hole", true));

    private enum Phase { FIND, MOVE, SEAL, DONE }
    private Phase phase = Phase.FIND;
    private BlockPos targetHole = null;
    private int clock = 0;

    public BurrowBot() {
        super("Burrow Bot", "Automatically moves into the nearest safe hole", Category.COMBAT, -1);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        phase = Phase.FIND;
        targetHole = null;
        clock = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        targetHole = null;
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (e.phase != TickEvent.Phase.PRE || mc.player == null || mc.world == null) return;
        if (clock++ < delay.intValue()) return;
        clock = 0;

        switch (phase) {
            case FIND -> {
                targetHole = findNearestHole();
                if (targetHole == null) return;
                phase = Phase.MOVE;
            }
            case MOVE -> {
                if (targetHole == null) { phase = Phase.FIND; return; }
                Vec3d center = Vec3d.ofCenter(targetHole);
                mc.player.setPosition(center.x, targetHole.getY(), center.z);
                phase = seal.getValue() ? Phase.SEAL : Phase.DONE;
            }
            case SEAL -> {
                if (!InventoryUtil.switchToItem(Items.OBSIDIAN)) { phase = Phase.DONE; return; }
                boolean placed = false;
                for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                    BlockPos side = targetHole.offset(d);
                    if (!BlockUtil.isAir(side)) continue;
                    BlockUtil.PlacePair pair = BlockUtil.findPlaceSide(side);
                    if (pair == null) continue;
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                            new BlockHitResult(pair.hitVec(), pair.face(), pair.pos(), false));
                    mc.player.swingHand(Hand.MAIN_HAND);
                    placed = true;
                    break;
                }
                // Check if all sides sealed
                boolean allSealed = BlockUtil.sideNeighbors(targetHole).stream()
                        .noneMatch(BlockUtil::isAir);
                if (allSealed) phase = Phase.DONE;
                if (!placed) phase = Phase.DONE;
            }
            case DONE -> {
                if (autoDisable.getValue()) { setEnabled(false); }
                phase = Phase.FIND;
            }
        }
    }

    private BlockPos findNearestHole() {
        BlockPos pp = mc.player.getBlockPos();
        int r = searchRange.intValue();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) for (int dy = -2; dy <= 0; dy++) {
            BlockPos candidate = pp.add(dx, dy, dz);
            if (!BlockUtil.isSafeHole(candidate)) continue;
            double dist = mc.player.squaredDistanceTo(Vec3d.ofCenter(candidate));
            if (dist < bestDist) { bestDist = dist; best = candidate; }
        }
        return best;
    }
}
