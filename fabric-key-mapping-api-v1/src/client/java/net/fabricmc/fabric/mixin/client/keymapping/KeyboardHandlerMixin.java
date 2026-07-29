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

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;

import net.fabricmc.fabric.impl.client.keymapping.KeyBindsScreenModifierKeyAccess;
import net.fabricmc.fabric.impl.client.keymapping.KeyMappingModifierImpl;

@Mixin(KeyboardHandler.class)
abstract class KeyboardHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@WrapMethod(method = "keyPress")
	private void fabric_withModifiers(long handle, int action, KeyEvent event, Operation<Void> original) {
		KeyMappingModifierImpl.withInputEvent(event.modifiers(), action, () -> {
			if (handle != 0L && handle == minecraft.getWindow().handle()) {
				KeyMappingModifierImpl.prepareInputEvent(InputConstants.getKey(event), event.modifiers(), action);
			}

			original.call(handle, action, event);
		});
	}

	@WrapOperation(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyReleased(Lnet/minecraft/client/input/KeyEvent;)Z"))
	private boolean fabric_finishModifierKey(Screen screen, KeyEvent event, Operation<Boolean> original) {
		if (screen instanceof KeyBindsScreenModifierKeyAccess access && access.fabric_finishModifierKey(event)) {
			return true;
		}

		return original.call(screen, event);
	}

	@Definition(id = "key", field = "Lnet/minecraft/client/KeyMapping;key:Lcom/mojang/blaze3d/platform/InputConstants$Key;")
	@Definition(id = "getValue", method = "Lcom/mojang/blaze3d/platform/InputConstants$Key;getValue()I")
	@Expression("?.key.getValue() == ?.key.getValue()")
	@ModifyExpressionValue(method = "keyPress", at = @At("MIXINEXTRAS:EXPRESSION"))
	private boolean fabric_debugKeysAreSame(boolean original) {
		return KeyMappingModifierImpl.haveSameModifiers(minecraft.options.keyDebugModifier, minecraft.options.keyDebugOverlay, original);
	}

	@ModifyExpressionValue(method = "keyPress", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants;isKeyDown(I)Z"))
	private boolean fabric_debugCrashModifiersAreDown(boolean original) {
		return original && KeyMappingModifierImpl.matchesArmedCurrentModifiers(minecraft.options.keyDebugCrash);
	}

	@Definition(id = "eventKey", local = @Local(type = InputConstants.Key.class, name = "key"))
	@Definition(id = "key", field = "Lnet/minecraft/client/KeyMapping;key:Lcom/mojang/blaze3d/platform/InputConstants$Key;")
	@Expression("eventKey == ?.key")
	@ModifyExpressionValue(method = "keyPress", at = @At("MIXINEXTRAS:EXPRESSION"))
	private boolean fabric_debugModifierMatches(boolean original) {
		return original && KeyMappingModifierImpl.matchesArmedCurrentModifiers(minecraft.options.keyDebugModifier);
	}
}
