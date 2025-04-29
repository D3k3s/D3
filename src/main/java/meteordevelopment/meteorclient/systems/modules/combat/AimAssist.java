/*
 * This file is part of the D3 (https://github.com/D3k3s/D3).
 * Copyright (c) Anthony Afonin
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.*;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.*;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.color.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.*;
import net.minecraft.util.math.MathHelper;

public class AimAssist extends Module {

	private static final String NAME = "Aim-assist";

	private static final String DESCRIPTION = "Helps you aim at entities";

	private enum RotationMode {
		Packet, Camera
	}

	private SettingGroup targetSettings = settings.createGroup("Target");

	private SettingGroup rotationSettings = settings.createGroup("Rotation");

	private SettingGroup renderSettings = settings.createGroup("Render");

	private final Setting<Set<EntityType<?>>> entities = targetSettings.add(new EntityTypeListSetting.Builder()
			.name("Entities").description("Target entities").onlyAttackable().defaultValue(EntityType.PLAYER).build());

	private final Setting<SortPriority> priority = targetSettings
			.add(new EnumSetting.Builder<SortPriority>().name("Priority")
					.description("How to filter targets within range").defaultValue(SortPriority.ClosestAngle).build());

	private final Setting<Boolean> ignoreInvisible = targetSettings.add(new BoolSetting.Builder()
			.name("Ignore invisible").description("Ignore invisible entities").defaultValue(true).build());

	private final Setting<Double> lookupRadius = targetSettings.add(new DoubleSetting.Builder().name("Lookup radius")
			.description("Target find radius").range(1, 8).defaultValue(4.8).sliderRange(1, 8).build());

	private final Setting<Integer> lookupAngle = targetSettings.add(new IntSetting.Builder().name("Lookup angle")
			.description("Target lookup FOV").range(0, 180).defaultValue(50).sliderRange(0, 180).build());

	private final Setting<RotationMode> rotationMode = rotationSettings.add(
			new EnumSetting.Builder<RotationMode>().name("Rotation mode").defaultValue(RotationMode.Packet).build());

	private final Setting<Double> rotationTime = rotationSettings.add(new DoubleSetting.Builder().name("Rotation time")
			.description("How long you will rotatate to target").range(0.05, 5).sliderRange(0.05, 1.5).build());

	private final Setting<Keybind> rotationKey = rotationSettings
			.add(new KeybindSetting.Builder().name("Rotation key").defaultValue(Keybind.fromButton(0)).build());

	private final Setting<Boolean> highlightTarget = renderSettings
			.add(new BoolSetting.Builder().name("Highlight target").defaultValue(true).build());

	private final Setting<Integer> cylinderSegments = renderSettings
			.add(new IntSetting.Builder().name("Cylinder segments").range(0, 80).defaultValue(50).sliderRange(4, 100)
					.visible(highlightTarget::get).build());

	private final Setting<SettingColor> cylinderColor = renderSettings.add(new ColorSetting.Builder()
			.name("Cylinder color").defaultValue(Color.CYAN).visible(highlightTarget::get).build());

	public AimAssist() {
		super(Categories.Combat, NAME, DESCRIPTION);
	}

	private Optional<Entity> target = Optional.empty();

	@EventHandler
	private void tick(TickEvent.Pre event) {
		target = findTarget();

	}

	@EventHandler
	private void render(Render3DEvent event) {
		if (target.isPresent() && highlightTarget.get()) {
			renderTarget(event.renderer, target.get(), event.tickDelta);
		}
	}

	private void renderTarget(Renderer3D renderer, Entity entity, float tickDelta) {
		final double TWICE_PI = Math.PI * 2;

		Color color = cylinderColor.get();
		double entityX = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
		double entityY = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
		double entityZ = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());
		double radius = entity.getWidth() * 1.1;
		double height = entity.getHeight();
		double offset = height * 0.1;
		double segments = cylinderSegments.get();

		double lastX = entityX + radius;
		double lastZ = entityZ;

		for (int i = 0; i < segments; i++) {
			double x = entityX + radius * Math.cos(i * TWICE_PI / segments);
			double z = entityZ + radius * Math.sin(i * TWICE_PI / segments);

			// bottom circle
			double bottomY = entityY + offset;
			renderer.line(lastX, bottomY, lastZ, x, bottomY, z, color);

			// top circle
			double topY = entityY + height - offset;
			renderer.line(lastX, topY, lastZ, x, topY, z, color);

			// vertical line
			renderer.line(x, bottomY, z, x, topY, z, color);

			lastX = x;
			lastZ = z;
		}
	}

	private Optional<Entity> findTarget() {
		return Optional.ofNullable(TargetUtils.get(this::isValidTarget, priority.get()));
	}

	private boolean isValidTarget(Entity entity) {
		if (entity.equals(mc.player)) {
			return false;
		}

		if (!(entity instanceof LivingEntity)) {
			return false;
		}

		if (!entities.get().contains(entity.getType())) {
			return false;
		}

		if (!PlayerUtils.canSeeEntity(entity)) {
			return false;
		}

		if (entity.isInvisible() && ignoreInvisible.get()) {
			return false;
		}

		if (entity.distanceTo(mc.player) > lookupRadius.get()) {
			return false;
		}

		if (Rotations.getYawDistanceTo(entity.getPos()) > lookupAngle.get()) {
			return false;
		}

		return true;
	}
}
