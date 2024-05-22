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

package net.fabricmc.fabric.mixin.client.rendering.shader;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderStage;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.impl.client.rendering.FabricShaderProgram;

import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ShaderProgram.class)
abstract class ShaderProgramMixin {
	@Shadow
	@Final
	private String name;

	// Allow loading FabricShaderPrograms from arbitrary namespaces.
	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Identifier;method_60656(Ljava/lang/String;)Lnet/minecraft/util/Identifier;"), allow = 1)
	private Identifier modifyProgramId(String id, Operation<Identifier> original) {
		if ((Object) this instanceof FabricShaderProgram) {
			return Identifier.method_60654(FabricShaderProgram.rewriteAsId(id, name));
		}

		return original.call(id);
	}

	// Allow loading shader stages from arbitrary namespaces.
	@WrapOperation(method = "loadShader", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Identifier;method_60656(Ljava/lang/String;)Lnet/minecraft/util/Identifier;", ordinal = 0))
	private static Identifier modifyStageId(String id, Operation<Identifier> original, ResourceFactory factory, ShaderStage.Type type, String name) {
		if (name.contains(String.valueOf(Identifier.NAMESPACE_SEPARATOR))) {
			return Identifier.method_60654(FabricShaderProgram.rewriteAsId(id, name));
		}

		return original.call(id);
	}
}
