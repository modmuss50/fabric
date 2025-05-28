/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.impl.client.rendering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

public class HudElementRegistryImpl {
	private static final List<HudElement> FIRST_ELEMENTS = new ArrayList<>();
	private static final Map<Identifier, List<HudElement>> BEFORE_ELEMENTS = new IdentityHashMap<>();
	private static final Map<Identifier, List<HudElement>> AFTER_ELEMENTS = new IdentityHashMap<>();
	private static final List<HudElement> LAST_ELEMENTS = new ArrayList<>();
	private static final Set<Identifier> VANILLA_IDS = Util.make(Collections.newSetFromMap(new IdentityHashMap<>()), ids -> ids.addAll(List.of(VanillaHudElements.MISC_OVERLAYS,
			VanillaHudElements.CROSSHAIR,
			VanillaHudElements.HOTBAR_AND_BARS,
			VanillaHudElements.STATUS_EFFECTS,
			VanillaHudElements.BOSS_BAR,
			VanillaHudElements.SLEEP,
			VanillaHudElements.DEMO_TIMER,
			VanillaHudElements.DEBUG,
			VanillaHudElements.SCOREBOARD,
			VanillaHudElements.OVERLAY_MESSAGE,
			VanillaHudElements.TITLE_AND_SUBTITLE,
			VanillaHudElements.CHAT,
			VanillaHudElements.PLAYER_LIST,
			VanillaHudElements.SUBTITLES)));

	public static void renderFirst(DrawContext context, RenderTickCounter tickCounter) {
		for (HudElement element : FIRST_ELEMENTS) {
			element.render(context, tickCounter);
		}
	}

	public static void renderVanilla(Identifier id, InGameHud instance, DrawContext context, RenderTickCounter tickCounter, Operation<Void> renderVanilla) {
		List<HudElement> before = BEFORE_ELEMENTS.get(id);
		List<HudElement> after = AFTER_ELEMENTS.get(id);

		if (before != null) {
			for (HudElement element : before) {
				element.render(context, tickCounter);
			}
		}

		renderVanilla.call(instance, context, tickCounter);

		if (after != null) {
			for (HudElement element : after) {
				element.render(context, tickCounter);
			}
		}
	}

	public static void renderLast(DrawContext context, RenderTickCounter tickCounter) {
		for (HudElement element : LAST_ELEMENTS) {
			element.render(context, tickCounter);
		}
	}

	public static void addFirst(HudElement element) {
		FIRST_ELEMENTS.add(element);
	}

	public static void addLast(HudElement element) {
		LAST_ELEMENTS.add(element);
	}

	public static void attachElementBefore(Identifier before, HudElement element) {
		checkVanillaId(before);
		BEFORE_ELEMENTS.computeIfAbsent(before, k -> new ArrayList<>()).add(element);
	}

	public static void attachElementAfter(Identifier after, HudElement element) {
		checkVanillaId(after);
		AFTER_ELEMENTS.computeIfAbsent(after, k -> new ArrayList<>()).add(element);
	}

	private static void checkVanillaId(Identifier id) {
		if (!VANILLA_IDS.contains(id)) {
			throw new IllegalArgumentException("Invalid HUD element ID: " + id + ". Must be one of the predefined vanilla IDs.");
		}
	}
}
