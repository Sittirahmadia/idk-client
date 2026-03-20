package dev.nova.client.module.modules.combat;

import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.TickEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import java.util.Random;

public final class TriggerBot extends Module {
    private final NumberSetting cooldown  = register(new NumberSetting("Cooldown %","Attack at this cooldown%",95,50,100,1));
    private final NumberSetting jitter    = register(new NumberSetting("Jitter",    "±% cooldown noise",       3, 0, 10, 1));
    private final NumberSetting reactMin  = register(new NumberSetting("React Min", "Min reaction ms",         50,0, 400,1));
    private final NumberSetting reactMax  = register(new NumberSetting("React Max", "Max reaction ms",         150,0,400,1));
    private final BoolSetting   needWeap  = register(new BoolSetting("Need Weapon","Only with sword/axe/tri", true));
    private final BoolSetting   noFriends = register(new BoolSetting("No Friends", "Skip friends",            true));

    private long reactStart=0,reactTarget=0; boolean waiting=false;
    private LivingEntity lastTarget=null; float jOff=0;
    private final Random rng=new Random();

    public TriggerBot(){super("TriggerBot","Auto-attack entity on crosshair",Category.COMBAT,-1);}

    @EventHandler
    public void onTick(TickEvent e) {
        if (e.phase!=TickEvent.Phase.PRE||mc.player==null||mc.currentScreen!=null) return;
        if (needWeap.getValue()){var i=mc.player.getMainHandStack().getItem();if(!(i instanceof SwordItem||i instanceof AxeItem||i instanceof TridentItem))return;}
        if (!(mc.crosshairTarget instanceof EntityHitResult ehr)) return;
        if (!(ehr.getEntity() instanceof LivingEntity target)||!target.isAlive()) return;
        if (target instanceof PlayerEntity p && noFriends.getValue()) {/* friend check placeholder */}
        long now=System.currentTimeMillis();
        if (target!=lastTarget){lastTarget=target;waiting=false;}
        if (!waiting){
            long min=(long)reactMin.getValue(),max=(long)reactMax.getValue();
            reactTarget=min>=max?min:min+(long)(rng.nextGaussian()*(max-min)/4.0+((min+max)/2.0)-min);
            reactTarget=Math.max(min,Math.min(max,reactTarget));
            reactStart=now; jOff=(float)((rng.nextDouble()*2-1)*jitter.getValue()); waiting=true; return;
        }
        if (now-reactStart<reactTarget) return;
        float thresh=Math.max(0.05f,Math.min(1f,(float)((cooldown.getValue()+jOff)/100.0)));
        if (mc.player.getAttackCooldownProgress(0f)<thresh) return;
        mc.interactionManager.attackEntity(mc.player,target);
        mc.player.swingHand(Hand.MAIN_HAND);
        waiting=false; jOff=0;
    }
}
