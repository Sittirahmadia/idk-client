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
 * Nova ClickGUI — Redesigned
 *
 * Layout:
 *  ┌─────────────────────────────────────────────────────────┐
 *  │  [NOVA]  Category tabs across the top                   │
 *  ├────────────┬────────────────────────────────────────────┤
 *  │  Module    │  Settings panel (right side)               │
 *  │  list      │  appears when module is right-clicked      │
 *  │  (left)    │                                            │
 *  └────────────┴────────────────────────────────────────────┘
 *
 * - Single window, no floating panels
 * - Category tabs at top
 * - Module list on left with enable toggle
 * - Settings panel slides in on right when a module is selected
 * - Right-click module → open settings, right-click again → close
 */
public final class NovaClickGUI extends Screen {

    // ── Theme ─────────────────────────────────────────────────────────────────
    private static final int C_BG        = 0xF20B0B0F;
    private static final int C_SIDEBAR   = 0xFF0F0F14;
    private static final int C_PANEL     = 0xFF14141A;
    private static final int C_HEADER    = 0xFF101016;
    private static final int C_ITEM_HOV  = 0x18FFFFFF;
    private static final int C_ITEM_EN   = 0x14B0C0FF;
    private static final int C_SEP       = 0x22FFFFFF;
    private static final int C_TEXT      = 0xFFDDDDEE;
    private static final int C_TEXT_DIM  = 0xFF666677;
    private static final int C_TEXT_EN   = 0xFFB0C0FF;
    private static final int C_ACCENT    = 0xFF5865F2;
    private static final int C_ACCENT2   = 0xFF7289DA;
    private static final int C_GREEN     = 0xFF43B581;
    private static final int C_DARK_BTN  = 0xFF1E1E28;

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final int TAB_H       = 28;
    private static final int LIST_W      = 180;
    private static final int ITEM_H      = 20;
    private static final int SETT_H      = 16;
    private static final int SETTINGS_W  = 200;
    private static final int PAD         = 8;
    private static final int CORNER      = 4;

    // ── State ─────────────────────────────────────────────────────────────────
    private Category selectedTab    = Category.COMBAT;
    private Module   selectedModule = null;
    private int      scroll         = 0;
    private int      settingsScroll = 0;

    // Mouse drag for slider
    private Setting.NumberSetting draggingSetting = null;
    private int dragSliderX = 0, dragSliderW = 0;

    public NovaClickGUI() {
        super(Text.literal("Nova"));
    }

    // ── Dimensions ────────────────────────────────────────────────────────────

    private int guiX()  { return (width  - guiW()) / 2; }
    private int guiY()  { return (height - guiH()) / 2; }
    private int guiW()  { return selectedModule != null ? LIST_W + SETTINGS_W + 1 : LIST_W; }
    private int guiH()  { return Math.min(height - 40, 380); }
    private int listX() { return guiX(); }
    private int listY() { return guiY() + TAB_H; }
    private int listH() { return guiH() - TAB_H; }
    private int settX() { return guiX() + LIST_W + 1; }
    private int settY() { return guiY() + TAB_H; }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Dim background
        ctx.fill(0, 0, width, height, 0x88000000);

        int gx = guiX(), gy = guiY(), gw = guiW(), gh = guiH();

        // ── Outer window ──────────────────────────────────────────────────────
        ctx.fill(gx, gy, gx + gw, gy + gh, C_BG);

        // Left border accent
        ctx.fill(gx, gy, gx + 2, gy + gh, selectedTab.color);

        // ── Tab bar ───────────────────────────────────────────────────────────
        renderTabs(ctx, gx, gy, gw, mx, my);

        // Separator under tabs
        ctx.fill(gx, gy + TAB_H - 1, gx + gw, gy + TAB_H, C_SEP);

        // ── Module list ───────────────────────────────────────────────────────
        renderModuleList(ctx, mx, my);

        // ── Settings panel ────────────────────────────────────────────────────
        if (selectedModule != null) {
            ctx.fill(settX() - 1, settY(), settX(), settY() + listH(), C_SEP);
            renderSettings(ctx, mx, my);
        }

        // ── Watermark ─────────────────────────────────────────────────────────
        renderWatermark(ctx, gx, gy);

        // ── Key hint ──────────────────────────────────────────────────────────
        String hint = "Left: toggle  •  Right: settings  •  Scroll: navigate";
        RenderUtil.text(ctx, hint, gx + 4, gy + gh + 3, C_TEXT_DIM, false);
    }

    private void renderWatermark(DrawContext ctx, int gx, int gy) {
        // Small pill top-left of window
        String label = "NOVA";
        int lw = RenderUtil.textWidth(label);
        int px = gx + LIST_W / 2 - lw / 2;
        int py = gy + (TAB_H - RenderUtil.textHeight()) / 2;
        RenderUtil.text(ctx, label, px, py, C_ACCENT, true);
    }

    private void renderTabs(DrawContext ctx, int gx, int gy, int gw, int mx, int my) {
        Category[] cats = Category.values();
        // Tabs start after the NOVA label area
        int tabStartX = gx + LIST_W / 2 + RenderUtil.textWidth("NOVA") / 2 + PAD;
        int tabW = (gx + gw - tabStartX - PAD) / cats.length;

        for (int i = 0; i < cats.length; i++) {
            Category cat = cats[i];
            int tx = tabStartX + i * tabW;
            int ty = gy;
            boolean sel = cat == selectedTab;
            boolean hov = mx >= tx && mx < tx + tabW && my >= ty && my < ty + TAB_H;

            if (sel) {
                ctx.fill(tx, ty + TAB_H - 3, tx + tabW - 2, ty + TAB_H, cat.color);
                RenderUtil.text(ctx, cat.name, tx + (tabW - RenderUtil.textWidth(cat.name)) / 2,
                        ty + (TAB_H - RenderUtil.textHeight()) / 2, C_TEXT, false);
            } else {
                if (hov) ctx.fill(tx, ty, tx + tabW - 2, ty + TAB_H, C_ITEM_HOV);
                RenderUtil.text(ctx, cat.name, tx + (tabW - RenderUtil.textWidth(cat.name)) / 2,
                        ty + (TAB_H - RenderUtil.textHeight()) / 2, C_TEXT_DIM, false);
            }
        }
    }

    private void renderModuleList(DrawContext ctx, int mx, int my) {
        List<Module> mods = NovaClient.INSTANCE.moduleManager.getByCategory(selectedTab);
        int x = listX(), y = listY(), w = LIST_W, h = listH();

        // Clip setup (approximate — Minecraft doesn't have real scissors here easily)
        int cy = y + 2 - scroll;

        for (int i = 0; i < mods.size(); i++) {
            Module mod = mods.get(i);
            if (cy + ITEM_H < y || cy > y + h) { cy += ITEM_H; continue; }

            boolean en  = mod.isEnabled();
            boolean hov = mx >= x && mx < x + w && my >= cy && my < cy + ITEM_H;
            boolean sel = mod == selectedModule;

            // Row background
            if (sel)       ctx.fill(x + 2, cy, x + w, cy + ITEM_H, C_ITEM_EN);
            else if (hov)  ctx.fill(x + 2, cy, x + w, cy + ITEM_H, C_ITEM_HOV);

            // Left enable indicator bar
            int barColor = en ? selectedTab.color : 0x33FFFFFF;
            ctx.fill(x + 2, cy + 3, x + 4, cy + ITEM_H - 3, barColor);

            // Module name
            int nameColor = en ? C_TEXT_EN : (hov ? C_TEXT : C_TEXT_DIM);
            RenderUtil.text(ctx, mod.getName(), x + PAD + 2, cy + (ITEM_H - RenderUtil.textHeight()) / 2, nameColor, en);

            // Right toggle pill
            int pillX = x + w - 26, pillY = cy + (ITEM_H - 8) / 2;
            ctx.fill(pillX, pillY, pillX + 20, pillY + 8, en ? C_GREEN : C_DARK_BTN);
            int dotX = en ? pillX + 12 : pillX + 2;
            ctx.fill(dotX, pillY + 1, dotX + 6, pillY + 7, 0xFFFFFFFF);

            cy += ITEM_H;
        }

        // Scrollbar
        int totalH = mods.size() * ITEM_H;
        if (totalH > h) {
            float ratio = (float) h / totalH;
            int sbH = Math.max(20, (int)(h * ratio));
            int sbY = y + (int)((scroll / (float)(totalH - h)) * (h - sbH));
            ctx.fill(x + w - 3, sbY, x + w, sbY + sbH, 0x55AAAAFF);
        }
    }

    private void renderSettings(DrawContext ctx, int mx, int my) {
        if (selectedModule == null) return;
        List<Setting<?>> settings = selectedModule.getSettings();
        int x = settX(), y = settY(), w = SETTINGS_W, h = listH();

        // Header
        ctx.fill(x, y, x + w, y + TAB_H - 4, C_HEADER);
        String title = selectedModule.getName();
        RenderUtil.text(ctx, title, x + PAD, y + (TAB_H - 4 - RenderUtil.textHeight()) / 2, C_TEXT, true);

        // Description
        String desc = selectedModule.getDescription();
        int descY = y + TAB_H - 4;
        ctx.fill(x, descY, x + w, descY + SETT_H, C_SIDEBAR);
        RenderUtil.text(ctx, desc, x + PAD, descY + (SETT_H - RenderUtil.textHeight()) / 2, C_TEXT_DIM, false);

        ctx.fill(x, descY + SETT_H, x + w, descY + SETT_H + 1, C_SEP);

        // Settings
        int cy = descY + SETT_H + 1 - settingsScroll;
        for (Setting<?> s : settings) {
            if (cy + SETT_H < y || cy > y + h) { cy += (s instanceof Setting.NumberSetting ? SETT_H + 6 : SETT_H); continue; }

            boolean hov = mx >= x && mx < x + w && my >= cy && my < cy + SETT_H;
            if (hov) ctx.fill(x, cy, x + w, cy + SETT_H, C_ITEM_HOV);

            if (s instanceof Setting.BoolSetting bs) {
                boolean val = bs.getValue();
                // Toggle switch right-side
                int sw = 26, sh = 10;
                int swX = x + w - sw - PAD, swY = cy + (SETT_H - sh) / 2;
                ctx.fill(swX, swY, swX + sw, swY + sh, val ? C_GREEN : C_DARK_BTN);
                int dotX2 = val ? swX + sw - 10 : swX + 2;
                ctx.fill(dotX2, swY + 2, dotX2 + 6, swY + sh - 2, 0xFFFFFFFF);
                RenderUtil.text(ctx, s.getName(), x + PAD, cy + (SETT_H - RenderUtil.textHeight()) / 2, C_TEXT, false);
                cy += SETT_H;

            } else if (s instanceof Setting.NumberSetting ns) {
                // Label + value
                String valStr = ns.getInc() >= 1
                        ? String.valueOf(ns.intValue())
                        : String.format("%.2f", ns.getValue());
                String label = s.getName() + "  " + valStr;
                RenderUtil.text(ctx, label, x + PAD, cy + 2, C_TEXT, false);
                // Slider bar
                int barX = x + PAD, barY = cy + SETT_H - 4, barW = w - PAD * 2, barH = 4;
                ctx.fill(barX, barY, barX + barW, barY + barH, C_DARK_BTN);
                double pct = (ns.getValue() - ns.getMin()) / (ns.getMax() - ns.getMin());
                int fillW = Math.max(4, (int)(barW * pct));
                ctx.fill(barX, barY, barX + fillW, barY + barH, C_ACCENT);
                // Slider knob
                int knobX = barX + fillW - 3;
                ctx.fill(knobX, barY - 2, knobX + 6, barY + barH + 2, 0xFFFFFFFF);
                cy += SETT_H + 6;

            } else if (s instanceof Setting.ModeSetting<?> ms) {
                String label = s.getName() + ": " + ms.getValue().toString();
                RenderUtil.text(ctx, label, x + PAD, cy + (SETT_H - RenderUtil.textHeight()) / 2, C_TEXT, false);
                String arrow = "◀ ▶";
                RenderUtil.text(ctx, arrow, x + w - RenderUtil.textWidth(arrow) - PAD,
                        cy + (SETT_H - RenderUtil.textHeight()) / 2, C_ACCENT, false);
                cy += SETT_H;
            }
        }
    }

    // ── Mouse events ──────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int imx = (int) mx, imy = (int) my;

        // Tab clicks
        handleTabClick(imx, imy, btn);

        // Module list clicks
        handleModuleClick(imx, imy, btn);

        // Settings clicks
        if (selectedModule != null) handleSettingsClick(imx, imy, btn);

        return true;
    }

    private void handleTabClick(int mx, int my, int btn) {
        if (btn != 0) return;
        Category[] cats = Category.values();
        int gx = guiX(), gy = guiY(), gw = guiW();
        int tabStartX = gx + LIST_W / 2 + RenderUtil.textWidth("NOVA") / 2 + PAD;
        int tabW = (gx + gw - tabStartX - PAD) / cats.length;

        for (int i = 0; i < cats.length; i++) {
            int tx = tabStartX + i * tabW;
            if (mx >= tx && mx < tx + tabW && my >= gy && my < gy + TAB_H) {
                selectedTab = cats[i];
                scroll = 0;
                selectedModule = null;
                return;
            }
        }
    }

    private void handleModuleClick(int mx, int my, int btn) {
        List<Module> mods = NovaClient.INSTANCE.moduleManager.getByCategory(selectedTab);
        int x = listX(), y = listY(), w = LIST_W;
        int cy = y + 2 - scroll;

        for (Module mod : mods) {
            if (my >= cy && my < cy + ITEM_H && mx >= x && mx < x + w) {
                if (btn == 0) {
                    // Check if clicking toggle pill
                    int pillX = x + w - 26;
                    if (mx >= pillX) mod.toggle();
                    else             mod.toggle(); // anywhere toggles for now
                } else if (btn == 1) {
                    // Right click = open/close settings
                    selectedModule = selectedModule == mod ? null : mod;
                    settingsScroll = 0;
                }
                return;
            }
            cy += ITEM_H;
        }
    }

    private void handleSettingsClick(int mx, int my, int btn) {
        if (selectedModule == null) return;
        List<Setting<?>> settings = selectedModule.getSettings();
        int x = settX(), y = settY(), w = SETTINGS_W;
        int cy = y + TAB_H - 4 + SETT_H + 1 - settingsScroll;

        for (Setting<?> s : settings) {
            int rowH = (s instanceof Setting.NumberSetting) ? SETT_H + 6 : SETT_H;
            if (my >= cy && my < cy + rowH && mx >= x && mx < x + w) {
                if (s instanceof Setting.BoolSetting bs && btn == 0) {
                    bs.toggle();
                } else if (s instanceof Setting.NumberSetting ns && btn == 0) {
                    int barX = x + PAD, barW = w - PAD * 2;
                    int barY = cy + SETT_H - 4;
                    if (my >= barY - 4 && my <= barY + 8) {
                        float pct = Math.max(0, Math.min(1, (float)(mx - barX) / barW));
                        ns.setValue(ns.getMin() + (ns.getMax() - ns.getMin()) * pct);
                    }
                } else if (s instanceof Setting.ModeSetting<?> ms && btn == 0) {
                    ms.cycle();
                }
                return;
            }
            cy += rowH;
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        // Drag sliders
        if (btn == 0 && selectedModule != null) {
            List<Setting<?>> settings = selectedModule.getSettings();
            int x = settX(), y = settY(), w = SETTINGS_W;
            int cy = y + TAB_H - 4 + SETT_H + 1 - settingsScroll;
            for (Setting<?> s : settings) {
                int rowH = (s instanceof Setting.NumberSetting) ? SETT_H + 6 : SETT_H;
                if (s instanceof Setting.NumberSetting ns) {
                    int barX = x + PAD, barW = w - PAD * 2;
                    float pct = Math.max(0, Math.min(1, (float)(mx - barX) / barW));
                    ns.setValue(ns.getMin() + (ns.getMax() - ns.getMin()) * pct);
                    return true;
                }
                cy += rowH;
            }
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        int imx = (int) mx, imy = (int) my;
        int x = listX(), y = listY();
        List<Module> mods = NovaClient.INSTANCE.moduleManager.getByCategory(selectedTab);
        int maxScroll = Math.max(0, mods.size() * ITEM_H - listH() + 4);

        if (selectedModule != null && imx >= settX()) {
            settingsScroll = Math.max(0, Math.min(settingsScroll - (int)(vScroll * SETT_H), 300));
        } else if (imx >= x && imx < x + LIST_W && imy >= y) {
            scroll = Math.max(0, Math.min(scroll - (int)(vScroll * ITEM_H), maxScroll));
        }
        return true;
    }

    @Override
    public boolean shouldPause() { return false; }
}
