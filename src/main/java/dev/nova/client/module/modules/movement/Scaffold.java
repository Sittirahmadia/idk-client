package dev.nova.client.module.modules.movement;

import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.TickEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import dev.nova.client.util.InventoryUtil;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

public final class Scaffold extends Module {
    private final BoolSetting  tower     = register(new BoolSetting ("Tower",   "Auto-tower on jump",   false));
    private final BoolSetting  safeWalk  = register(new BoolSetting ("SafeWalk","Prevent falling off",  true));
    private final NumberSetting delay    = register(new NumberSetting("Delay",   "Ticks between places", 1, 0, 5, 1));
    private int clock = 0;

    public Scaffold() { super("Scaffold", "Places blocks under feet", Category.MOVEMENT, -1); }

    @EventHandler
    public void onTick(TickEvent e) {
        if (e.phase != TickEvent.Phase.PRE || mc.player == null || mc.world == null) return;
        if (clock++ < delay.intValue()) return; clock = 0;
        if (safeWalk.getValue()) mc.player.setSneaking(true);

        // Find block item
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof BlockItem) {
                InventoryUtil.switchTo(i); break;
            }
        }
        if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem)) return;

        BlockPos below = mc.player.getBlockPos().down();
        if (!mc.world.isAir(below)) return;

        for (Direction d : Direction.values()) {
            BlockPos adj = below.offset(d);
            BlockState st = mc.world.getBlockState(adj);
            if (!st.isAir() && st.isSolidBlock(mc.world, adj)) {
                Vec3d hitVec = Vec3d.ofCenter(adj).add(
                        d.getOpposite().getOffsetX() * 0.5,
                        d.getOpposite().getOffsetY() * 0.5,
                        d.getOpposite().getOffsetZ() * 0.5);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                        new BlockHitResult(hitVec, d.getOpposite(), adj, false));
                mc.player.swingHand(Hand.MAIN_HAND);
                return;
            }
        }
    }
}
