package dev.jensh.buildingpalette.test;

import dev.jensh.buildingpalette.BuildingPalette;
import net.minecraft.gametest.framework.*;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@Mod("building_palette_test")
@GameTestHolder(BuildingPalette.ID)
@PrefixGameTestTemplate(false)
public final class NeoPaletteTests {
    public NeoPaletteTests() {
        if (Boolean.getBoolean("building_palette.client_test") && net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) NeoClientTest.install();
    }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void stepsalwaysusemultipartplacement(GameTestHelper test) { new PaletteGameTestScenarios().stepsAlwaysUseMultipartPlacement(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void placementchecksexactpieceagainstplayer(GameTestHelper test) { new PaletteGameTestScenarios().placementChecksExactPieceAgainstPlayer(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void pickkeepsexistingshapethenreshapesheldmaterial(GameTestHelper test) { new PaletteGameTestScenarios().pickKeepsExistingShapeThenReshapesHeldMaterial(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void pickrefillsinsufficientheldmaterial(GameTestHelper test) { new PaletteGameTestScenarios().pickRefillsInsufficientHeldMaterial(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void shiftclickmergesshapesbeforeemptyslots(GameTestHelper test) { new PaletteGameTestScenarios().shiftClickMergesShapesBeforeEmptySlots(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void singlepiecesusenativeblocks(GameTestHelper test) { new PaletteGameTestScenarios().singlePiecesUseNativeBlocks(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void offgridfenceandstepineitherorder(GameTestHelper test) { new PaletteGameTestScenarios().offgridFenceAndStepInEitherOrder(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void storagedescriptionsnevercreatematerial(GameTestHelper test) { new PaletteGameTestScenarios().storageDescriptionsNeverCreateMaterial(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void offgridsmallpiecessharecombinedblocks(GameTestHelper test) { new PaletteGameTestScenarios().offgridSmallPiecesShareCombinedBlocks(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void overlappingpiecesmerge(GameTestHelper test) { new PaletteGameTestScenarios().overlappingPiecesMerge(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void shiftclickmergesnativeblocksintopartialshapes(GameTestHelper test) { new PaletteGameTestScenarios().shiftClickMergesNativeBlocksIntoPartialShapes(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void slabroofmatchesnativelightblocking(GameTestHelper test) { new PaletteGameTestScenarios().slabRoofMatchesNativeLightBlocking(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void everyshapemergesbymaterial(GameTestHelper test) { new PaletteGameTestScenarios().everyShapeMergesByMaterial(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void nativeplacementandpreferences(GameTestHelper test) { new PaletteGameTestScenarios().nativePlacementAndPreferences(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void noairplacementineithermode(GameTestHelper test) { new PaletteGameTestScenarios().noAirPlacementInEitherMode(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void randomizednativetransfers(GameTestHelper test) { new PaletteGameTestScenarios().randomizedNativeTransfers(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void eightcornersbecomeblock(GameTestHelper test) { new PaletteGameTestScenarios().eightCornersBecomeBlock(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void connectedmining(GameTestHelper test) { new PaletteGameTestScenarios().connectedMining(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void nativeshapedrops(GameTestHelper test) { new PaletteGameTestScenarios().nativeShapeDrops(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void repeatedcrafting(GameTestHelper test) { new PaletteGameTestScenarios().repeatedCrafting(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void inventorygestures(GameTestHelper test) { new PaletteGameTestScenarios().inventoryGestures(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void furnaceremainder(GameTestHelper test) { new PaletteGameTestScenarios().furnaceRemainder(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void materialstacks(GameTestHelper test) { new PaletteGameTestScenarios().materialStacks(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void remaindertransfers(GameTestHelper test) { new PaletteGameTestScenarios().remainderTransfers(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void multipartpersistence(GameTestHelper test) { new PaletteGameTestScenarios().multipartPersistence(test); }
    @GameTest(template = "empty", timeoutTicks = 120, required = false)
    public static void networkstoreswholeblocks(GameTestHelper test) { if (!net.neoforged.fml.ModList.get().isLoaded("omnivault")) { test.succeed(); return; } new OmniVaultScenarios().networkStoresWholeBlocks(test); }
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void pickusesequippedbackpack(GameTestHelper test) { if (!net.neoforged.fml.ModList.get().isLoaded("travelersbackpack")) { test.succeed(); return; } BackpackScenarios.pickUsesEquippedBackpack(test, new NeoBackpack()); }
}
