package dev.nova.client.module.modules.misc;
import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.ReceivePacketEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
public final class AntiCrash extends Module {
    public AntiCrash(){super("Anti Crash","Blocks crash-exploit packets",Category.MISC,-1);}
    @EventHandler
    public void onPacket(ReceivePacketEvent e){
        if(e.packet instanceof ExplosionS2CPacket p){
            float pw=p.getPower();
            if(pw>100||Float.isNaN(pw)||Float.isInfinite(pw)) e.cancel();
        }
    }
}
