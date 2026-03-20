package dev.nova.client.module.modules.render;

import dev.nova.client.NovaClient;
import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.RenderWorldEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.modules.combat.PopCounter;
import dev.nova.client.module.setting.Setting.*;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class NameTags extends Module {
    private final BoolSetting health = register(new BoolSetting("Health",  "Show HP",          true));
    private final BoolSetting pops   = register(new BoolSetting("Pops",    "Show pop count",   true));
    private final BoolSetting dist   = register(new BoolSetting("Distance","Show distance",    false));
    private final NumberSetting scl  = register(new NumberSetting("Scale", "Text scale",       1.0, 0.3, 3.0, 0.1));

    public NameTags() { super("Name Tags", "Player info above head", Category.RENDER, -1); }

    @EventHandler
    public void onRenderWorld(RenderWorldEvent e) {
        if (mc.world == null || mc.player == null || mc.gameRenderer == null) return;
        PopCounter pc = NovaClient.INSTANCE.moduleManager.get(PopCounter.class);
        Camera cam = mc.gameRenderer.getCamera();
        Vec3d camPos = cam.getPos();

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            Vec3d worldPos = p.getLerpedPos(e.delta).add(0, p.getHeight() + 0.35, 0);

            // Basic behind-camera cull
            Vec3d rel = worldPos.subtract(camPos);
            Vec3d look = Vec3d.fromPolar(cam.getPitch(), cam.getYaw());
            if (look.dotProduct(rel.normalize()) < 0) continue;

            StringBuilder sb = new StringBuilder("§f").append(p.getName().getString());
            if (health.getValue()) {
                float hp = p.getHealth() + p.getAbsorptionAmount();
                String c = hp > 14 ? "§a" : hp > 7 ? "§e" : "§c";
                sb.append(" ").append(c).append(String.format("%.1f❤", hp));
            }
            if (pops.getValue() && pc != null && pc.isEnabled()) {
                int n = pc.getPopCount(p);
                if (n > 0) sb.append(" §c[").append(n).append("p]");
            }
            if (dist.getValue())
                sb.append(" §7").append(String.format("%.0fm", mc.player.distanceTo(p)));

            renderTag(worldPos, camPos, cam, sb.toString());
        }
    }

    private void renderTag(Vec3d world, Vec3d cam, Camera camera, String text) {
        MatrixStack ms = new MatrixStack();
        ms.push();
        ms.translate(world.x - cam.x, world.y - cam.y, world.z - cam.z);
        ms.multiply(camera.getRotation());
        float s = scl.floatValue() * 0.025f;
        ms.scale(-s, -s, s);

        TextRenderer tr = mc.textRenderer;
        int w = tr.getWidth(text);
        int ox = -w / 2;
        var bufSrc = mc.getBufferBuilders().getEntityVertexConsumers();
        tr.draw(text, ox, 0, 0xFFFFFFFF, true, ms.peek().getPositionMatrix(), bufSrc,
                TextRenderer.TextLayerType.SEE_THROUGH, 0x55000000,
                LightmapTextureManager.MAX_LIGHT_COORDINATE);
        bufSrc.draw();
        ms.pop();
    }
}
