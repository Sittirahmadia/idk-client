package dev.nova.client.module.modules.combat;

import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.TickEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import dev.nova.client.util.InventoryUtil;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

public final class Surround extends Module {
    private final NumberSetting delay = register(new NumberSetting("Delay","Ticks between placements",2,0,10,1));
    private final BoolSetting   center= register(new BoolSetting("Center","Snap to block center",true));
    private int clock=0;

    public Surround(){super("Surround","Places obsidian around your feet",Category.COMBAT,-1);}

    @EventHandler
    public void onTick(TickEvent e) {
        if (e.phase!=TickEvent.Phase.PRE||mc.player==null||mc.world==null) return;
        if (!mc.player.isOnGround()) return;
        if (clock++<delay.intValue()) return; clock=0;
        if (!InventoryUtil.switchToItem(Items.OBSIDIAN)) return;
        if (center.getValue()) {
            BlockPos p=mc.player.getBlockPos();
            mc.player.setPosition(p.getX()+0.5,mc.player.getY(),p.getZ()+0.5);
        }
        BlockPos feet=mc.player.getBlockPos();
        Direction[] sides={Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST};
        for (Direction d:sides) {
            BlockPos target=feet.offset(d);
            if (!mc.world.isAir(target)) continue;
            Vec3d hit=Vec3d.ofCenter(target).add(d.getOpposite().getOffsetX()*0.5,0,d.getOpposite().getOffsetZ()*0.5);
            mc.interactionManager.interactBlock(mc.player,Hand.MAIN_HAND,
                    new BlockHitResult(hit,d.getOpposite(),target,false));
        }
    }
}
