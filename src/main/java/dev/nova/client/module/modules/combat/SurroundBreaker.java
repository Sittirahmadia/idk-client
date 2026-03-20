package dev.nova.client.module.modules.combat;

import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.TickEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import dev.nova.client.util.BlockUtil;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class SurroundBreaker extends Module {
    private final NumberSetting range = register(new NumberSetting("Range", "Target scan range", 6,   1,16,0.5));
    private final NumberSetting reach = register(new NumberSetting("Reach", "Break reach",       4.5, 1, 6,0.1));
    private final NumberSetting delay = register(new NumberSetting("Delay", "Ticks between hits",1,   0,10,  1));
    private int clock = 0;

    public SurroundBreaker() { super("Surround Breaker", "Breaks obsidian from enemy surround", Category.COMBAT, -1); }

    @EventHandler
    public void onTick(TickEvent e) {
        if (e.phase!=TickEvent.Phase.PRE||mc.player==null||mc.world==null) return;
        if (clock++<delay.intValue()) return; clock=0;

        PlayerEntity target = mc.world.getPlayers().stream()
                .filter(p->p!=mc.player&&p.isAlive()&&mc.player.distanceTo(p)<=range.getValue())
                .min((a,b)->Double.compare(mc.player.squaredDistanceTo(a),mc.player.squaredDistanceTo(b)))
                .orElse(null);
        if (target==null) return;

        BlockPos feet = target.getBlockPos();
        for (Direction d : new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST}) {
            BlockPos side = feet.offset(d);
            if (mc.world.getBlockState(side).getBlock()!=Blocks.OBSIDIAN) continue;
            if (!BlockUtil.inReach(side, reach.getValue())) continue;

            Vec3d c = Vec3d.ofCenter(side), eye = mc.player.getEyePos();
            double dx=c.x-eye.x, dy=c.y-eye.y, dz=c.z-eye.z, dist=Math.sqrt(dx*dx+dz*dz);
            mc.player.setYaw((float)(Math.toDegrees(Math.atan2(dz,dx))-90));
            mc.player.setPitch((float)-Math.toDegrees(Math.atan2(dy,dist)));
            mc.interactionManager.updateBlockBreakingProgress(side, d.getOpposite());
            mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            break;
        }
    }
}
