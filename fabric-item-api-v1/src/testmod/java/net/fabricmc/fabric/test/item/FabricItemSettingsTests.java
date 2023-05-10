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

package net.fabricmc.fabric.test.item;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;

public class FabricItemSettingsTests implements ModInitializer {
	@Override
	public void onInitialize() {
		// Registers an item with a custom equipment slot.
		Item testItem = new Item(new FabricItemSettings().equipmentSlot(stack -> EquipmentSlot.CHEST));
		Registry.register(Registries.ITEM, new Identifier("fabric-item-api-v1-testmod", "test_item"), testItem);

		final List<String> vanillaMethods = getMethods(Item.Settings.class);
		final List<String> fabricMethods = getMethods(FabricItemSettings.class);

		final List<String> missingMethods = new ArrayList<>();

		for (String method : vanillaMethods) {
			if (!fabricMethods.contains(method)) {
				missingMethods.add(method);
			}
		}

		if (missingMethods.isEmpty()) {
			return;
		}

		throw new IllegalStateException("Missing method overrides in FabricItemSettings: " + String.join(", ", missingMethods));
	}

	private List<String> getMethods(Class<?> clazz) {
		List<String> methods = new ArrayList<>();

		for (final Method method : clazz.getDeclaredMethods()) {
			if (method.getReturnType() != clazz) {
				continue;
			}

			if (Modifier.isStatic(method.getModifiers())) {
				continue;
			}

			methods.add(method.getName());
		}

		return Collections.unmodifiableList(methods);
	}
}
