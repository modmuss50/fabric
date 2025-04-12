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

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.fabricmc.fabric.api.config.v1.Config;
import net.fabricmc.fabric.api.config.v1.FabricConfigApi;
import net.fabricmc.loader.api.FabricLoader;

public class FabricConfigApiImpl implements FabricConfigApi {
	private static final Map<ConfigRef, Config> CONFIGS = new HashMap<>();

	static void register(Config config) {
		ConfigRoot root = (ConfigRoot) config;
		ConfigRef ref = new ConfigRef(root.getModId(), root.getPath());

		if (CONFIGS.containsKey(ref)) {
			throw new IllegalStateException("Config already registered: " + ref);
		}

		// Don't allow any further modification to the config specification
		root.finalizeSpec();

		CONFIGS.put(ref, config);
	}

	private record ConfigRef(String modId, String... path) {
		private ConfigRef {
			// TODO validate the path is reasonable
		}

		Path getPath() {
			return FabricLoader.getInstance().getConfigDir()
					.resolve(modId)
					.resolve(String.join("/", path) + ".cfg");
		}
	}
}
