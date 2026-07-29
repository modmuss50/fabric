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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;

import net.fabricmc.fabric.impl.client.keymapping.KeyMappingModifierImpl;
import net.fabricmc.fabric.impl.client.keymapping.KeyMappingRegistryImpl;

@Mixin(Options.class)
public class OptionsMixin {
	@Unique
	private static final String FABRIC_KEY_MODIFIERS_OPTION = "fabricKeyModifiers";

	@Mutable
	@Shadow
	@Final
	public KeyMapping[] keyMappings;

	@Unique
	private boolean fabric_loadingOptions;

	@Inject(at = @At("HEAD"), method = "load()V")
	public void loadHook(CallbackInfo info) {
		keyMappings = KeyMappingRegistryImpl.process(keyMappings);
	}

	@WrapOperation(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;processOptions(Lnet/minecraft/client/Options$FieldAccess;)V"))
	private void fabric_trackOptionsLoad(Options options, Options.FieldAccess access, Operation<Void> original) {
		fabric_loadingOptions = true;

		try {
			original.call(options, access);
		} finally {
			fabric_loadingOptions = false;
		}
	}

	@Inject(method = "processOptions", at = @At("TAIL"))
	private void fabric_processKeyModifiers(Options.FieldAccess access, CallbackInfo ci) {
		if (fabric_loadingOptions) {
			String overrides = access.process(FABRIC_KEY_MODIFIERS_OPTION, "");
			KeyMappingModifierImpl.applyOverrides(overrides, keyMappings);
		} else {
			String overrides = KeyMappingModifierImpl.serializeOverrides(keyMappings);

			if (!overrides.isEmpty()) {
				access.process(FABRIC_KEY_MODIFIERS_OPTION, overrides);
			}
		}
	}
}
