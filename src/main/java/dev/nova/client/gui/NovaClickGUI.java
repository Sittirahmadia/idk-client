package dev.nova.client.gui;

import dev.nova.client.NovaClient;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting;
import dev.nova.client.util.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Nova ClickGUI
 * Dark theme with neon accent, smooth panel open/close animations,
 * per-category draggable panels, scrollable module list with settings inline.
 */
public final class NovaClickGUI extends Screen {

    // ── Theme ─────────────────────────────────────────────────────────────────
    private static final int BG          = 0xE5101014; // near-black
    private static final int PANEL_BG    = 0xF0141418;
    private static final int HEADER_BG   = 0xFF1E1E24;
    private static final int ACCENT      = 0xFF5865F2; // Discord-ish blurple
    private static final int ACCENT2     = 0xFF7B68EE;
    private static final int TEXT        = 0xFFE8E8F0;
    private static final int TEXT_DIM    = 0xFF888898;
    private static final int ENABLED_COL = 0xFF5865F2;
    private static final int SCROLL_BAR  = 0xFF333344;

    private static final int PANEL_W     = 140;
    private static final int HEADER_H    = 24;
    private static final int ITEM_H      = 18;
    private static final int SETTING_H   = 14;
    private static final int PADDING     = 6;

    // ── Panels ────────────────────────────────────────────────────────────────
    private final List<Panel> panels = new ArrayList<>();

    public NovaClickGUI() {
        super(Text.literal("Nova GUI"));
    }

    @Override
    protected void init() {
        panels.clear();
        int x = 20;
        for (Category cat : Category.values()) {
            panels.add(new Panel(cat, x, 30));
            x += PANEL_W + 10;
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Dim background
        ctx.fill(0, 0, this.width, this.height, 0x88000000);

        for (Panel p : panels) p.render(ctx, mx, my, delta);

        // Watermark top-right
        String wm = "Nova " + NovaClient.VERSION;
        int wx = this.width - RenderUtil.textWidth(wm) - 6;
        RenderUtil.text(ctx, wm, wx, 4, ACCENT, true);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        for (Panel p : panels) if (p.mouseClicked((int)mx, (int)my, btn)) return true;
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        for (Panel p : panels) if (p.dragging) { p.x += (int)dx; p.y += (int)dy; return true; }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        for (Panel p : panels) p.dragging = false;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        for (Panel p : panels) if (p.hovered((int)mx, (int)my)) { p.scroll -= (int)vScroll * ITEM_H; p.clampScroll(); return true; }
        return super.mouseScrolled(mx, my, hScroll, vScroll);
    }

    @Override
    public boolean shouldPause() { return false; }

    // ── Panel class ───────────────────────────────────────────────────────────
    private final class Panel {
        final Category cat;
        int x, y;
        boolean open    = true;
        boolean dragging = false;
        int scroll = 0;
        float animH = 0; // animated height for open/close

        // Per-module expanded state for showing settings
        final List<Boolean> expanded = new ArrayList<>();

        Panel(Category cat, int x, int y) {
            this.cat = cat;
            this.x   = x;
            this.y   = y;
            NovaClient.INSTANCE.moduleManager.getByCategory(cat).forEach(m -> expanded.add(false));
        }

        int contentHeight() {
            if (!open) return 0;
            var mods = NovaClient.INSTANCE.moduleManager.getByCategory(cat);
            int h = 0;
            for (int i = 0; i < mods.size(); i++) {
                h += ITEM_H;
                if (i < expanded.size() && expanded.get(i)) {
                    h += mods.get(i).getSettings().size() * SETTING_H + 4;
                }
            }
            return h;
        }

        boolean hovered(int mx, int my) {
            return mx >= x && mx <= x + PANEL_W && my >= y && my <= y + HEADER_H + (int)animH;
        }

        void render(DrawContext ctx, int mx, int my, float delta) {
            // Animate height
            float target = open ? contentHeight() : 0;
            animH += (target - animH) * 0.22f;
            if (Math.abs(animH - target) < 0.5f) animH = target;

            int totalH = HEADER_H + (int)animH;

            // Panel background
            RenderUtil.rect(ctx, x, y, PANEL_W, totalH, PANEL_BG);

            // Accent left border
            RenderUtil.rect(ctx, x, y, 2, totalH, cat.color);

            // Header
            RenderUtil.rect(ctx, x, y, PANEL_W, HEADER_H, HEADER_BG);
            // Category name
            RenderUtil.text(ctx, cat.name, x + PADDING + 2, y + (HEADER_H - RenderUtil.textHeight()) / 2, TEXT, false);
            // Open/close arrow
            String arrow = open ? "▼" : "▶";
            RenderUtil.text(ctx, arrow, x + PANEL_W - 14, y + (HEADER_H - RenderUtil.textHeight()) / 2, TEXT_DIM, false);

            if (animH < 2) return;

            // Clip content to panel area
            var mods = NovaClient.INSTANCE.moduleManager.getByCategory(cat);
            int cy = y + HEADER_H - scroll;

            for (int i = 0; i < mods.size(); i++) {
                Module mod = mods.get(i);
                if (cy + ITEM_H < y + HEADER_H || cy > y + totalH) { cy += ITEM_H; continue; }

                // Hover highlight
                boolean hov = mx >= x && mx <= x + PANEL_W && my >= cy && my < cy + ITEM_H;
                if (hov) RenderUtil.rect(ctx, x + 2, cy, PANEL_W - 2, ITEM_H, 0x22FFFFFF);

                // Enabled indicator
                boolean en = mod.isEnabled();
                int indicatorColor = en ? ENABLED_COL : 0x55555566;
                RenderUtil.rect(ctx, x + PANEL_W - 6, cy + (ITEM_H - 8) / 2, 4, 8, indicatorColor);

                // Module name
                int nameColor = en ? TEXT : TEXT_DIM;
                RenderUtil.text(ctx, mod.getName(), x + PADDING, cy + (ITEM_H - RenderUtil.textHeight()) / 2, nameColor, en);
                cy += ITEM_H;

                // Settings if expanded
                if (i < expanded.size() && expanded.get(i)) {
                    for (Setting<?> s : mod.getSettings()) {
                        if (cy + SETTING_H < y + HEADER_H || cy > y + totalH) { cy += SETTING_H; continue; }
                        renderSetting(ctx, s, x + 10, cy, PANEL_W - 10, mx, my);
                        cy += SETTING_H;
                    }
                    cy += 4;
                }
            }

            // Scroll bar
            int cH = contentHeight();
            if (cH > (int)animH) {
                float ratio = (float)animH / cH;
                int sbH = Math.max(16, (int)(animH * ratio));
                int sbY = y + HEADER_H + (int)((scroll / (float)(cH - animH)) * (animH - sbH));
                RenderUtil.rect(ctx, x + PANEL_W - 3, sbY, 3, sbH, SCROLL_BAR);
            }
        }

        void renderSetting(DrawContext ctx, Setting<?> s, int sx, int sy, int sw, int mx, int my) {
            RenderUtil.rect(ctx, sx, sy, sw, SETTING_H, 0x11FFFFFF);

            if (s instanceof Setting.BoolSetting bs) {
                boolean val = bs.getValue();
                int checkColor = val ? ACCENT : 0x44FFFFFF;
                RenderUtil.rect(ctx, sx + 2, sy + 3, 8, 8, checkColor);
                if (val) RenderUtil.text(ctx, "✓", sx + 3, sy + 2, 0xFFFFFFFF, false);
                RenderUtil.text(ctx, s.getName(), sx + 14, sy + (SETTING_H - RenderUtil.textHeight()) / 2, TEXT_DIM, false);

            } else if (s instanceof Setting.NumberSetting ns) {
                double val = ns.getValue(), mn = ns.getMin(), mx2 = ns.getMax();
                float pct = (float)((val - mn) / (mx2 - mn));
                int barW = sw - 4;
                RenderUtil.rect(ctx, sx + 2, sy + SETTING_H - 4, barW, 3, 0x22FFFFFF);
                RenderUtil.rect(ctx, sx + 2, sy + SETTING_H - 4, (int)(barW * pct), 3, ACCENT);
                String label = s.getName() + ": " + (ns.getInc() >= 1 ? ns.intValue() : String.format("%.2f", val));
                RenderUtil.text(ctx, label, sx + 2, sy + 1, TEXT_DIM, false);

            } else if (s instanceof Setting.ModeSetting<?> ms) {
                String label = s.getName() + ": " + ms.getValue();
                RenderUtil.text(ctx, label, sx + 2, sy + (SETTING_H - RenderUtil.textHeight()) / 2, TEXT_DIM, false);
            }
        }

        boolean mouseClicked(int mx, int my, int btn) {
            // Header click
            if (mx >= x && mx <= x + PANEL_W && my >= y && my < y + HEADER_H) {
                if (btn == 0) { open = !open; return true; }
                if (btn == 2) { dragging = true; return true; }
            }
            if (!open || animH < 2) return false;

            var mods = NovaClient.INSTANCE.moduleManager.getByCategory(cat);
            int cy = y + HEADER_H - scroll;

            for (int i = 0; i < mods.size(); i++) {
                Module mod = mods.get(i);
                if (my >= cy && my < cy + ITEM_H && mx >= x && mx <= x + PANEL_W) {
                    if (btn == 0) { mod.toggle(); return true; }
                    if (btn == 1 && i < expanded.size()) { expanded.set(i, !expanded.get(i)); return true; }
                }
                cy += ITEM_H;
                if (i < expanded.size() && expanded.get(i)) {
                    for (Setting<?> s : mod.getSettings()) {
                        if (my >= cy && my < cy + SETTING_H && mx >= x && mx <= x + PANEL_W) {
                            handleSettingClick(s, mx, cy, x + 10, PANEL_W - 10);
                            return true;
                        }
                        cy += SETTING_H;
                    }
                    cy += 4;
                }
            }
            return false;
        }

        void handleSettingClick(Setting<?> s, int mx, int cy, int sx, int sw) {
            if (s instanceof Setting.BoolSetting bs) {
                bs.toggle();
            } else if (s instanceof Setting.NumberSetting ns) {
                int barStart = sx + 2, barW = sw - 4;
                float pct = Math.max(0, Math.min(1, (float)(mx - barStart) / barW));
                ns.setValue(ns.getMin() + (ns.getMax() - ns.getMin()) * pct);
            } else if (s instanceof Setting.ModeSetting<?> ms) {
                ms.cycle();
            }
        }

        void clampScroll() {
            int maxScroll = Math.max(0, contentHeight() - (int)animH);
            scroll = Math.max(0, Math.min(scroll, maxScroll));
        }
    }
}
