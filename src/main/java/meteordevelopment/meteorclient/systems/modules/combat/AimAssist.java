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
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.*;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.color.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;

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

	private final Setting<Double> yawSpeed = rotationSettings.add(new DoubleSetting.Builder().name("Yaw speed")
			.description("yaw speed").range(0, 2480).sliderRange(0.0, 2480).build());

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

	private double aimTimer;

	@EventHandler
	private void tick(TickEvent.Pre event) {
		target = findTarget();

		target.ifPresentOrElse((entity) -> {
			if (rotationKey.get().isPressed()) {
				aimTimer = rotationTime.get();
			}
		}, () -> {
			aimTimer = 0;
		});
		
	}
	
	@Override
	public void onDeactivate() {
		aimTimer = 0;
		target = Optional.empty();
	}

	@EventHandler
	private void render(Render3DEvent event) {
		if (target.isEmpty()) {
			return;
		}

		Entity targetEntity = target.get();

		if (highlightTarget.get()) {
			renderTarget(event.renderer, targetEntity, event.tickDelta);
		}

		if (aimTimer > 0 && mc.currentScreen == null) {
			double delta = event.frameTime;
			double playerYaw = mc.player.getYaw();
			double targetYaw = Rotations.getYaw(targetEntity);

			double deltaAngle = ((targetYaw - playerYaw + 540) % 360) - 180;

			// Interpolate with a smoothing factor
			double maxRotation = yawSpeed.get() * delta; // Max degrees per second scaled by delta time
			double newYaw = playerYaw + Math.max(-maxRotation, Math.min(maxRotation, deltaAngle));

			mc.player.setYaw(MathHelper.lerp(event.tickDelta, mc.player.prevYaw, (float) newYaw));

			aimTimer -= delta;
		}

	}

	private void renderTarget(Renderer3D renderer, Entity entity, float tickDelta) {
		final double TWICE_PI = Math.PI * 2;
		double entityX = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
		double entityY = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
		double entityZ = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());
		double radius = entity.getWidth() * 1.1;
		double height = entity.getHeight();
		double offset = height * 0.1;
		double segments = cylinderSegments.get();

		Color color = cylinderColor.get();

		// damage animation
	//	if (entity instanceof LivingEntity living && living.maxHurtTime > 0) {
	//		float animationProgress = (float) living.hurtTime / living.maxHurtTime;
	//		color.a = (int) (1 - (color.a * animationProgress));
	//	}

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
		
		if(entity instanceof PlayerEntity player && Friends.get().isFriend(player)) {
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
