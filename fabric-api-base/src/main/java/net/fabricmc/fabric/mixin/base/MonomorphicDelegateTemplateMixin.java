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

package net.fabricmc.fabric.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.fabricmc.loader.api.FabricLoader;

@Mixin(targets = "net.minecraft.util.monomorphizer.MonomorphicDelegateTemplate")
abstract class MonomorphicDelegateTemplateMixin {
	@ModifyReturnValue(method = "loadClassBytes", at = @At("RETURN"))
	private static byte[] transformClassBytes(byte[] bytes, Class<?> targetClass) {
		return FabricLoader.getInstance().transformClassBytes(targetClass.getName(), bytes);
	}
}
