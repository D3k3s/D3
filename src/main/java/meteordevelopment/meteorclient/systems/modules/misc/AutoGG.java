/*
 * The MIT License
 *
 * Copyright (c) 2025 Anthony Afonin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;

public class AutoGG extends meteordevelopment.meteorclient.systems.modules.Module {

	private Optional<Entity> lastAttackedEntity = Optional.empty();

	private int sprintTimer;

	private static final int TICKS = 4;

	public AutoGG() {
		super(Categories.Misc, "Auto GG", "asdf");
	}

	@EventHandler
	private void onAttack(AttackEntityEvent event) {
		lastAttackedEntity = Optional.of(event.entity);
	}

	@EventHandler
	private void tick(TickEvent.Pre event) {

		if (sprintTimer > 0) {
			sprintTimer--;
		}

		if (sprintTimer == 1) {
			mc.player.setSprinting(true);
			System.out.println("asdf");
			String message = "Я люблю дашульку дикес " + ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
			
			ChatUtils.sendPlayerMsg(message);
			
		}

		lastAttackedEntity.ifPresent((entity) -> {
			if (!entity.isAlive()) {
				lastAttackedEntity = Optional.empty();
				mc.player.setSprinting(false);
		        double yaw = Math.toRadians(mc.player.getYaw());
		        float boost = -1f;
		        mc.player.addVelocity(-Math.sin(yaw) * boost, 0.0, -Math.cos(yaw) * boost);
				sprintTimer = TICKS;
			}
		});

	}

	@EventHandler
	private void asdf(Render2DEvent event) {
		event.drawContext.drawText(mc.textRenderer, "Я люблю дашулю D3k3s", 30, 39, 0xFFFFFFFF, true);
	}

}
