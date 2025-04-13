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

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface ConfigGroup extends ConfigEntry {
	<T> ConfigValue<T> codec(String key, Codec<T> codec, T defaultValue);

	default ConfigValue<Boolean> boolValue(String key, boolean defaultValue) {
		return codec(key, Codec.BOOL, defaultValue);
	}

	StringConfigValue stringValue(String key, String defaultValue);

	<T extends Number> NumericConfigValue<T> numeric(String key, Codec<T> codec, T defaultValue);

	default NumericConfigValue<Byte> byteValue(String key, byte defaultValue) {
		return numeric(key, Codec.BYTE, defaultValue);
	}

	default NumericConfigValue<Short> shortValue(String key, short defaultValue) {
		return numeric(key, Codec.SHORT, defaultValue);
	}

	default NumericConfigValue<Integer> intValue(String key, int defaultValue) {
		return numeric(key, Codec.INT, defaultValue);
	}

	default NumericConfigValue<Long> longValue(String key, long defaultValue) {
		return numeric(key, Codec.LONG, defaultValue);
	}

	default NumericConfigValue<Float> floatValue(String key, float defaultValue) {
		return numeric(key, Codec.FLOAT, defaultValue);
	}

	default NumericConfigValue<Double> doubleValue(String key, double defaultValue) {
		return numeric(key, Codec.DOUBLE, defaultValue);
	}

	ConfigGroup group(String key);

	@Override
	ConfigGroup comment(String comment);
}
