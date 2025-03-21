/*
 * This file is part of the D3 (https://github.com/D3k3s/D3).
 * Copyright (c) Anthony Afonin
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.*;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.*;
import meteordevelopment.meteorclient.utils.player.*;
import net.minecraft.entity.*;

public class AimAssist extends Module {

	private static final String NAME = "Aim-assist";

	private static final String DESCRIPTION = "Helps you aim at entities";

	private SettingGroup targetSettings = settings.createGroup("Target");

	private SettingGroup rotationSettings = settings.createGroup("Rotation");

	private final Setting<Set<EntityType<?>>> entities = targetSettings.add(new EntityTypeListSetting.Builder()
			.name("Entities").description("Target entities").onlyAttackable().defaultValue(EntityType.PLAYER).build());

	private final Setting<SortPriority> priority = targetSettings.add(
			new EnumSetting.Builder<SortPriority>().name("Priority").description("How to filter targets within range")
					.defaultValue(SortPriority.ClosestAngle).build());

	private final Setting<Boolean> ignoreInvisible = targetSettings.add(new BoolSetting.Builder()
			.name("Ignore invisible").description("Ignore invisible entities").defaultValue(true).build());

	private final Setting<Double> lookupRadius = targetSettings.add(new DoubleSetting.Builder().name("Lookup radius")
			.description("Target find radius").range(1, 8).defaultValue(4.8).sliderRange(1, 8).build());

	private final Setting<Integer> lookupAngle = targetSettings.add(new IntSetting.Builder().name("Lookup angle")
			.description("Target lookup FOV").range(0, 180).defaultValue(50).sliderRange(0, 180).build());

	public AimAssist() {
		super(Categories.Combat, NAME, DESCRIPTION);
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
		
		if(Rotations.getYawDistanceTo(entity.getPos()) > lookupAngle.get()) {
			return false;
		}

		return true;
	}
}
