package dev.nova.client.module.modules.combat;

import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.TickEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import dev.nova.client.util.InventoryUtil;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import java.util.Comparator;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

/**
 * AutoCart
 *
 * When RMB is held:
 *  1. Places a rail on the block you are looking at (if not already a rail)
 *  2. Spawns a TNT minecart on the rail
 *  3. Waits for you to aim and draws a bow
 *  4. When you release RMB (or the cart is in the world) it fires the arrow
 *     to ignite the TNT cart
 *
 * Requires in hotbar: Rail, TNT Minecart, Bow + Arrows
 */
public final class AutoCart extends Module {

    private final NumberSetting placeDelay = register(new NumberSetting("Place Delay", "Ticks between steps", 3, 1, 20, 1));
    private final BoolSetting   autoFire   = register(new BoolSetting  ("Auto Fire",   "Auto-shoot bow at cart", true));
    private final NumberSetting fireDelay  = register(new NumberSetting("Fire Delay",  "Ticks to charge bow",   20, 5, 40, 1));
    private final NumberSetting range      = register(new NumberSetting("Range",       "Cart spawn range",       4.5, 1, 6, 0.1));
    private final BoolSetting   autoRail   = register(new BoolSetting  ("Auto Rail",   "Place rail if missing",  true));

    private enum Phase { IDLE, PLACE_RAIL, PLACE_CART, CHARGE_BOW, FIRE }
    private Phase phase = Phase.IDLE;
    private int   clock = 0;
    private int   chargeTime = 0;
    private TntMinecartEntity spawnedCart = null;
    private final Random rng = new Random();

    public AutoCart() {
        super("Auto Cart", "Places rail + TNT cart and fires bow at it", Category.COMBAT, -1);
    }

    @Override public void onEnable()  { super.onEnable();  reset(); }
    @Override public void onDisable() { super.onDisable(); reset(); }

    @EventHandler
    public void onTick(TickEvent e) {
        if (e.phase != TickEvent.Phase.PRE || mc.player == null || mc.world == null) return;

        boolean rmbHeld = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        // Start sequence when RMB pressed and crosshair on block
        if (phase == Phase.IDLE) {
            if (!rmbHeld) return;
            if (!(mc.crosshairTarget instanceof BlockHitResult bhr)) return;
            if (bhr.getType() == HitResult.Type.MISS) return;

            var state = mc.world.getBlockState(bhr.getBlockPos());
            boolean isRail = state.getBlock() instanceof AbstractRailBlock;

            if (!isRail && autoRail.getValue()) {
                phase = Phase.PLACE_RAIL;
            } else if (isRail) {
                phase = Phase.PLACE_CART;
            } else {
                return; // no rail and autoRail off
            }
            clock = 0;
            return;
        }

        if (clock++ < placeDelay.intValue()) return;
        clock = 0;

        switch (phase) {
            case PLACE_RAIL  -> doPlaceRail();
            case PLACE_CART  -> doPlaceCart();
            case CHARGE_BOW  -> doChargeBow();
            case FIRE        -> doFire();
        }
    }

    // ── Phase handlers ────────────────────────────────────────────────────────

    private void doPlaceRail() {
        if (!(mc.crosshairTarget instanceof BlockHitResult bhr)) { reset(); return; }

        if (!InventoryUtil.switchToItem(Items.RAIL)) {
            // No rail — skip to placing cart directly if surface is valid
            phase = Phase.PLACE_CART;
            return;
        }

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
        mc.player.swingHand(Hand.MAIN_HAND);
        phase = Phase.PLACE_CART;
    }

    private void doPlaceCart() {
        if (!(mc.crosshairTarget instanceof BlockHitResult bhr)) { reset(); return; }

        if (!InventoryUtil.switchToItem(Items.TNT_MINECART)) {
            reset(); return;
        }

        // Place on top face of rail block
        BlockPos railPos = bhr.getBlockPos();
        var state = mc.world.getBlockState(railPos);
        if (!(state.getBlock() instanceof AbstractRailBlock)) {
            // Try one block above if crosshair shifted
            railPos = railPos.up();
            state = mc.world.getBlockState(railPos);
            if (!(state.getBlock() instanceof AbstractRailBlock)) { reset(); return; }
        }

        Vec3d face = Vec3d.ofCenter(railPos).add(0, 0.5, 0);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                new BlockHitResult(face, Direction.UP, railPos, false));
        mc.player.swingHand(Hand.MAIN_HAND);

        // Find the cart we just placed
        spawnedCart = findNearestCart(face, range.getValue());

        if (autoFire.getValue()) {
            phase = Phase.CHARGE_BOW;
            chargeTime = 0;
        } else {
            phase = Phase.IDLE;
        }
    }

    private void doChargeBow() {
        // Switch to bow and start charging
        if (!InventoryUtil.switchToItem(Items.BOW)) { reset(); return; }

        if (chargeTime == 0) {
            // Start using (charging) bow
            mc.options.useKey.setPressed(true);
        }

        chargeTime++;

        // Face the cart if we have it
        if (spawnedCart != null && !spawnedCart.isRemoved()) {
            faceEntity(spawnedCart);
        } else {
            // Cart gone already — try aiming at last known position
        }

        if (chargeTime >= fireDelay.intValue()) {
            phase = Phase.FIRE;
        }
    }

    private void doFire() {
        mc.options.useKey.setPressed(false);

        if (mc.player.getMainHandStack().getItem() instanceof BowItem) {
            // Face cart accurately before releasing
            if (spawnedCart != null && !spawnedCart.isRemoved()) {
                faceEntity(spawnedCart);
            }
            mc.interactionManager.stopUsingItem(mc.player);
        }

        reset();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TntMinecartEntity findNearestCart(Vec3d near, double radius) {
        return mc.world.getEntitiesByClass(TntMinecartEntity.class,
                new Box(near.subtract(radius,radius,radius), near.add(radius,radius,radius)),
                c -> true)
                .stream()
                .min(Comparator.comparingDouble(c -> c.getPos().squaredDistanceTo(near)))
                .orElse(null);
    }

    private void faceEntity(Entity entity) {
        Vec3d target = entity.getPos().add(0, entity.getHeight() / 2.0, 0);
        Vec3d eye    = mc.player.getEyePos();
        double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        mc.player.setYaw((float)(MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90)
                + rng.nextGaussian() * 0.3));
        mc.player.setPitch(MathHelper.clamp((float)(-Math.toDegrees(Math.atan2(dy, dist))
                + rng.nextGaussian() * 0.3), -90, 90));
    }

    private void reset() {
        phase = Phase.IDLE;
        clock = 0;
        chargeTime = 0;
        spawnedCart = null;
        if (mc.player != null) mc.options.useKey.setPressed(false);
    }
}
