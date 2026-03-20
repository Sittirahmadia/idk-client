package dev.nova.client.module.modules.combat;

import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.TickEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import dev.nova.client.util.BlockUtil;
import dev.nova.client.util.InventoryUtil;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

public final class HoleFiller extends Module {
    private final NumberSetting range     = register(new NumberSetting("Range",     "Hole scan range",      5,   1,10,0.5));
    private final NumberSetting reach     = register(new NumberSetting("Reach",     "Place reach",          4.5, 1, 6,0.1));
    private final NumberSetting delay     = register(new NumberSetting("Delay",     "Ticks between places", 2,   0,10,  1));
    private final BoolSetting   onlyEnemy = register(new BoolSetting ("Only Enemy","Only fill enemy holes", true));
    private final BoolSetting   safeOnly  = register(new BoolSetting ("Safe Only", "Only bedrock holes",    true));
    private final BoolSetting   rotate    = register(new BoolSetting ("Rotate",    "Face block while placing",true));

    private int clock = 0;

    public HoleFiller() { super("Hole Filler", "Fills holes near enemies", Category.COMBAT, -1); }

    @EventHandler
    public void onTick(TickEvent e) {
        if (e.phase != TickEvent.Phase.PRE || mc.player == null || mc.world == null) return;
        if (clock++ < delay.intValue()) return; clock = 0;
        if (!InventoryUtil.switchToItem(Items.OBSIDIAN)) return;

        BlockPos pp = mc.player.getBlockPos();
        int r = range.intValue();

        outer:
        for (int dx=-r;dx<=r;dx++) for (int dz=-r;dz<=r;dz++) for (int dy=-2;dy<=1;dy++) {
            BlockPos candidate = pp.add(dx,dy,dz);
            if (!mc.world.isAir(candidate) || !mc.world.isAir(candidate.up())) continue;
            if (safeOnly.getValue() && !BlockUtil.isSafeHole(candidate)) continue;
            if (onlyEnemy.getValue()) {
                boolean near = mc.world.getPlayers().stream()
                        .filter(p -> p != mc.player)
                        .anyMatch(p -> p.getBlockPos().isWithinDistance(candidate, 1.5));
                if (!near) continue;
            }
            for (Direction d : new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST}) {
                BlockPos side = candidate.offset(d);
                if (!mc.world.isAir(side)) continue;
                if (!BlockUtil.inReach(side, reach.getValue())) continue;
                BlockUtil.PlacePair pair = BlockUtil.findPlaceSide(side);
                if (pair == null) continue;
                if (rotate.getValue()) rotateTo(pair.hitVec());
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                        new BlockHitResult(pair.hitVec(), pair.face(), pair.pos(), false));
                mc.player.swingHand(Hand.MAIN_HAND);
                break outer;
            }
        }
    }

    private void rotateTo(Vec3d t) {
        Vec3d eye = mc.player.getEyePos();
        double dx=t.x-eye.x, dy=t.y-eye.y, dz=t.z-eye.z, dist=Math.sqrt(dx*dx+dz*dz);
        mc.player.setYaw((float)(Math.toDegrees(Math.atan2(dz,dx))-90));
        mc.player.setPitch(MathHelper.clamp((float)-Math.toDegrees(Math.atan2(dy,dist)),-90,90));
    }
}
