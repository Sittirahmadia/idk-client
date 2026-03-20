package dev.nova.client.module.modules.combat;

import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.TickEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class CevBreaker extends Module {
    private final NumberSetting range    = register(new NumberSetting("Range",     "Break reach",          4.5, 1, 6, 0.1));
    private final NumberSetting delay    = register(new NumberSetting("Delay",     "Ticks between swings", 1,   0,10, 1));
    private final BoolSetting   onlyHole = register(new BoolSetting("Only Hole",  "Only break over holes", true));
    private int clock = 0;

    public CevBreaker() { super("CEV Breaker", "Breaks covers placed over enemy holes", Category.COMBAT, -1); }

    @EventHandler
    public void onTick(TickEvent e) {
        if (e.phase!=TickEvent.Phase.PRE||mc.player==null||mc.world==null) return;
        if (clock++<delay.intValue()) return; clock=0;

        double r = range.getValue();
        Vec3d eye = mc.player.getEyePos();

        PlayerEntity target = mc.world.getPlayers().stream()
                .filter(p->p!=mc.player&&p.isAlive()&&mc.player.distanceTo(p)<=16)
                .min((a,b)->Double.compare(mc.player.squaredDistanceTo(a),mc.player.squaredDistanceTo(b)))
                .orElse(null);
        if (target==null) return;

        BlockPos targetFeet = target.getBlockPos();
        BlockPos coverPos   = targetFeet.up(2);

        var block = mc.world.getBlockState(coverPos).getBlock();
        if (block!=Blocks.OBSIDIAN&&block!=Blocks.ENDER_CHEST&&block!=Blocks.CRYING_OBSIDIAN) return;
        if (eye.distanceTo(Vec3d.ofCenter(coverPos))>r) return;

        if (onlyHole.getValue()) {
            boolean isOverHole = mc.world.isAir(targetFeet) && !mc.world.isAir(targetFeet.down());
            if (!isOverHole) return;
        }

        faceBlock(coverPos);
        mc.interactionManager.updateBlockBreakingProgress(coverPos, Direction.DOWN);
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
    }

    private void faceBlock(BlockPos pos) {
        Vec3d c = Vec3d.ofCenter(pos), eye = mc.player.getEyePos();
        double dx=c.x-eye.x, dy=c.y-eye.y, dz=c.z-eye.z, dist=Math.sqrt(dx*dx+dz*dz);
        mc.player.setYaw((float)(Math.toDegrees(Math.atan2(dz,dx))-90));
        mc.player.setPitch((float)-Math.toDegrees(Math.atan2(dy,dist)));
    }
}
