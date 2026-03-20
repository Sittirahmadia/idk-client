package dev.nova.client.module.modules.misc;

import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.TickEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import dev.nova.client.util.InventoryUtil;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import java.util.Random;

public final class AutoEXP extends Module {
    private final NumberSetting delayMin = register(new NumberSetting("Delay Min",  "Min ticks between throws",3,  1,20,1));
    private final NumberSetting delayMax = register(new NumberSetting("Delay Max",  "Max ticks between throws", 7,  1,20,1));
    private final NumberSetting stopLvl  = register(new NumberSetting("Stop Level", "Stop at this XP level",    30, 1,40,1));
    private final BoolSetting   autoSwap = register(new BoolSetting  ("Auto Swap",  "Switch to XP bottle",      true));

    private int clock = 0, nextThrow = 5;
    private final Random rng = new Random();

    public AutoEXP() { super("Auto EXP", "Throws XP bottles automatically", Category.MISC, -1); }

    @EventHandler
    public void onTick(TickEvent e) {
        if (e.phase != TickEvent.Phase.PRE || mc.player == null) return;
        if (mc.player.experienceLevel >= stopLvl.intValue()) { setEnabled(false); return; }
        if (autoSwap.getValue() && !mc.player.getMainHandStack().isOf(Items.EXPERIENCE_BOTTLE)) {
            if (!InventoryUtil.switchToItem(Items.EXPERIENCE_BOTTLE)) return;
        } else if (!mc.player.getMainHandStack().isOf(Items.EXPERIENCE_BOTTLE)) return;

        if (clock++ < nextThrow) return;
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        clock = 0;
        int lo = delayMin.intValue(), hi = delayMax.intValue();
        nextThrow = lo >= hi ? lo : lo + rng.nextInt(hi - lo + 1);
    }
}
