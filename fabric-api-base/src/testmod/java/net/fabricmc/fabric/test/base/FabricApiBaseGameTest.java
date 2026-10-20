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

package net.fabricmc.fabric.test.base;

import java.util.function.IntSupplier;

import org.spongepowered.asm.mixin.MixinEnvironment;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.monomorphizer.MonomorphizableDelegate;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

public class FabricApiBaseGameTest {
	@GameTest
	public void auditMixins(GameTestHelper helper) {
		MixinEnvironment.getCurrentEnvironment().audit();

		helper.succeed();
	}

	@GameTest
	public void monomorphicDelegatePreservesMixins(GameTestHelper helper) {
		MonomorphicTestRecord target = new MonomorphicTestRecord(41);
		helper.assertTrue(target.getAsInt() == 42, "Mixin must transform the target record");

		IntSupplier delegate = new TestDelegate(target).makeMonomorphic(MonomorphicTestRecord.lookup());
		helper.assertTrue(delegate.getClass().isHidden(), "Delegate must be a generated hidden class");
		helper.assertTrue(delegate.getAsInt() == 42, "Monomorphic delegate must preserve the target's mixin");
		helper.succeed();
	}

	private static final class TestDelegate extends MonomorphizableDelegate<IntSupplier, MonomorphicTestRecord> implements IntSupplier {
		private TestDelegate(MonomorphicTestRecord target) {
			super(target);
		}

		@Override
		public int getAsInt() {
			return target.getAsInt();
		}
	}
}
