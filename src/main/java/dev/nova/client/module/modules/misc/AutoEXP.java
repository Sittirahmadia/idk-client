package dev.nova.client.module.modules.misc;
import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.TickEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import java.util.Random;
public final class AutoEXP extends Module {
    private final NumberSetting delayMin=register(new NumberSetting("Delay Min","Min ticks",3,1,20,1));
    private final NumberSetting delayMax=register(new NumberSetting("Delay Max","Max ticks",7,1,20,1));
    private final NumberSetting stopLvl =register(new NumberSetting("Stop Level","Stop at this XP level",30,1,40,1));
    private int clock=0,next=5;
    private final Random rng=new Random();
    public AutoEXP(){super("Auto EXP","Throws XP bottles automatically",Category.MISC,-1);}
    @EventHandler
    public void onTick(TickEvent e){
        if(e.phase!=TickEvent.Phase.PRE||mc.player==null) return;
        if(mc.player.experienceLevel>=stopLvl.intValue()){setEnabled(false);return;}
        if(!mc.player.getMainHandStack().isOf(Items.EXPERIENCE_BOTTLE)) return;
        if(clock++<next) return;
        mc.interactionManager.interactItem(mc.player,Hand.MAIN_HAND);
        clock=0;
        int mn=delayMin.intValue(),mx=delayMax.intValue();
        next=mn>=mx?mn:mn+rng.nextInt(mx-mn+1);
    }
}
