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

package net.fabricmc.fabric.impl.config.serialization;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.DataResult;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;

import net.fabricmc.fabric.impl.config.AbstractConfigEntry;
import net.fabricmc.fabric.impl.config.ConfigGroupImpl;

public class ConfigEncoder {
	public static String encodeToSNBT(ConfigGroupImpl config) {
		DataResult<NbtElement> result = config.codec().encodeStart(NbtOps.INSTANCE, config);
		return new ConfigStringFormatter(createEncodeContext(config)).apply(result.getOrThrow());
	}

	private static EncodeContext createEncodeContext(ConfigGroupImpl group) {
		var comments = new HashMap<String, String>();
		var childContexts = new HashMap<String, EncodeContext>();

		for (Map.Entry<String, AbstractConfigEntry> entry : group.getEntries().entrySet()) {
			AbstractConfigEntry configEntry = entry.getValue();

			if (configEntry.getComment() != null) {
				comments.put(entry.getKey(), configEntry.getComment());
			}

			if (configEntry instanceof ConfigGroupImpl configGroup) {
				childContexts.put(entry.getKey(), createEncodeContext(configGroup));
			}
		}

		return new EncodeContext(comments, childContexts);
	}
}
