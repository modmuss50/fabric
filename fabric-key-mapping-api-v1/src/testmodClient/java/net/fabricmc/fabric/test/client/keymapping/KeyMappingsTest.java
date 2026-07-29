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

package net.fabricmc.fabric.test.client.keymapping;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.sdl.SDLScancode;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyModifier;

public class KeyMappingsTest implements ClientModInitializer {
	static KeyMapping plainB;
	static KeyMapping shiftB;
	static KeyMapping controlB;
	static KeyMapping shiftControlMouse4;
	static KeyMapping shiftToggle;
	static KeyMapping shiftConditionalToggle;
	static KeyMapping editable;
	static boolean conditionalToggleMode;

	@Override
	public void onInitializeClient() {
		// Register 2 before 1, but in-game 1 should appear before 2 due to sorting. Both should appear after all vanilla categories.
		KeyMapping.Category category2 = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("fabric-key-mapping-api-v1-testmod", "test_category_2"));
		KeyMapping.Category category1 = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("fabric-key-mapping-api-v1-testmod", "test_category_1"));

		KeyMapping binding1 = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.fabric-key-mapping-api-v1-testmod.test_keymapping_1", InputConstants.Type.KEYBOARD, SDLScancode.SDL_SCANCODE_P, category1));
		KeyMapping binding2 = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.fabric-key-mapping-api-v1-testmod.test_keymapping_2", InputConstants.Type.KEYBOARD, SDLScancode.SDL_SCANCODE_U, category1));
		KeyMapping stickyBinding = KeyMappingHelper.registerKeyMapping(new ToggleKeyMapping("key.fabric-key-mapping-api-v1-testmod.test_keymapping_sticky", SDLScancode.SDL_SCANCODE_R, category2, () -> true, false));
		KeyMapping duplicateBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.fabric-key-mapping-api-v1-testmod.test_keymapping_duplicate", SDLScancode.SDL_SCANCODE_RSHIFT, category2));
		plainB = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.fabric-key-mapping-api-v1-testmod.plain_b", InputConstants.KEY_B, category2));
		shiftB = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.fabric-key-mapping-api-v1-testmod.shift_b", InputConstants.KEY_B, category2), KeyModifier.SHIFT);
		controlB = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.fabric-key-mapping-api-v1-testmod.control_b", InputConstants.KEY_B, category2), KeyModifier.CONTROL);
		shiftControlMouse4 = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.fabric-key-mapping-api-v1-testmod.shift_control_mouse_4", InputConstants.Type.MOUSE, InputConstants.MOUSE_BUTTON_4, category2), KeyModifier.SHIFT, KeyModifier.CONTROL);
		shiftToggle = KeyMappingHelper.registerKeyMapping(new ToggleKeyMapping("key.fabric-key-mapping-api-v1-testmod.shift_toggle", InputConstants.KEY_F13, category2, () -> true, false), KeyModifier.SHIFT);
		shiftConditionalToggle = KeyMappingHelper.registerKeyMapping(new ToggleKeyMapping("key.fabric-key-mapping-api-v1-testmod.shift_conditional_toggle", InputConstants.KEY_F14, category2, () -> conditionalToggleMode, false), KeyModifier.SHIFT);
		editable = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.fabric-key-mapping-api-v1-testmod.editable", InputConstants.KEY_G, category2));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;

			while (binding1.consumeClick()) {
				client.player.sendSystemMessage(Component.literal("Key 1 was pressed!"));
			}

			while (binding2.consumeClick()) {
				client.player.sendSystemMessage(Component.literal("Key 2 was pressed!"));
			}

			if (stickyBinding.isDown()) {
				client.player.sendSystemMessage(Component.literal("Sticky Key was pressed!"));
			}

			while (duplicateBinding.consumeClick()) {
				client.player.sendSystemMessage(Component.literal("Duplicate Key was pressed!"));
			}
		});
	}
}
