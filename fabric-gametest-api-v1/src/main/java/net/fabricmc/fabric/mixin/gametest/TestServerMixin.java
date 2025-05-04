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

package net.fabricmc.fabric.mixin.gametest;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.test.GameTestState;
import net.minecraft.test.TestServer;
import net.minecraft.test.TestSet;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.impl.gametest.reporting.TestReporter;

@Mixin(TestServer.class)
public abstract class TestServerMixin {
	@Shadow
	@Nullable
	private TestSet testSet;

	@Inject(method = "isDedicated", at = @At("HEAD"), cancellable = true)
	public void isDedicated(CallbackInfoReturnable<Boolean> cir) {
		// Allow dedicated server commands to be registered.
		// Should aid with mods that use this to detect if they are running on a dedicated server as well.
		cir.setReturnValue(true);
	}

	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/test/TestServer;stop(Z)V"))
	private void complete(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		TestSetAccessor testSetAccessor = (TestSetAccessor) this.testSet;

		Map<String, List<Identifier>> tests = testSetAccessor.getTests().stream()
				.map(GameTestState::toString)
				.map(Identifier::of)
				.collect(Collectors.groupingBy(Identifier::getNamespace));

		for (Map.Entry<String, List<Identifier>> entry : tests.entrySet()) {
			try (var group = TestReporter.group(entry.getKey())) {
				for (Identifier test : entry.getValue()) {
					TestReporter.Test t = TestReporter.test(test.getPath());
					t.success();
				}
			}
		}
	}
}
