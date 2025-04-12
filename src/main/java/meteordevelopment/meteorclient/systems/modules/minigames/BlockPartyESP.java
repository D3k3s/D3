/*
 * This file is part of the D3 (https://github.com/D3k3s/D3).
 * Copyright (c) Anthony Afonin
 */

package meteordevelopment.meteorclient.systems.modules.minigames;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class BlockPartyESP extends Module {

	private static final String NAME = "Block-party-ESP";

	private static final String DESCRIPTION = "Higlights correct blocks in block party mini game";

	private final SettingGroup plotSettings = settings.createGroup("Plot");

	private final SettingGroup renderSettings = settings.createGroup("Render");

	private final Setting<Integer> plotSize = plotSettings.add(new IntSetting.Builder().name("Plot size")
			.description("Playing field size (blocks)").range(2, 100).defaultValue(70).sliderRange(2, 100).build());

	private final Setting<Integer> plotY = plotSettings.add(new IntSetting.Builder().name("Plot Y")
			.description("Playing field Y coordinate").range(0, 320).defaultValue(70).noSlider().build());

	private final Setting<Keybind> aquirePlotY = plotSettings.add(
			new KeybindSetting.Builder().name("Aquire plot Y").description("Key bind for acquiring plot Y").build());

	private final Setting<ShapeMode> shapeMode = renderSettings.add(new EnumSetting.Builder<ShapeMode>()
			.name("shape-mode").description("How the ESP are rendered").defaultValue(ShapeMode.Both).build());

	private final Setting<SettingColor> faceColor = renderSettings
			.add(new ColorSetting.Builder().name("Face color").defaultValue(new SettingColor(255, 0, 0, 150)).build());

	private final Setting<SettingColor> lineColor = renderSettings.add(
			new ColorSetting.Builder().name("Line color").defaultValue(new SettingColor(255, 255, 255, 255)).build());

	private List<BlockPos> correctBlocks = new ArrayList<>();

	private Set<Block> lastHotbarBlocks = new HashSet<>();

	public BlockPartyESP() {
		super(Categories.MINIGAMES, NAME, DESCRIPTION);
	}

	@EventHandler
	private void render(Render3DEvent event) {
		for (BlockPos pos : correctBlocks) {
			event.renderer.sideHorizontal(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getZ() + 1,
					faceColor.get(), lineColor.get(), shapeMode.get());
		}
	}

	@EventHandler
	private void tick(TickEvent.Post event) {
		Set<Block> hotbarBlocks = getHotbarBlocks();

		if (!hotbarBlocks.equals(lastHotbarBlocks)) {
			updatePositions(hotbarBlocks);
			lastHotbarBlocks = hotbarBlocks;
		}
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

	private void updatePositions(Set<Block> blocks) {
		correctBlocks.clear();

		PlayerEntity player = mc.player;
		int y = plotY.get();
		int halfPlot = plotSize.get() / 2;

		for (int x = player.getBlockX() - halfPlot; x < player.getBlockX() + halfPlot; x++) {
			for (int z = player.getBlockZ() - halfPlot; z < player.getBlockZ() + halfPlot; z++) {
				BlockPos pos = new BlockPos(x, y, z);
				Block block = mc.world.getBlockState(pos).getBlock();

				for (Block b : blocks) {
					if (b.equals(block)) {
						correctBlocks.add(pos);
					}
				}
			}
		}
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
