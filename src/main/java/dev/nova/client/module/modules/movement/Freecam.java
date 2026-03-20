package dev.nova.client.module.modules.movement;
import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.PlayerMoveEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import net.minecraft.util.math.Vec3d;
public final class Freecam extends Module {
    private final NumberSetting speed=register(new NumberSetting("Speed","Camera speed",1.0,0.1,10,0.1));
    private Vec3d savedPos=null; private float savedYaw,savedPitch;
    public Freecam(){super("Freecam","Free camera movement",Category.MOVEMENT,-1);}
    @Override public void onEnable(){
        super.onEnable();
        if(mc.player!=null){savedPos=mc.player.getPos();savedYaw=mc.player.getYaw();savedPitch=mc.player.getPitch();}
    }
    @Override public void onDisable(){
        super.onDisable();
        if(mc.player!=null&&savedPos!=null){mc.player.setPosition(savedPos);mc.player.setYaw(savedYaw);mc.player.setPitch(savedPitch);}
    }
    @EventHandler
    public void onMove(PlayerMoveEvent e){
        if(mc.player==null) return;
        mc.player.setVelocity(Vec3d.ZERO); e.cancel();
    }
}
