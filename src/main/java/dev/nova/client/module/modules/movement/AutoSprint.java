package dev.nova.client.module.modules.movement;
import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.TickEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
public final class AutoSprint extends Module {
    private final BoolSetting omni=register(new BoolSetting("Omni-Sprint","Sprint in all directions",true));
    public AutoSprint(){super("Auto Sprint","Always sprint without holding the key",Category.MOVEMENT,-1);}
    @EventHandler
    public void onTick(TickEvent e){
        if(e.phase!=TickEvent.Phase.PRE||mc.player==null) return;
        if(mc.player.forwardSpeed!=0||(omni.getValue()&&(mc.player.sidewaysSpeed!=0||mc.player.forwardSpeed!=0)))
            mc.player.setSprinting(true);
    }
}
