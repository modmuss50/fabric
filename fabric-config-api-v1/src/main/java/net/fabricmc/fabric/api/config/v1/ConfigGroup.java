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

import java.util.List;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.config.v1.ui.UIControl;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.util.StringIdentifiable;

@ApiStatus.NonExtendable
public interface ConfigGroup extends ConfigEntry {
	<T> ConfigValue<T> codec(String key, Codec<T> codec, T defaultValue);

	default ConfigValue<Boolean> boolValue(String key, boolean defaultValue) {
		return codec(key, Codec.BOOL, defaultValue)
				.uiControl(UIControl.BooleanControl.FACTORY);
	}

	default ConfigValue<String> stringValue(String key, String defaultValue) {
		return codec(key, Codec.STRING, defaultValue)
				.uiControl(UIControl.StringControl.FACTORY);
	}

	default ConfigValue<List<String>> stringListValue(String key, List<String> defaultValue) {
		return codec(key, Codec.STRING.listOf(), defaultValue)
				.uiControl(UIControl.StringListControl.FACTORY);
	}

	default ConfigValue<Byte> byteValue(String key, byte defaultValue) {
		return codec(key, Codec.BYTE, defaultValue)
				.uiControl(UIControl.NumberControl.BYTE_FACTORY);
	}

	default ConfigValue<Short> shortValue(String key, short defaultValue) {
		return codec(key, Codec.SHORT, defaultValue)
				.uiControl(UIControl.NumberControl.SHORT_FACTORY);
	}

	default ConfigValue<Integer> intValue(String key, int defaultValue) {
		return codec(key, Codec.INT, defaultValue)
				.uiControl(UIControl.NumberControl.INTEGER_FACTORY);
	}

	default ConfigValue<Long> longValue(String key, long defaultValue) {
		return codec(key, Codec.LONG, defaultValue)
				.uiControl(UIControl.NumberControl.LONG_FACTORY);
	}

	default ConfigValue<Float> floatValue(String key, float defaultValue) {
		return codec(key, Codec.FLOAT, defaultValue)
				.uiControl(UIControl.NumberControl.FLOAT_FACTORY);
	}

	default ConfigValue<Double> doubleValue(String key, double defaultValue) {
		return codec(key, Codec.DOUBLE, defaultValue)
				.uiControl(UIControl.NumberControl.DOUBLE_FACTORY);
	}

	default <T extends StringIdentifiable> ConfigValue<T> enumValue(String key, Supplier<T[]> values, T defaultValue) {
		return codec(key, StringIdentifiable.createBasicCodec(values), defaultValue);
	}

	ConfigGroup group(String key);

	@Override
	ConfigGroup comment(String comment);
}
