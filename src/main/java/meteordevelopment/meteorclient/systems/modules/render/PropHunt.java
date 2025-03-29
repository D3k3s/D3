/*
 * This file is part of the D3 (https://github.com/D3k3s/D3).
 * Copyright (c) Anthony Afonin
 */

package meteordevelopment.meteorclient.systems.modules.render;

import java.util.*;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.*;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.*;
import net.minecraft.util.math.*;

public class PropHunt extends Module {

	private static final String NAME = "Prop-hunt";

	private static final String DESCRIPTION = "Highlights hiders in prop hunt mini game";

	private final SettingGroup renderSettings = settings.createGroup("Render");

	private final Setting<ShapeMode> shapeMode = renderSettings.add(new EnumSetting.Builder<ShapeMode>()
			.name("Shape mode").description("How the ESP are rendered").defaultValue(ShapeMode.Both).build());

	private final Setting<SettingColor> faceColor = renderSettings
			.add(new ColorSetting.Builder().name("Face color").defaultValue(new SettingColor(255, 0, 0, 70)).build());

	private final Setting<SettingColor> lineColor = renderSettings.add(
			new ColorSetting.Builder().name("Line color").defaultValue(new SettingColor(255, 255, 255, 255)).build());

	private List<FallingBlockEntity> fallingBlocks = new ArrayList<>();

	public PropHunt() {
		super(Categories.Render, NAME, DESCRIPTION);
	}

	@EventHandler
	private void tick(TickEvent.Post event) {
		fallingBlocks.clear();
		for (Entity entity : mc.world.getEntities()) {
			if (entity instanceof FallingBlockEntity fallingBlock) {
				fallingBlocks.add(fallingBlock);
			}
		}
	}

	@EventHandler
	private void render(Render3DEvent event) {
		for (FallingBlockEntity entity : fallingBlocks) {
			double x = MathHelper.lerp(event.tickDelta, entity.lastRenderX, entity.getX());
			double y = MathHelper.lerp(event.tickDelta, entity.lastRenderY, entity.getY());
			double z = MathHelper.lerp(event.tickDelta, entity.lastRenderZ, entity.getZ());

			event.renderer.box(x - 0.5, y, z - 0.5, x + 0.5, y + 1, z + 0.5, faceColor.get(), lineColor.get(),
					shapeMode.get(), 0);
		}
	}

}
