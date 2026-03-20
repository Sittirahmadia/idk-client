package dev.nova.client.module.modules.combat;

import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.TickEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import java.util.Random;

public final class Hitboxes extends Module {
    private final NumberSetting expand    = register(new NumberSetting("Expand",   "Hitbox expansion",  0.08,0,0.5,0.01));
    private final BoolSetting   onlyPlays = register(new BoolSetting("Players Only","Only players",     true));
    private final BoolSetting   onlyWeap  = register(new BoolSetting("Weapon Only","Only with weapon",  false));
    private final BoolSetting   pulse     = register(new BoolSetting("Pulse",      "Toggle off/on cycle",true));
    private final NumberSetting pulseOn   = register(new NumberSetting("Pulse On",  "Ticks expanded",    8,1,20,1));
    private final NumberSetting pulseOff  = register(new NumberSetting("Pulse Off", "Ticks collapsed",   4,1,20,1));
    private final BoolSetting   randJit   = register(new BoolSetting("Jitter",      "Random variance",   true));

    private int pTick=0; private boolean pOn=true;
    private double jitter=0; private final Random rng=new Random();

    public Hitboxes(){super("Hitboxes","Expands entity hitboxes for easier hitting",Category.COMBAT,-1);}

    @EventHandler
    public void onTick(TickEvent e) {
        if (e.phase!=TickEvent.Phase.PRE) return;
        if (pulse.getValue()){
            if (++pTick>=(pOn?pulseOn.intValue():pulseOff.intValue())){pOn=!pOn;pTick=0;}
        } else pOn=true;
        jitter=randJit.getValue()?(rng.nextDouble()*2-1)*0.015:0;
    }

    public double getExpansion(Entity en) {
        if (!isEnabled()||en==mc.player) return 0;
        if (onlyPlays.getValue()&&!(en instanceof PlayerEntity)) return 0;
        if (onlyWeap.getValue()){var i=mc.player.getMainHandStack().getItem();if(!(i instanceof SwordItem||i instanceof AxeItem))return 0;}
        if (!pOn) return 0;
        return Math.max(0,expand.getValue()+jitter);
    }
}
