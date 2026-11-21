package dev.jensh.buildingpalette.test;

import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import dev.jensh.buildingpalette.MultipartBlock;
import dev.jensh.buildingpalette.MultipartEntity;
import dev.jensh.buildingpalette.Placement;
import dev.jensh.buildingpalette.core.Cells;
import dev.jensh.buildingpalette.neoforge.CreateCollisions;
import dev.ryanhcode.sable.api.sublevel.KinematicContraption;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.neoforge.gametest.SableTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.fml.ModList;
import org.joml.Vector3d;

final class SableFloorTest {
    static void run(GameTestHelper test) {
        var level = test.getLevel();
        var floor = test.absolutePos(new BlockPos(2, 3, 2));
        var upper = new Cells().place(0x0c, "minecraft:glass").place(0xc0, "minecraft:blue_stained_glass");
        var lower = new Cells().place(0x03, "minecraft:glass").place(0x30, "minecraft:blue_stained_glass");
        for (var pos : BlockPos.betweenClosed(floor.offset(-1, 0, -1), floor.offset(1, 0, 1)))
            Placement.store(level, pos, upper, false);
        if (ModList.get().isLoaded("create")) CreateSnapshot.check(test, floor);
        var container = SubLevelContainer.getContainer(level);
        var body = SableTestHelper.spawnSingleBlockSubLevel(container,
            new Vector3d(floor.getX() + .5, floor.getY() + 4, floor.getZ() + .5), Blocks.STONE.defaultBlockState());
        test.runAfterDelay(50, () -> {
            test.assertTrue(Math.abs(body.boundingBox().minY() - (floor.getY() + 1)) < .06, "Falling stone rests on top of the glass floor");
            var before = level.getBlockState(floor);
            for (var pos : BlockPos.betweenClosed(floor.offset(-1, 0, -1), floor.offset(1, 0, 1)))
                ((MultipartEntity) level.getBlockEntity(pos)).setCells(lower);
            test.assertTrue(before == level.getBlockState(floor), "Moving glass changes geometry with identical vanilla state");
        });
        test.runAfterDelay(100, () -> {
            double height = body.boundingBox().minY() - floor.getY();
            SableTestHelper.removeSubLevel(container, body);
            test.assertTrue(Math.abs(height - .5) < .06, "Stone falls into the opened half and lands on the lower glass slab, height=" + height);
            test.succeed();
        });
    }

    private static final class CreateSnapshot {
        static void check(GameTestHelper test, BlockPos floor) {
            var level = test.getLevel();
            var state = level.getBlockState(floor);
            var saved = level.getBlockEntity(floor).saveWithFullMetadata(level.registryAccess());
            var contraption = new BearingContraption();
            var local = BlockPos.ZERO;
            contraption.bounds = new net.minecraft.world.phys.AABB(local);
            contraption.getBlocks().put(local, new StructureBlockInfo(local, state, saved));
            var entity = OrientedContraptionEntity.create(level, contraption, Direction.SOUTH);
            var projected = CreateCollisions.physicsState(
                (KinematicContraption) entity, level, local, state);
            test.assertValueEqual(projected.getValue(MultipartBlock.OCCLUSION), 0xcc, "Moving contraption reads glass cells in its captured block");
        }
    }
}
