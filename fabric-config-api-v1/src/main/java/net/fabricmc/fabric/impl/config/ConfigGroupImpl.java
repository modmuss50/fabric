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

package net.fabricmc.fabric.impl.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.config.v1.ConfigGroup;
import net.fabricmc.fabric.api.config.v1.ConfigValue;
import net.fabricmc.fabric.impl.config.serialization.ConfigGroupCodec;

public class ConfigGroupImpl extends AbstractConfigEntry implements ConfigGroup {
	private final Map<String, AbstractConfigEntry> entries = new HashMap<>();
	private final Codec<Map<String, AbstractConfigEntry>> entriesCodec = Codec.dispatchedMap(Codec.STRING, key -> entries.get(key).codec());
	private final ConfigGroupCodec groupCodec = new ConfigGroupCodec(entries);

	@Override
	public <T> ConfigValue<T> codec(String key, Codec<T> codec, T defaultValue) {
		return addEntry(key, new ConfigValueImpl<>(key, codec, defaultValue));
	}

	@Override
	public ConfigGroup group(String key) {
		return addEntry(key, new ConfigGroupImpl());
	}

	@Override
	public ConfigGroup comment(String comment) {
		super.comment(comment);
		return this;
	}

	private <T extends AbstractConfigEntry> T addEntry(String key, T entry) {
		if (entries.containsKey(key)) {
			throw new IllegalStateException("Entry with key '" + key + "' already exists in the config group");
		}

		entries.put(key, entry);
		return entry;
	}

	public Map<String, AbstractConfigEntry> getEntries() {
		return Collections.unmodifiableMap(entries);
	}

	@Override
	public Codec<ConfigGroupImpl> codec() {
		return groupCodec
				.xmap(newEntries -> {
					// this.entries.putAll(newEntries);
					return this;
				}, ConfigGroupImpl::getEntries);
	}

	@Override
	public void finalizeSpec() {
		super.finalizeSpec();

		for (AbstractConfigEntry entry : entries.values()) {
			entry.finalizeSpec();
		}
	}
}
