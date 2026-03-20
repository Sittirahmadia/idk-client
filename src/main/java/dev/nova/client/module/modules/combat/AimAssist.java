package dev.nova.client.module.modules.combat;

import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.RenderHudEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import java.util.Random;

public final class AimAssist extends Module {
    private final NumberSetting smoothing  = register(new NumberSetting("Smoothing",   "Lerp speed (low=human)",0.12,0.01,1,0.01));
    private final NumberSetting fov        = register(new NumberSetting("FOV",         "Target FOV",            50,  0,180,  1));
    private final NumberSetting range      = register(new NumberSetting("Range",       "Max range",             5,   0.1,10,0.1));
    private final NumberSetting noise      = register(new NumberSetting("Noise",       "Micro-jitter amount",   0.06,0,  3, 0.01));
    private final NumberSetting aimSpeed   = register(new NumberSetting("Aim Speed",   "Max deg/tick clamp",    6,   0.5,30,0.5));
    private final BoolSetting   weaponOnly = register(new BoolSetting ("Weapon Only",  "Only sword/axe",       false));
    private final BoolSetting   sticky     = register(new BoolSetting ("Sticky",       "Lock onto target",     true));
    private final BoolSetting   losCheck   = register(new BoolSetting ("LoS Check",    "Line of sight check",  true));
    private final BoolSetting   crystals   = register(new BoolSetting ("Crystals",     "Also target crystals", false));

    private Entity target; double noiseY = 0, noiseP = 0;
    private final Random rng = new Random();

    public AimAssist() { super("Aim Assist", "Smooth human-like aim assistance", Category.COMBAT, -1); }

    @EventHandler
    public void onRender(RenderHudEvent e) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) return;
        if (weaponOnly.getValue()) {
            var item = mc.player.getMainHandStack().getItem();
            if (!(item instanceof SwordItem) && !(item instanceof AxeItem)) return;
        }
        float pYaw = mc.player.getYaw(), pPitch = mc.player.getPitch();
        if (target != null && (!target.isAlive()
                || mc.player.distanceTo(target) > range.getValue()
                || !inFov(pYaw, pPitch, aimPos(target))))
            target = null;
        if (!sticky.getValue() || target == null) target = findTarget(pYaw, pPitch);
        if (target == null) return;

        Vec3d aim = aimPos(target);
        if (losCheck.getValue()) {
            var ray = mc.world.raycast(new RaycastContext(mc.player.getCameraPosVec(e.delta), aim,
                    RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.ANY, mc.player));
            if (ray.getType() == HitResult.Type.BLOCK) return;
        }
        Vec3d eyes = mc.player.getEyePos();
        double dx=aim.x-eyes.x, dy=aim.y-eyes.y, dz=aim.z-eyes.z, dist=Math.sqrt(dx*dx+dz*dz);
        float nYaw   = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz,dx))-90);
        float nPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        float s = smoothing.floatValue();
        float maxSpd = aimSpeed.floatValue();
        double noiseAmt = noise.getValue();
        noiseY = noiseY * 0.82 + rng.nextGaussian() * noiseAmt * 0.18;
        noiseP = noiseP * 0.82 + rng.nextGaussian() * noiseAmt * 0.18;
        // Clamp rotation speed
        float rawYaw   = (float) MathHelper.lerpAngleDegrees(s, pYaw,   nYaw);
        float rawPitch = (float) MathHelper.lerpAngleDegrees(s, pPitch, nPitch);
        float dY = MathHelper.clamp(MathHelper.wrapDegrees(rawYaw   - pYaw),   -maxSpd, maxSpd);
        float dP = MathHelper.clamp(                        rawPitch - pPitch,  -maxSpd, maxSpd);
        mc.player.setYaw((float)(pYaw   + dY + noiseY));
        mc.player.setPitch(MathHelper.clamp((float)(pPitch + dP + noiseP), -90, 90));
    }

    private Entity findTarget(float pY, float pP) {
        Entity best = null; double bestSq = Double.MAX_VALUE;
        for (Entity en : mc.world.getEntities()) {
            boolean valid = (en instanceof PlayerEntity && en != mc.player)
                    || (crystals.getValue() && en instanceof EndCrystalEntity);
            if (!valid || !en.isAlive()) continue;
            Vec3d pos = aimPos(en);
            double sq = mc.player.squaredDistanceTo(pos.x, pos.y, pos.z);
            if (sq < bestSq && Math.sqrt(sq) <= range.getValue() && inFov(pY, pP, pos))
                { best = en; bestSq = sq; }
        }
        return best;
    }

    private Vec3d aimPos(Entity e) { return e.getEyePos(); }

    private boolean inFov(float pY, float pP, Vec3d t) {
        Vec3d eyes = mc.player.getEyePos();
        double dx=t.x-eyes.x, dy=t.y-eyes.y, dz=t.z-eyes.z;
        double dist = Math.sqrt(dx*dx+dz*dz);
        double nY = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz,dx))-90);
        double nP = -Math.toDegrees(Math.atan2(dy,dist));
        double f = fov.getValue();
        return Math.abs(MathHelper.wrapDegrees((float)(pY-nY))) <= f && Math.abs(pP-nP) <= f;
    }
}
