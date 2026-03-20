package dev.nova.client.module.modules.render;
import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.TickEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
public final class Fullbright extends Module {
    private final NumberSetting gamma=register(new NumberSetting("Gamma","Brightness multiplier",16,1,20,0.5));
    private double savedGamma=-1;
    public Fullbright(){super("Fullbright","Removes darkness / increases gamma",Category.RENDER,-1);}
    @Override public void onEnable(){super.onEnable();if(mc.options!=null)savedGamma=mc.options.getGamma().getValue();}
    @Override public void onDisable(){super.onDisable();if(mc.options!=null&&savedGamma!=-1)mc.options.getGamma().setValue(savedGamma);}
    @EventHandler
    public void onTick(TickEvent e){if(e.phase==TickEvent.Phase.PRE&&mc.options!=null)mc.options.getGamma().setValue(gamma.getValue());}
}
