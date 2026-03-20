package dev.nova.client.module.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.RenderWorldEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import dev.nova.client.util.BlockUtil;
import dev.nova.client.util.DamageUtil;
import dev.nova.client.util.RenderUtil;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Comparator;

public final class CrystalESP extends Module {
    private final BoolSetting crystalBox = register(new BoolSetting("Crystal Box",    "Box on crystals",        true));
    private final BoolSetting bestPlace  = register(new BoolSetting("Best Placement", "Highlight best spot",    true));
    private final NumberSetting placeRng = register(new NumberSetting("Place Range",  "Placement scan range",   5,1,8,0.1));
    private final NumberSetting alpha    = register(new NumberSetting("Alpha",         "Box opacity",            180,0,255,1));

    public CrystalESP() { super("Crystal ESP", "Visual aids for crystal PvP", Category.RENDER, -1); }

    @EventHandler
    public void onRenderWorld(RenderWorldEvent e) {
        if (mc.world == null || mc.player == null || mc.gameRenderer == null) return;
        MatrixStack matrices = new MatrixStack();

        if (crystalBox.getValue()) {
            int col = RenderUtil.argb(alpha.intValue(), 100, 180, 255);
            for (EndCrystalEntity c : mc.world.getEntitiesByClass(EndCrystalEntity.class,
                    mc.player.getBoundingBox().expand(20), x -> true))
                RenderUtil.drawEntityBox(matrices, c, e.delta, col);
        }

        if (bestPlace.getValue()) {
            PlayerEntity target = mc.world.getPlayers().stream()
                    .filter(p -> p != mc.player && p.isAlive())
                    .min(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(p)))
                    .orElse(null);
            if (target != null) {
                BlockPos best = findBest(target);
                if (best != null) drawBlockOutline(best, RenderUtil.argb(alpha.intValue(), 50, 255, 80), e.delta);
            }
        }
    }

    private BlockPos findBest(PlayerEntity target) {
        BlockPos pp = mc.player.getBlockPos(); Vec3d eye = mc.player.getEyePos();
        float selfHp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        BlockPos best = null; double bestScore = -Double.MAX_VALUE;
        for (int dx=-5;dx<=5;dx++) for (int dz=-5;dz<=5;dz++) for (int dy=-2;dy<=3;dy++) {
            BlockPos base = pp.add(dx,dy,dz);
            if (!BlockUtil.canPlaceCrystal(base)) continue;
            if (eye.distanceTo(Vec3d.ofCenter(base).add(0,0.5,0)) > placeRng.getValue()) continue;
            Vec3d boom = Vec3d.ofCenter(base.up());
            float selfDmg = DamageUtil.crystalDamage(mc.player, boom);
            if (selfDmg >= selfHp) continue;
            double score = DamageUtil.crystalDamage(target, boom) * 2.0 - selfDmg;
            if (score > bestScore) { bestScore = score; best = base; }
        }
        return best;
    }

    private void drawBlockOutline(BlockPos pos, int color, float delta) {
        if (mc.gameRenderer == null) return;
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        float x=(float)(pos.getX()-cam.x), y=(float)(pos.getY()+1-cam.y), z=(float)(pos.getZ()-cam.z);
        float rv=((color>>16)&0xFF)/255f, gv=((color>>8)&0xFF)/255f, bv=(color&0xFF)/255f, av=((color>>24)&0xFF)/255f;

        RenderSystem.enableBlend(); RenderSystem.disableDepthTest();
        MatrixStack ms = new MatrixStack();
        var buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f mat = ms.peek().getPositionMatrix();
        float[][] pts = {{x,y,z},{x+1,y,z},{x+1,y,z+1},{x,y,z+1}};
        for (int i=0;i<4;i++){float[] p1=pts[i],p2=pts[(i+1)%4];
            buf.vertex(mat,p1[0],p1[1],p1[2]).color(rv,gv,bv,av);
            buf.vertex(mat,p2[0],p2[1],p2[2]).color(rv,gv,bv,av);}
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.enableDepthTest(); RenderSystem.disableBlend();
    }
}
