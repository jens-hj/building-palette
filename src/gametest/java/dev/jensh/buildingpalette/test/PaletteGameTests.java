package dev.jensh.buildingpalette.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class PaletteGameTests {
    @GameTest public void stepsAlwaysUseMultipartPlacement(GameTestHelper test) { new PaletteGameTestScenarios().stepsAlwaysUseMultipartPlacement(test); }
    @GameTest public void placementChecksExactPieceAgainstPlayer(GameTestHelper test) { new PaletteGameTestScenarios().placementChecksExactPieceAgainstPlayer(test); }
    @GameTest public void pickKeepsExistingShapeThenReshapesHeldMaterial(GameTestHelper test) { new PaletteGameTestScenarios().pickKeepsExistingShapeThenReshapesHeldMaterial(test); }
    @GameTest public void shiftClickMergesShapesBeforeEmptySlots(GameTestHelper test) { new PaletteGameTestScenarios().shiftClickMergesShapesBeforeEmptySlots(test); }
    @GameTest public void singlePiecesUseNativeBlocks(GameTestHelper test) { new PaletteGameTestScenarios().singlePiecesUseNativeBlocks(test); }
    @GameTest public void shiftClickMergesNativeBlocksIntoPartialShapes(GameTestHelper test) { new PaletteGameTestScenarios().shiftClickMergesNativeBlocksIntoPartialShapes(test); }
    @GameTest(maxTicks = 120, skyAccess = true) public void slabRoofMatchesNativeLightBlocking(GameTestHelper test) { new PaletteGameTestScenarios().slabRoofMatchesNativeLightBlocking(test); }
    @GameTest public void everyShapeMergesByMaterial(GameTestHelper test) { new PaletteGameTestScenarios().everyShapeMergesByMaterial(test); }
    @GameTest public void nativePlacementAndPreferences(GameTestHelper test) { new PaletteGameTestScenarios().nativePlacementAndPreferences(test); }
    @GameTest public void noAirPlacementInEitherMode(GameTestHelper test) { new PaletteGameTestScenarios().noAirPlacementInEitherMode(test); }
    @GameTest public void randomizedNativeTransfers(GameTestHelper test) { new PaletteGameTestScenarios().randomizedNativeTransfers(test); }
    @GameTest public void eightCornersBecomeBlock(GameTestHelper test) { new PaletteGameTestScenarios().eightCornersBecomeBlock(test); }
    @GameTest public void connectedMining(GameTestHelper test) { new PaletteGameTestScenarios().connectedMining(test); }
    @GameTest public void nativeShapeDrops(GameTestHelper test) { new PaletteGameTestScenarios().nativeShapeDrops(test); }
    @GameTest public void repeatedCrafting(GameTestHelper test) { new PaletteGameTestScenarios().repeatedCrafting(test); }
    @GameTest public void inventoryGestures(GameTestHelper test) { new PaletteGameTestScenarios().inventoryGestures(test); }
    @GameTest public void furnaceRemainder(GameTestHelper test) { new PaletteGameTestScenarios().furnaceRemainder(test); }
    @GameTest public void materialStacks(GameTestHelper test) { new PaletteGameTestScenarios().materialStacks(test); }
    @GameTest public void remainderTransfers(GameTestHelper test) { new PaletteGameTestScenarios().remainderTransfers(test); }
    @GameTest public void multipartPersistence(GameTestHelper test) { new PaletteGameTestScenarios().multipartPersistence(test); }
}
