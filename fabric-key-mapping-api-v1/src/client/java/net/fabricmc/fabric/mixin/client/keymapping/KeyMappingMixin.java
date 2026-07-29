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

import java.util.function.Consumer;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import net.fabricmc.fabric.impl.client.keymapping.KeyMappingModifierImpl;

@Mixin(KeyMapping.class)
abstract class KeyMappingMixin {
	@Unique
	private static boolean fabric_releasingKeyMapping;

	@WrapMethod(method = "set")
	private static void fabric_markRelease(InputConstants.Key key, boolean state, Operation<Void> original) {
		boolean previous = fabric_releasingKeyMapping;
		fabric_releasingKeyMapping = !state;

		try {
			original.call(key, state);
		} finally {
			fabric_releasingKeyMapping = previous;
		}
	}

	@WrapWithCondition(method = "forAllKeyMappings", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
	private static boolean fabric_testModifiers(Consumer<KeyMapping> operation, Object value, @Local(argsOnly = true) InputConstants.Key key) {
		return KeyMappingModifierImpl.shouldProcess((KeyMapping) value, key, fabric_releasingKeyMapping);
	}

	@WrapOperation(method = "setAll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;setDown(Z)V"))
	private static void fabric_restoreWithModifiers(KeyMapping keyMapping, boolean down, Operation<Void> original) {
		original.call(keyMapping, down && KeyMappingModifierImpl.matchesCurrentModifiers(keyMapping));
	}

	@ModifyReturnValue(method = "matches(Lnet/minecraft/client/input/KeyEvent;)Z", at = @At("RETURN"))
	private boolean fabric_matchesKeyEvent(boolean original, @Local(argsOnly = true) KeyEvent event) {
		return original && KeyMappingModifierImpl.matchesInputEvent((KeyMapping) (Object) this, event.modifiers());
	}

	@ModifyReturnValue(method = "matchesMouse", at = @At("RETURN"))
	private boolean fabric_matchesMouseEvent(boolean original, @Local(argsOnly = true) MouseButtonEvent event) {
		return original && KeyMappingModifierImpl.matchesMouseInputEvent((KeyMapping) (Object) this, event.modifiers());
	}

	@ModifyReturnValue(method = "matches(Lcom/mojang/blaze3d/platform/InputConstants$Key;)Z", at = @At("RETURN"))
	private boolean fabric_matchesKey(boolean original) {
		return original && KeyMappingModifierImpl.matchesCurrentInputEvent((KeyMapping) (Object) this);
	}

	@ModifyReturnValue(method = "same", at = @At("RETURN"))
	private boolean fabric_same(boolean original, @Local(argsOnly = true) KeyMapping other) {
		return KeyMappingModifierImpl.same((KeyMapping) (Object) this, other, original);
	}

	@ModifyReturnValue(method = "isDefault", at = @At("RETURN"))
	private boolean fabric_isDefault(boolean original) {
		return KeyMappingModifierImpl.isDefault((KeyMapping) (Object) this, original);
	}

	@ModifyReturnValue(method = "getTranslatedKeyMessage", at = @At("RETURN"))
	private Component fabric_addModifiers(Component original) {
		return KeyMappingModifierImpl.getTranslatedKeyMessage((KeyMapping) (Object) this, original);
	}

	@Inject(method = "setKey", at = @At("TAIL"))
	private void fabric_setKey(InputConstants.Key key, CallbackInfo ci) {
		KeyMappingModifierImpl.onSetKey((KeyMapping) (Object) this, key);
	}
}
