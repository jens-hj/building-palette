package dev.jensh.buildingpalette.test;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.*;

/** Listed under the fabric-gametest entrypoint; scenarios are shared with NeoForge. */
public final class FabricPaletteTests {
    private static final String EMPTY = "building_palette:empty";
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void stepsAlwaysUseMultipartPlacement(GameTestHelper test) { new PaletteGameTestScenarios().stepsAlwaysUseMultipartPlacement(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void placementChecksExactPieceAgainstPlayer(GameTestHelper test) { new PaletteGameTestScenarios().placementChecksExactPieceAgainstPlayer(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void pickKeepsExistingShapeThenReshapesHeldMaterial(GameTestHelper test) { new PaletteGameTestScenarios().pickKeepsExistingShapeThenReshapesHeldMaterial(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void pickRefillsInsufficientHeldMaterial(GameTestHelper test) { new PaletteGameTestScenarios().pickRefillsInsufficientHeldMaterial(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void shiftClickMergesShapesBeforeEmptySlots(GameTestHelper test) { new PaletteGameTestScenarios().shiftClickMergesShapesBeforeEmptySlots(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void singlePiecesUseNativeBlocks(GameTestHelper test) { new PaletteGameTestScenarios().singlePiecesUseNativeBlocks(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void offgridFenceAndStepInEitherOrder(GameTestHelper test) { new PaletteGameTestScenarios().offgridFenceAndStepInEitherOrder(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void storageDescriptionsNeverCreateMaterial(GameTestHelper test) { new PaletteGameTestScenarios().storageDescriptionsNeverCreateMaterial(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void offgridSmallPiecesShareCombinedBlocks(GameTestHelper test) { new PaletteGameTestScenarios().offgridSmallPiecesShareCombinedBlocks(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void overlappingPiecesMerge(GameTestHelper test) { new PaletteGameTestScenarios().overlappingPiecesMerge(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void shiftClickMergesNativeBlocksIntoPartialShapes(GameTestHelper test) { new PaletteGameTestScenarios().shiftClickMergesNativeBlocksIntoPartialShapes(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void slabRoofMatchesNativeLightBlocking(GameTestHelper test) { new PaletteGameTestScenarios().slabRoofMatchesNativeLightBlocking(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void everyShapeMergesByMaterial(GameTestHelper test) { new PaletteGameTestScenarios().everyShapeMergesByMaterial(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void nativePlacementAndPreferences(GameTestHelper test) { new PaletteGameTestScenarios().nativePlacementAndPreferences(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void noAirPlacementInEitherMode(GameTestHelper test) { new PaletteGameTestScenarios().noAirPlacementInEitherMode(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void randomizedNativeTransfers(GameTestHelper test) { new PaletteGameTestScenarios().randomizedNativeTransfers(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void eightCornersBecomeBlock(GameTestHelper test) { new PaletteGameTestScenarios().eightCornersBecomeBlock(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void connectedMining(GameTestHelper test) { new PaletteGameTestScenarios().connectedMining(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void nativeShapeDrops(GameTestHelper test) { new PaletteGameTestScenarios().nativeShapeDrops(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void repeatedCrafting(GameTestHelper test) { new PaletteGameTestScenarios().repeatedCrafting(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void inventoryGestures(GameTestHelper test) { new PaletteGameTestScenarios().inventoryGestures(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void furnaceRemainder(GameTestHelper test) { new PaletteGameTestScenarios().furnaceRemainder(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void materialStacks(GameTestHelper test) { new PaletteGameTestScenarios().materialStacks(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void remainderTransfers(GameTestHelper test) { new PaletteGameTestScenarios().remainderTransfers(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void multipartPersistence(GameTestHelper test) { new PaletteGameTestScenarios().multipartPersistence(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120, required = false)
    public void networkStoresWholeBlocks(GameTestHelper test) { if (!FabricLoader.getInstance().isModLoaded("omnivault")) { test.succeed(); return; } new OmniVaultScenarios().networkStoresWholeBlocks(test); }
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public void pickUsesEquippedBackpack(GameTestHelper test) { if (!FabricLoader.getInstance().isModLoaded("travelersbackpack")) { test.succeed(); return; } BackpackScenarios.pickUsesEquippedBackpack(test, new FabricBackpack()); }
}
