package dev.nova.client.module.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.RenderWorldEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import dev.nova.client.util.BlockUtil;
import dev.nova.client.util.RenderUtil;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class HoleESP extends Module {
    private final NumberSetting range = register(new NumberSetting("Range", "Search radius", 10, 1, 20, 1));
    private final NumberSetting alpha = register(new NumberSetting("Alpha", "Opacity",        160, 0, 255, 1));

    public HoleESP() { super("Hole ESP", "Highlights safe holes", Category.RENDER, -1); }

    @EventHandler
    public void onRenderWorld(RenderWorldEvent e) {
        if (mc.player == null || mc.world == null || mc.gameRenderer == null) return;
        BlockPos pp = mc.player.getBlockPos(); int r = range.intValue();
        int col = RenderUtil.argb(alpha.intValue(), 50, 200, 255);
        Vec3d cam = mc.gameRenderer.getCamera().getPos();

        for (int dx=-r;dx<=r;dx++) for (int dz=-r;dz<=r;dz++) for (int dy=-3;dy<=1;dy++) {
            BlockPos c = pp.add(dx,dy,dz);
            if (!BlockUtil.isSafeHole(c)) continue;
            drawTop(c, col, cam);
        }
    }

    private void drawTop(BlockPos pos, int color, Vec3d cam) {
        float x=(float)(pos.getX()-cam.x), y=(float)(pos.getY()-cam.y), z=(float)(pos.getZ()-cam.z);
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
