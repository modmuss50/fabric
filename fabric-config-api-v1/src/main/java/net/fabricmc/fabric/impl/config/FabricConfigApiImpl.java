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

import net.fabricmc.fabric.api.config.v1.Config;
import net.fabricmc.fabric.api.config.v1.FabricConfigApi;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class FabricConfigApiImpl implements FabricConfigApi {
	private static final Map<Identifier, Config> CONFIGS = new HashMap<>();

	static void register(Config config) {
		ConfigRoot root = (ConfigRoot) config;
		Identifier id = root.getId();

		if (CONFIGS.containsKey(id)) {
			throw new IllegalStateException("Config already registered: " + id);
		}

		// Don't allow any further modification to the config specification
		root.finalizeSpec();

		CONFIGS.put(id, config);
	}
}
