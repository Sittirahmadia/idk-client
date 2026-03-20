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
import net.minecraft.util.math.*;

public final class Surround extends Module {
    private final NumberSetting delay     = register(new NumberSetting("Delay",    "Ticks between places",     2,  0,10, 1));
    private final BoolSetting   center    = register(new BoolSetting ("Center",    "Snap to block center",     true));
    private final BoolSetting   onGround  = register(new BoolSetting ("On Ground", "Only when grounded",       true));
    private final BoolSetting   topLayer  = register(new BoolSetting ("Top Layer", "Place second layer above", false));
    private final BoolSetting   corners   = register(new BoolSetting ("Corners",   "Also fill diagonal corners",false));
    private int clock = 0;

    public Surround() { super("Surround", "Places obsidian around your feet", Category.COMBAT, -1); }

    @EventHandler
    public void onTick(TickEvent e) {
        if (e.phase != TickEvent.Phase.PRE || mc.player == null || mc.world == null) return;
        if (onGround.getValue() && !mc.player.isOnGround()) return;
        if (clock++ < delay.intValue()) return;
        clock = 0;
        if (!InventoryUtil.switchToItem(Items.OBSIDIAN)) return;

        if (center.getValue()) {
            BlockPos p = mc.player.getBlockPos();
            mc.player.setPosition(p.getX() + 0.5, mc.player.getY(), p.getZ() + 0.5);
        }

        BlockPos feet = mc.player.getBlockPos();
        boolean placed = false;

        // Cardinal sides
        Direction[] sides = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction d : sides) {
            BlockPos target = feet.offset(d);
            if (!mc.world.isAir(target)) continue;
            BlockUtil.PlacePair pair = BlockUtil.findPlaceSide(target);
            if (pair == null) continue;
            rotateTo(pair.hitVec());
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                    new BlockHitResult(pair.hitVec(), pair.face(), pair.pos(), false));
            placed = true;
        }

        // Diagonal corners
        if (corners.getValue()) {
            int[][] diags = {{1,1},{1,-1},{-1,1},{-1,-1}};
            for (int[] d : diags) {
                BlockPos target = feet.add(d[0], 0, d[1]);
                if (!mc.world.isAir(target)) continue;
                BlockUtil.PlacePair pair = BlockUtil.findPlaceSide(target);
                if (pair == null) continue;
                rotateTo(pair.hitVec());
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                        new BlockHitResult(pair.hitVec(), pair.face(), pair.pos(), false));
            }
        }

        // Top layer
        if (topLayer.getValue() && placed) {
            BlockPos up = feet.up();
            for (Direction d : sides) {
                BlockPos target = up.offset(d);
                if (!mc.world.isAir(target)) continue;
                BlockUtil.PlacePair pair = BlockUtil.findPlaceSide(target);
                if (pair == null) continue;
                rotateTo(pair.hitVec());
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                        new BlockHitResult(pair.hitVec(), pair.face(), pair.pos(), false));
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
