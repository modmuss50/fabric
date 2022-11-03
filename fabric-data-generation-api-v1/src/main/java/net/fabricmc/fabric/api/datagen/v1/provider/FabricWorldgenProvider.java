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

package net.fabricmc.fabric.api.datagen.v1.provider;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.class_7871;
import net.minecraft.class_7876;
import net.minecraft.command.CommandRegistryWrapper;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.data.report.WorldgenProvider;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.RegistryOps;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.RegistryLoader;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.feature.PlacedFeature;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;

/**
 * A provider to help with data-generation of worldgen objects.
 */
@ApiStatus.Experimental
public abstract class FabricWorldgenProvider implements DataProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(WorldgenProvider.class);

	private final FabricDataOutput output;
	private final CompletableFuture<CommandRegistryWrapper.class_7874> registriesFuture;

	public FabricWorldgenProvider(FabricDataOutput output, CompletableFuture<CommandRegistryWrapper.class_7874> registriesFuture) {
		this.output = output;
		this.registriesFuture = registriesFuture;
	}

	protected abstract void configure(CommandRegistryWrapper.class_7874 registries, Entries entries);

	public static final class Entries {
		private final CommandRegistryWrapper.class_7874 registries;
		// Registry ID -> Entries for that registry
		private final Map<Identifier, RegistryEntries<?>> queuedEntries;

		@ApiStatus.Internal
		Entries(CommandRegistryWrapper.class_7874 registries) {
			this.registries = registries;
			this.queuedEntries = RegistryLoader.DYNAMIC_REGISTRIES.stream()
					.collect(Collectors.toMap(
							e -> e.key().getValue(),
							e -> RegistryEntries.create(registries, e)
					));
		}

		/**
		 * Gets access to all lookups.
		 */
		public CommandRegistryWrapper.class_7874 getLookups() {
			return registries;
		}

		/**
		 * Gets a lookup for entries from the given registry.
		 */
		public <T> class_7871<T> getLookup(RegistryKey<? extends Registry<T>> registryKey) {
			return registries.method_46762(registryKey);
		}

		/**
		 * Returns a lookup for placed features. Useful when creating biomes.
		 */
		public class_7871<PlacedFeature> placedFeatures() {
			return getLookup(Registry.PLACED_FEATURE_KEY);
		}

		/**
		 * Returns a lookup for configured carvers features. Useful when creating biomes.
		 */
		public class_7871<ConfiguredCarver<?>> configuredCarvers() {
			return getLookup(Registry.CONFIGURED_CARVER_KEY);
		}

		/**
		 * Gets a reference to a registry entry for use in other registrations.
		 */
		public <T> RegistryEntry<T> ref(RegistryKey<T> key) {
			RegistryEntries<T> entries = getQueuedEntries(key);
			return RegistryEntry.Reference.standAlone(entries.lookup, key);
		}

		/**
		 * Adds a new object to be data generated and returns a reference to it for use in other worldgen objects.
		 */
		public <T> RegistryEntry<T> add(RegistryKey<T> registry, T object) {
			return getQueuedEntries(registry).add(registry.getValue(), object);
		}

		@SuppressWarnings("unchecked")
		<T> RegistryEntries<T> getQueuedEntries(RegistryKey<T> key) {
			RegistryEntries<?> regEntries = queuedEntries.get(key.getRegistry());

			if (regEntries == null) {
				throw new IllegalArgumentException("Registry " + key.getRegistry() + " is not loaded from datapacks");
			}

			return (RegistryEntries<T>) regEntries;
		}
	}

	private static class RegistryEntries<T> {
		final net.minecraft.class_7876<T> lookup;
		final RegistryKey<? extends Registry<T>> registry;
		final Codec<T> elementCodec;
		Map<RegistryKey<T>, T> entries = new IdentityHashMap<>();

		RegistryEntries(class_7876<T> lookup,
						RegistryKey<? extends Registry<T>> registry,
						Codec<T> elementCodec) {
			this.lookup = lookup;
			this.registry = registry;
			this.elementCodec = elementCodec;
		}

		static <T> RegistryEntries<T> create(CommandRegistryWrapper.class_7874 lookups, RegistryLoader.Entry<T> loaderEntry) {
			CommandRegistryWrapper.Impl<T> lookup = lookups.method_46762(loaderEntry.key());
			return new RegistryEntries<>(lookup, loaderEntry.key(), loaderEntry.elementCodec());
		}

		public RegistryEntry<T> add(RegistryKey<T> key, T value) {
			if (entries.put(key, value) != null) {
				throw new IllegalArgumentException("Trying to add registry key " + key + " more than once.");
			}

			return RegistryEntry.Reference.standAlone(lookup, key);
		}

		public RegistryEntry<T> add(Identifier id, T value) {
			return add(RegistryKey.of(registry, id), value);
		}
	}

	@Override
	public CompletableFuture<?> run(DataWriter writer) {
		return registriesFuture.thenCompose(registries -> {
			return CompletableFuture
					.supplyAsync(() -> {
						Entries entries = new Entries(registries);
						configure(registries, entries);
						return entries;
					})
					.thenCompose(entries -> {
						final RegistryOps<JsonElement> dynamicOps = RegistryOps.method_46632(JsonOps.INSTANCE, registries);
						ArrayList<CompletableFuture<?>> futures = new ArrayList<>();

						for (RegistryEntries<?> registryEntries : entries.queuedEntries.values()) {
							futures.add(writeRegistryEntries(writer, dynamicOps, registryEntries));
						}

						return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
					});
		});
	}

	private <T> CompletableFuture<?> writeRegistryEntries(DataWriter writer, RegistryOps<JsonElement> ops, RegistryEntries<T> entries) {
		final RegistryKey<? extends Registry<T>> registry = entries.registry;
		final DataOutput.PathResolver pathResolver = output.getResolver(DataOutput.OutputType.DATA_PACK, registry.getValue().getPath());
		final List<CompletableFuture<?>> futures = new ArrayList<>();

		for (Map.Entry<RegistryKey<T>, T> entry : entries.entries.entrySet()) {
			Path path = pathResolver.resolveJson(entry.getKey().getValue());
			futures.add(writeToPath(path, writer, ops, entries.elementCodec, entry.getValue()));
		}

		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
	}

	private static <E> CompletableFuture<?> writeToPath(Path path, DataWriter cache, DynamicOps<JsonElement> json, Encoder<E> encoder, E value) {
		Optional<JsonElement> optional = encoder.encodeStart(json, value).resultOrPartial((error) -> {
			LOGGER.error("Couldn't serialize element {}: {}", path, error);
		});

		if (optional.isPresent()) {
			return DataProvider.writeToPath(cache, optional.get(), path);
		}

		return CompletableFuture.completedFuture(null);
	}
}
