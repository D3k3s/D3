/*
 * This file is part of the D3 (https://github.com/D3k3s/D3).
 * Copyright (c) Anthony Afonin
 */

package meteordevelopment.meteorclient.systems.modules.minigames;

import java.util.*;

import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.*;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import net.minecraft.text.Text;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;

public class BlockPartyAssistant extends Module {

	private static final String NAME = "Block-party-assistant";

	private static final String DESCRIPTION = "Helps you in block party mini game";

	private enum RotationMode {
		NONE, STRAIGHT
	}

	private final SettingGroup plotSettings = settings.createGroup("Plot");

	private final SettingGroup renderSettings = settings.createGroup("Render");

	private final SettingGroup rotationSettings = settings.createGroup("Rotation");

	private final SettingGroup movementSettings = settings.createGroup("Movement");

	private final Setting<Integer> plotSize = plotSettings.add(new IntSetting.Builder().name("Plot size")
			.description("Playing field size (blocks)").range(2, 100).defaultValue(70).sliderRange(2, 100).build());

	private final Setting<Integer> plotY = plotSettings.add(new IntSetting.Builder().name("Plot Y")
			.description("Playing field Y coordinate").range(0, 320).defaultValue(59).noSlider().build());

	private final Setting<Keybind> aquirePlotY = plotSettings.add(
			new KeybindSetting.Builder().name("Aquire plot Y").description("Key bind for acquiring plot Y").build());

	private final Setting<Boolean> highlightCorrectBlocks = renderSettings
			.add(new BoolSetting.Builder().name("Highlight correct blocks").defaultValue(true).build());

	private final Setting<ShapeMode> correctBlocksShapeMode = renderSettings.add(new EnumSetting.Builder<ShapeMode>()
			.name("Correct blocks shape mode").description("How the correct blocks ESP are rendered")
			.defaultValue(ShapeMode.Both).visible(highlightCorrectBlocks::get).build());

	private final Setting<SettingColor> correctBlocksFaceColor = renderSettings
			.add(new ColorSetting.Builder().name("Correct blocks ESP face color")
					.defaultValue(new SettingColor(255, 0, 135, 255)).visible(highlightCorrectBlocks::get).build());

	private final Setting<SettingColor> correctBlocksLineColor = renderSettings
			.add(new ColorSetting.Builder().name("Correct blocks ESP line color")
					.defaultValue(new SettingColor(255, 255, 255, 255)).visible(highlightCorrectBlocks::get).build());

	private final Setting<Boolean> highlightTargetBlock = renderSettings
			.add(new BoolSetting.Builder().name("Highlight target block").defaultValue(true).build());

	private final Setting<ShapeMode> targetBlockShapeMode = renderSettings.add(new EnumSetting.Builder<ShapeMode>()
			.name("Target block shape mode").description("How the target ESP are rendered").defaultValue(ShapeMode.Both)
			.visible(highlightTargetBlock::get).build());

	private final Setting<SettingColor> targetBlockFaceColor = renderSettings
			.add(new ColorSetting.Builder().name("Target block ESP face color")
					.defaultValue(new SettingColor(255, 0, 0, 150)).visible(highlightTargetBlock::get).build());

	private final Setting<SettingColor> targetBlockLineColor = renderSettings
			.add(new ColorSetting.Builder().name("Target block line color")
					.defaultValue(new SettingColor(255, 255, 255, 255)).visible(highlightTargetBlock::get).build());

	private final Setting<RotationMode> rotationMode = rotationSettings.add(new EnumSetting.Builder<RotationMode>()
			.name("Mode").description("Determines how the player will turn towards the target block")
			.defaultValue(RotationMode.NONE).build());

	private final Setting<Boolean> autoMove = movementSettings.add(new BoolSetting.Builder().name("Automatic movement")
			.description("Automatic runs to target block").defaultValue(false).build());

	private List<BlockPos> correctBlocksPositions = new ArrayList<>();

	private Optional<BlockPos> targetPos = Optional.empty();

	private Set<Block> lastHotbarBlocks = new HashSet<>();

	private CustomPlayerInput dummyInput = new CustomPlayerInput();

	public BlockPartyAssistant() {
		super(Categories.MINIGAMES, NAME, DESCRIPTION);
	}

	private boolean isPlayerInSafePos() {
		for (BlockPos pos : correctBlocksPositions) {
			if (pos.getX() == mc.player.getBlockX() && pos.getZ() == mc.player.getBlockZ()) {
				return true;
			}
		}

		return false;
	}

	@EventHandler
	private void render(Render3DEvent event) {
		if (highlightCorrectBlocks.get()) {
			correctBlocksPositions.forEach(pos -> event.renderer.sideHorizontal(pos.getX(), pos.getY() + 1, pos.getZ(),
					pos.getX() + 1, pos.getZ() + 1, correctBlocksFaceColor.get(), correctBlocksLineColor.get(),
					correctBlocksShapeMode.get()));
		}

		if (highlightTargetBlock.get()) {
			targetPos.ifPresent((pos) -> {
				event.renderer.sideHorizontal(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getZ() + 1,
						targetBlockFaceColor.get(), targetBlockLineColor.get(), targetBlockShapeMode.get());
			});
		}
	}

	private void updateAutomaticMovement(BlockPos target) {
		mc.player.input = autoMove.get() ? dummyInput : new KeyboardInput(mc.options);
		
		if (!isPlayerInSafePos()) {
			dummyInput.jump(mc.player.getBlockPos().getSquaredDistance(target) > 3);
			dummyInput.sprint(true);
			dummyInput.forward(true);
		} else {
			dummyInput.sprint(false);
			dummyInput.forward(false);
		}
	}

	@EventHandler
	private void tick(TickEvent.Post event) {
		targetPos.ifPresent(pos -> {
			if (rotationMode.get() == RotationMode.STRAIGHT) {
				mc.player.setYaw((float) Rotations.getYaw(pos));
			}
			
			updateAutomaticMovement(pos);
		});
		

		Set<Block> hotbarBlocks = getHotbarBlocks();

		if (hotbarBlocks.equals(lastHotbarBlocks)) {
			return;
		}

		updatePositions(hotbarBlocks);
		lastHotbarBlocks = hotbarBlocks;

		
	}

	@EventHandler
	private void handleKey(KeyEvent event) {
		if (mc.currentScreen == null && event.key == aquirePlotY.get().getValue() && event.action == KeyAction.Press) {
			if (mc.player.isOnGround()) {
				int y = mc.player.getBlockY() - 1;
				if (plotY.set(y)) {
					ChatUtils.sendMsg(title, Text.of("Plot Y coordinate is set to " + y));
				} else {
					ChatUtils.sendMsg(title, Text.of("Plot Y coordinate is out of range"));
				}
			} else {
				ChatUtils.sendMsg(title, Text.of("You must stay on the ground"));
			}

		}

	}

	@Override
	public void onActivate() {
		lastHotbarBlocks.clear();
	}
	
	@Override
	public void onDeactivate() {
		mc.player.input = new KeyboardInput(mc.options);
	}

	private void updatePositions(Set<Block> blocks) {
		correctBlocksPositions.clear();

		PlayerEntity player = mc.player;
		int y = plotY.get();
		int halfPlot = plotSize.get() / 2;
		BlockPos closest = BlockPos.ORIGIN;

		for (int x = player.getBlockX() - halfPlot; x < player.getBlockX() + halfPlot; x++) {
			for (int z = player.getBlockZ() - halfPlot; z < player.getBlockZ() + halfPlot; z++) {
				BlockPos pos = new BlockPos(x, y, z);
				Block block = mc.world.getBlockState(pos).getBlock();

				for (Block b : blocks) {
					if (b.equals(block)) {
						correctBlocksPositions.add(pos);
						if (pos.getSquaredDistance(player.getPos()) < closest.getSquaredDistance(player.getPos())) {
							closest = pos;
						}
					}
				}
			}
		}

		targetPos = Optional.ofNullable(closest == BlockPos.ORIGIN ? null : closest);

	}

	private Set<Block> getHotbarBlocks() {
		Set<Block> blocks = new HashSet<>();
		PlayerInventory inventory = mc.player.getInventory();

		for (int i = 0; i < PlayerInventory.HOTBAR_SIZE; i++) {
			Item item = inventory.getStack(i).getItem();
			if (item instanceof BlockItem block) {
				blocks.add(block.getBlock());
			}
		}

		return blocks;
	}

}
