package dev.nova.client.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public final class BlockUtil {

    private static final MinecraftClient MC = MinecraftClient.getInstance();

    public static boolean isAir(BlockPos pos)            { return MC.world.isAir(pos); }
    public static boolean isSolid(BlockPos pos)          { return !MC.world.getBlockState(pos).isAir(); }
    public static boolean isFullBlock(BlockPos pos)      { var s=MC.world.getBlockState(pos); return !s.isAir()&&s.isSolidBlock(MC.world,pos); }

    public static boolean isObsidianOrBedrock(BlockPos pos) {
        Block b = MC.world.getBlockState(pos).getBlock();
        return b == Blocks.OBSIDIAN || b == Blocks.BEDROCK;
    }

    public static boolean isHardBlock(BlockPos pos) {
        Block b = MC.world.getBlockState(pos).getBlock();
        return b==Blocks.OBSIDIAN||b==Blocks.BEDROCK||b==Blocks.ENDER_CHEST||b==Blocks.CRYING_OBSIDIAN;
    }

    /** Can a crystal be placed on top of base? */
    public static boolean canPlaceCrystal(BlockPos base) {
        if (!isObsidianOrBedrock(base)) return false;
        BlockPos up=base.up(), up2=up.up();
        if (!isAir(up)||!isAir(up2)) return false;
        Box box=new Box(up.getX(),up.getY(),up.getZ(),up.getX()+1,up.getY()+2,up.getZ()+1);
        return MC.world.getOtherEntities(null,box).isEmpty();
    }

    /** True if pos is a bedrock/obsidian 1x1 hole safe to stand in. */
    public static boolean isSafeHole(BlockPos pos) {
        if (!isAir(pos)||!isAir(pos.up())) return false;
        for (Direction d:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST}) {
            Block s=MC.world.getBlockState(pos.offset(d)).getBlock();
            if (s!=Blocks.BEDROCK&&s!=Blocks.OBSIDIAN) return false;
        }
        Block below=MC.world.getBlockState(pos.down()).getBlock();
        return below==Blocks.BEDROCK||below==Blocks.OBSIDIAN;
    }

    /** 4-side horizontal neighbors. */
    public static List<BlockPos> sideNeighbors(BlockPos p) {
        return List.of(p.north(),p.south(),p.east(),p.west());
    }

    /** Best face+hitVec to use when placing a block at target. Returns null if no viable face. */
    public static PlacePair findPlaceSide(BlockPos target) {
        for (Direction dir : Direction.values()) {
            BlockPos nb = target.offset(dir);
            BlockState st = MC.world.getBlockState(nb);
            if (!st.isAir()&&st.isSolidBlock(MC.world,nb)) {
                Direction face = dir.getOpposite();
                Vec3d hit = Vec3d.ofCenter(nb).add(face.getOffsetX()*0.5, face.getOffsetY()*0.5, face.getOffsetZ()*0.5);
                return new PlacePair(nb, face, hit);
            }
        }
        return null;
    }

    public record PlacePair(BlockPos pos, Direction face, Vec3d hitVec) {}

    public static boolean inReach(BlockPos pos, double reach) {
        return MC.player!=null && MC.player.getEyePos().distanceTo(Vec3d.ofCenter(pos))<=reach;
    }

    public static boolean valuableLootNearby(BlockPos origin, double r) {
        if (MC.world==null) return false;
        Box box=new Box(origin.getX()-r,origin.getY()-r,origin.getZ()-r,origin.getX()+r,origin.getY()+r,origin.getZ()+r);
        for (Entity e:MC.world.getOtherEntities(null,box)) {
            if (!(e instanceof ItemEntity ie)) continue;
            var it=ie.getStack().getItem();
            if (it==Items.TOTEM_OF_UNDYING||it==Items.ELYTRA) return true;
        }
        return false;
    }
}
