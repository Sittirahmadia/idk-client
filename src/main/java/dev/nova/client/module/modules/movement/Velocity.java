package dev.nova.client.module.modules.movement;
import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.ReceivePacketEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
public final class Velocity extends Module {
    private final NumberSetting horizonal=register(new NumberSetting("Horizontal","Horizontal multiplier %",0,0,100,1));
    private final NumberSetting vertical =register(new NumberSetting("Vertical",  "Vertical multiplier %",  100,0,200,1));
    public Velocity(){super("Velocity","Modifies knockback velocity",Category.MOVEMENT,-1);}
    @EventHandler
    public void onPacket(ReceivePacketEvent e){
        if(mc.player==null) return;
        if(e.packet instanceof EntityVelocityUpdateS2CPacket p&&p.getEntityId()==mc.player.getId()) e.cancel();
    }
}
