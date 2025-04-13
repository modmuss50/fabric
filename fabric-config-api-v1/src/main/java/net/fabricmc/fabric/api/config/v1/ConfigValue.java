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

import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface ConfigValue<T> extends Supplier<T>, ConfigEntry {
	default ConfigValue<T> syncWithClient() {
		return this;
	}

	@Override
	ConfigValue<T> comment(String comment);

	ConfigValue<T> validate(Predicate<T> predicate, String requirement);

	ConfigValue<T> oneOf(T... values);

	ConfigValue<T> requiresRestart();
}
