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

package net.fabricmc.fabric.api.config.v1;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.ApiStatus;

import net.fabricmc.fabric.api.config.v1.constraint.Constraint;
import net.fabricmc.fabric.api.config.v1.ui.UIControl;

@ApiStatus.NonExtendable
public interface ConfigValue<T> extends Supplier<T>, Consumer<T>, ConfigEntry {
	default ConfigValue<T> syncWithClient() {
		return this;
	}

	@Override
	ConfigValue<T> comment(String comment);

	ConfigValue<T> requiresRestart();

	ConfigValue<T> constraint(Constraint<T> constraintConsumer);

	default ConfigValue<T> uiControl(UIControl.Factory<T> uiControlFactory) {
		return uiControl(uiControlFactory, Function.identity(), Function.identity());
	}

	<J> ConfigValue<T> uiControl(UIControl.Factory<J> uiControlFactory,
								 Function<T, J> toUIValue,
								 Function<J, T> fromUIValue);

	UIControl<?> getUIControl();
}
