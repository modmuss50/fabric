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

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import net.fabricmc.loader.api.FabricLoader;

public class ConfigDirWatcher {
	private static ConfigDirWatcher INSTANCE = null;

	public static void startWatching() throws IOException {
		if (INSTANCE == null) {
			INSTANCE = new ConfigDirWatcher();
		}
	}

	private final Path configDir;
	private final WatchService watchService;

	private ConfigDirWatcher() throws IOException {
		configDir = FabricLoader.getInstance().getConfigDir();
		watchService = FileSystems.getDefault().newWatchService();

		configDir.register(
				watchService,
				StandardWatchEventKinds.ENTRY_MODIFY);
	}

	private void poll() throws InterruptedException {
		WatchKey key;
		while ((key = watchService.take()) != null) {
			for (WatchEvent<?> event : key.pollEvents()) {
				// TODO: Handle the event
			}
			key.reset();
		}
	}
}
