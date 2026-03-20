package dev.nova.client.gui;

import dev.nova.client.NovaClient;
import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.RenderHudEvent;
import dev.nova.client.module.Module;
import dev.nova.client.util.RenderUtil;
import net.minecraft.client.MinecraftClient;

import java.util.Comparator;
import java.util.List;

public final class HudRenderer {

    private static final int ITEM_H  = 11;
    private static final int PADDING = 4;
    private static final int BG      = 0xAA0D0D10;

    private float[] animWidths = new float[64];
    private float   hue        = 0f;

    public HudRenderer() {
        NovaClient.INSTANCE.eventBus.subscribe(this);
    }

    @EventHandler
    public void onRenderHud(RenderHudEvent event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        hue = (hue + 0.5f) % 360f;

        renderWatermark(event, screenW);
        renderArrayList(event, screenW);
        renderInfoBar(event, screenH);
    }

    private void renderWatermark(RenderHudEvent event, int screenW) {
        String name = "Nova";
        String ver  = " " + NovaClient.VERSION;
        int nameW   = RenderUtil.textWidth(name);
        int totalW  = nameW + RenderUtil.textWidth(ver) + PADDING * 2;
        int x       = (screenW - totalW) / 2;
        int y       = 4;

        RenderUtil.rect(event.context, x - PADDING, y - 2, totalW + PADDING * 2, RenderUtil.textHeight() + 4, BG);

        int rgb = hsvToRgb(hue, 0.75f, 1f);
        RenderUtil.text(event.context, name, x, y, 0xFF000000 | rgb, true);
        RenderUtil.text(event.context, ver,  x + nameW, y, 0xFFAAAAAA, true);
    }

    private void renderArrayList(RenderHudEvent event, int screenW) {
        List<Module> enabled = NovaClient.INSTANCE.moduleManager.getEnabled();
        enabled.sort(Comparator.comparingInt(m -> -RenderUtil.textWidth(m.getName())));

        if (animWidths.length < enabled.size()) animWidths = new float[enabled.size() + 16];

        int y = 4;
        for (int i = 0; i < enabled.size(); i++) {
            Module mod = enabled.get(i);
            float targetW = RenderUtil.textWidth(mod.getName()) + PADDING * 2f;
            animWidths[i] += (targetW - animWidths[i]) * 0.2f;
            int w = (int) animWidths[i];
            int x = screenW - w;

            RenderUtil.rect(event.context, x, y, w, ITEM_H, BG);
            RenderUtil.rect(event.context, x, y, 2, ITEM_H, mod.getCategory().color);
            RenderUtil.text(event.context, mod.getName(),
                    x + PADDING, y + (ITEM_H - RenderUtil.textHeight()) / 2, 0xFFE8E8F0, false);
            y += ITEM_H + 1;
        }
    }

    private void renderInfoBar(RenderHudEvent event, int screenH) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int fps  = mc.getCurrentFps();
        int ping = 0;
        var entry = mc.getNetworkHandler() != null
                ? mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) : null;
        if (entry != null) ping = entry.getLatency();

        var pos = mc.player.getBlockPos();
        String info = String.format("FPS %d  Ping %d  %d %d %d", fps, ping, pos.getX(), pos.getY(), pos.getZ());
        int tw = RenderUtil.textWidth(info) + PADDING * 2;
        int y  = screenH - RenderUtil.textHeight() - 6;

        RenderUtil.rect(event.context, 0, y - 2, tw, RenderUtil.textHeight() + 4, BG);
        RenderUtil.text(event.context, info, PADDING, y, 0xFFCCCCDD, false);
    }

    private static int hsvToRgb(float h, float s, float v) {
        float c=v*s, x=c*(1-Math.abs((h/60f)%2-1)), m=v-c;
        float r,g,b;
        if(h<60){r=c;g=x;b=0;}else if(h<120){r=x;g=c;b=0;}else if(h<180){r=0;g=c;b=x;}
        else if(h<240){r=0;g=x;b=c;}else if(h<300){r=x;g=0;b=c;}else{r=c;g=0;b=x;}
        return((int)((r+m)*255)<<16)|((int)((g+m)*255)<<8)|(int)((b+m)*255);
    }
}
