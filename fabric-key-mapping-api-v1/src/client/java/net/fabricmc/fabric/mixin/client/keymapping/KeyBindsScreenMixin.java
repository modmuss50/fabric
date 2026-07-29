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

package net.fabricmc.fabric.mixin.client.keymapping;

import com.mojang.blaze3d.platform.InputConstants;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Util;

import net.fabricmc.fabric.impl.client.keymapping.KeyBindsScreenModifierKeyAccess;
import net.fabricmc.fabric.impl.client.keymapping.KeyMappingModifierImpl;

@Mixin(KeyBindsScreen.class)
abstract class KeyBindsScreenMixin implements KeyBindsScreenModifierKeyAccess {
	@Shadow
	public @Nullable KeyMapping selectedKey;

	@Shadow
	public long lastKeySelection;

	@Shadow
	private KeyBindsList keyBindsList;

	@Unique
	private InputConstants.@Nullable Key fabric_pendingModifierKey;

	@Unique
	private int fabric_pendingModifierMask;

	@Unique
	private @Nullable KeyMapping fabric_mappingBeingSet;

	@Unique
	private int fabric_modifiersBeingSet;

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void fabric_captureKeyModifiers(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		fabric_mappingBeingSet = null;

		if (selectedKey == null) {
			fabric_pendingModifierKey = null;
			fabric_pendingModifierMask = 0;
			return;
		}

		if (event.isEscape()) {
			fabric_pendingModifierKey = null;
			fabric_pendingModifierMask = 0;
			return;
		}

		InputConstants.Key key = InputConstants.getKey(event);

		if (KeyMappingModifierImpl.isModifierKey(key)) {
			if (fabric_pendingModifierKey == null || !fabric_pendingModifierKey.equals(key)) {
				fabric_pendingModifierKey = key;
				fabric_pendingModifierMask = KeyMappingModifierImpl.modifierMask(event.modifiers(), key);
			}

			cir.setReturnValue(true);
			return;
		}

		fabric_prepareModifierUpdate(selectedKey, event.modifiers(), key);
		fabric_pendingModifierKey = null;
		fabric_pendingModifierMask = 0;
	}

	@Inject(method = "keyPressed", at = @At("RETURN"))
	private void fabric_applyKeyModifiers(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		fabric_applyPreparedModifiers();
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"))
	private void fabric_captureMouseModifiers(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		fabric_mappingBeingSet = null;

		if (selectedKey != null) {
			InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(event.button());
			fabric_prepareModifierUpdate(selectedKey, event.modifiers(), key);
			fabric_pendingModifierKey = null;
			fabric_pendingModifierMask = 0;
		}
	}

	@Inject(method = "mouseClicked", at = @At("RETURN"))
	private void fabric_applyMouseModifiers(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		fabric_applyPreparedModifiers();
	}

	@Override
	public boolean fabric_finishModifierKey(KeyEvent event) {
		InputConstants.Key key = InputConstants.getKey(event);

		if (selectedKey == null || fabric_pendingModifierKey == null || !fabric_pendingModifierKey.equals(key)) {
			return false;
		}

		KeyMapping keyMapping = selectedKey;
		keyMapping.setKey(key);
		KeyMappingModifierImpl.setModifierMask(keyMapping, fabric_pendingModifierMask);
		selectedKey = null;
		lastKeySelection = Util.getMillis();
		fabric_pendingModifierKey = null;
		fabric_pendingModifierMask = 0;
		keyBindsList.resetMappingAndUpdateButtons();
		return true;
	}

	@Unique
	private void fabric_prepareModifierUpdate(KeyMapping keyMapping, int rawModifiers, InputConstants.Key key) {
		fabric_mappingBeingSet = keyMapping;
		fabric_modifiersBeingSet = KeyMappingModifierImpl.modifierMask(rawModifiers, key);
	}

	@Unique
	private void fabric_applyPreparedModifiers() {
		if (fabric_mappingBeingSet != null && selectedKey == null) {
			KeyMappingModifierImpl.setModifierMask(fabric_mappingBeingSet, fabric_modifiersBeingSet);
			keyBindsList.refreshEntries();
		}

		fabric_mappingBeingSet = null;
	}
}
