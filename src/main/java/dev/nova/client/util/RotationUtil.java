package dev.nova.client.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class RotationUtil {

    /** Yaw/pitch from player eye position toward a world Vec3d. */
    public static float[] getRotations(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float yaw   = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float)-Math.toDegrees(Math.atan2(dy, dist));

        return new float[]{ MathHelper.wrapDegrees(yaw), MathHelper.clamp(pitch, -90, 90) };
    }

    /** Smooth lerp toward target rotation, respecting max speed (deg/tick). */
    public static float[] smooth(float[] current, float[] target, float speed) {
        float yaw   = (float) MathHelper.lerpAngleDegrees(speed, current[0], target[0]);
        float pitch = current[1] + MathHelper.clamp(target[1] - current[1], -speed, speed);
        return new float[]{ yaw, MathHelper.clamp(pitch, -90, 90) };
    }

    /** Angle in degrees between two rotations. */
    public static float angleDiff(float[] a, float[] b) {
        float dy = Math.abs(MathHelper.wrapDegrees(a[0] - b[0]));
        float dp = Math.abs(a[1] - b[1]);
        return (float) Math.sqrt(dy * dy + dp * dp);
    }

    /** Wrap degrees to [-180, 180]. */
    public static float wrap(float deg) { return MathHelper.wrapDegrees(deg); }
}
