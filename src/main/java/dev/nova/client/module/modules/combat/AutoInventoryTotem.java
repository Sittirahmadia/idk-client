package dev.nova.client.module.modules.combat;

import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.ReceivePacketEvent;
import dev.nova.client.event.events.TickEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * AutoInventoryTotem — improved
 *
 * Fills the offhand (and optionally main hand) with totems while the
 * player inventory screen is open.
 *
 * Key improvements over original:
 *   • Totem pop (EntityStatus 35) detected via packet — bypasses open-delay
 *     and forces an immediate refill on the very next tick.
 *   • Per-session randomised open delay (min–max) breaks fixed-timing patterns.
 *   • Cached slot resolution (only re-scans when needed) = no per-tick inventory walk.
 *   • Randomised slot pick order option prevents predictable selection patterns.
 *   • Swap delay between successive swaps avoids packet flooding.
 *   • Health gate: skip acting when above a configurable HP threshold.
 *   • Auto-switch hotbar to a configured slot when inventory opens.
 *   • Hotbar fallback: if main inventory is empty, falls back to hotbar totems.
 *
 * Screen-handler slot mapping (player inventory screen, Minecraft 1.21):
 *   Slots  9–35 → main inventory rows (matches PlayerInventory.main index directly)
 *   Slots 36–44 → hotbar slots (hotbar index i → screen slot i + 36)
 *   Slot  45    → offhand
 */
public final class AutoInventoryTotem extends Module {

    // ── Settings ──────────────────────────────────────────────────────────────
    private final BoolSetting   autoSwitch  = register(new BoolSetting  ("Auto Switch",     "Switch hotbar slot when screen opens",    false));
    private final NumberSetting switchSlot  = register(new NumberSetting("Switch Slot",     "Hotbar slot to switch to on open (1-9)",  1,    1, 9,  1));
    private final NumberSetting openDelMin  = register(new NumberSetting("Open Delay Min",  "Min ticks to wait after screen opens",    0,    0, 20, 1));
    private final NumberSetting openDelMax  = register(new NumberSetting("Open Delay Max",  "Max ticks to wait after screen opens",    2,    0, 20, 1));
    private final NumberSetting swapDelay   = register(new NumberSetting("Swap Delay",      "Ticks between successive swaps",          1,    0, 10, 1));
    private final BoolSetting   forceMain   = register(new BoolSetting  ("Force Main Hand", "Also fill selected hotbar slot",          false));
    private final BoolSetting   hotbarFB    = register(new BoolSetting  ("Hotbar Fallback", "Use hotbar totems if inventory empty",    true));
    private final BoolSetting   randOrder   = register(new BoolSetting  ("Random Order",    "Randomise which slot is picked",          true));
    private final BoolSetting   healthGate  = register(new BoolSetting  ("Health Gate",     "Only act when HP below threshold",        false));
    private final NumberSetting healthThr   = register(new NumberSetting("Health Thresh",   "HP threshold for health gate",            10,   1, 20, 0.5));

    // ── State ─────────────────────────────────────────────────────────────────
    /** Set by packet thread when server confirms our totem was consumed. */
    private volatile boolean popPending   = false;
    private int    openClock    = -1;   // -1 = screen not yet tracked this session
    private int    swapClock    = 0;
    private int    resolvedOpen = 0;    // randomised once per screen-open session
    private boolean hasSwitched = false;
    /** Cached screen-handler slot for offhand fill (-1 = needs rescan). */
    private int    cachedOff    = -1;
    /** Cached screen-handler slot for main-hand fill (-1 = needs rescan). */
    private int    cachedMain   = -1;
    private boolean cacheValid  = false;

    private static final int    TOTEM_POP  = 35;
    private static final Random RNG        = new Random();

    public AutoInventoryTotem() {
        super("Auto Inventory Totem",
              "Swaps totems into offhand/main-hand while inventory is open",
              Category.COMBAT, -1);
    }

    @Override public void onEnable()  { super.onEnable();  resetSession(); }
    @Override public void onDisable() { super.onDisable(); resetSession(); }

    // ── Packet: detect totem pop ───────────────────────────────────────────────
    @EventHandler
    public void onPacket(ReceivePacketEvent event) {
        if (!(event.packet instanceof EntityStatusS2CPacket pkt)) return;
        if (mc.player == null || mc.world == null) return;
        if (pkt.getStatus() == TOTEM_POP && pkt.getEntity(mc.world) == mc.player) {
            // Offhand is now empty — invalidate cache and schedule immediate refill
            cacheValid  = false;
            popPending  = true;
        }
    }

    // ── Main tick ──────────────────────────────────────────────────────────────
    @EventHandler
    public void onTick(TickEvent event) {
        if (event.phase != TickEvent.Phase.PRE || mc.player == null) return;

        // Must be inside player inventory screen
        if (!(mc.currentScreen instanceof InventoryScreen inv)) {
            if (openClock != -1) resetSession();
            return;
        }

        // Health gate
        if (healthGate.getValue() && mc.player.getHealth() > healthThr.floatValue()) return;

        // First tick the screen is detected open this session
        if (openClock == -1) {
            openClock    = 0;
            hasSwitched  = false;
            cacheValid   = false;
            int lo = openDelMin.intValue(), hi = openDelMax.intValue();
            resolvedOpen = lo >= hi ? lo : lo + RNG.nextInt(hi - lo + 1);
        }

        // Totem pop forces immediate action — bypass all delay clocks
        boolean forcedByPop = popPending;
        popPending = false;

        if (!forcedByPop) {
            if (openClock < resolvedOpen) { openClock++; return; }
            if (swapClock > 0)           { swapClock--; return; }
        }

        // Optional one-time hotbar slot switch
        if (autoSwitch.getValue() && !hasSwitched) {
            mc.player.getInventory().selectedSlot = switchSlot.intValue() - 1;
            hasSwitched = true;
        }

        // Rebuild inventory scan cache if stale
        if (!cacheValid) { rebuildCache(); cacheValid = true; }

        var invInst = mc.player.getInventory();
        int syncId  = inv.getScreenHandler().syncId;

        // ── Priority 1: Fill offhand ───────────────────────────────────────
        if (!invInst.offHand.get(0).isOf(Items.TOTEM_OF_UNDYING) && cachedOff != -1) {
            // Button 40 = vanilla F-key "swap with offhand" — single packet, instant
            mc.interactionManager.clickSlot(syncId, cachedOff, 40, SlotActionType.SWAP, mc.player);
            cachedOff  = -1;
            cacheValid = false;          // slot changed — rescan on next need
            swapClock  = swapDelay.intValue();
            return;
        }

        // ── Priority 2: Fill main hand ─────────────────────────────────────
        if (forceMain.getValue()
                && !invInst.main.get(invInst.selectedSlot).isOf(Items.TOTEM_OF_UNDYING)
                && cachedMain != -1) {
            mc.interactionManager.clickSlot(syncId, cachedMain,
                    invInst.selectedSlot, SlotActionType.SWAP, mc.player);
            cachedMain = -1;
            cacheValid = false;
            swapClock  = swapDelay.intValue();
        }
    }

    // ── Cache builder ─────────────────────────────────────────────────────────
    /**
     * Scans the inventory once and pre-resolves screen-handler slot indices.
     *
     * Slot layout for player inventory screen handler (1.21):
     *   Screen slot 9–35  → main inventory row-by-row
     *   Screen slot 36–44 → hotbar (inventory index i → screen slot i+36)
     */
    private void rebuildCache() {
        var inv    = mc.player.getInventory();
        int selHot = inv.selectedSlot;

        List<Integer> candidates = new ArrayList<>();

        // Main inventory rows first — won't disrupt the visible hotbar
        for (int i = 9; i < 36; i++) {
            if (inv.main.get(i).isOf(Items.TOTEM_OF_UNDYING))
                candidates.add(i);
        }

        // Hotbar fallback — skip the selected slot to avoid trivial self-swap
        if (hotbarFB.getValue()) {
            for (int i = 0; i < 9; i++) {
                if (i == selHot) continue;
                if (inv.main.get(i).isOf(Items.TOTEM_OF_UNDYING))
                    candidates.add(i + 36);
            }
        }

        if (randOrder.getValue()) Collections.shuffle(candidates, RNG);

        // Offhand source = first candidate
        cachedOff = candidates.isEmpty() ? -1 : candidates.get(0);

        // Main-hand source = second candidate (or first if offhand already full)
        if (forceMain.getValue()) {
            boolean offFull = inv.offHand.get(0).isOf(Items.TOTEM_OF_UNDYING);
            cachedMain = offFull && !candidates.isEmpty() ? candidates.get(0)
                       : candidates.size() >= 2           ? candidates.get(1)
                       : -1;
        } else {
            cachedMain = -1;
        }
    }

    private void resetSession() {
        openClock = -1; swapClock = 0;
        cacheValid = false; cachedOff = -1; cachedMain = -1;
        hasSwitched = false; popPending = false;
    }
}
