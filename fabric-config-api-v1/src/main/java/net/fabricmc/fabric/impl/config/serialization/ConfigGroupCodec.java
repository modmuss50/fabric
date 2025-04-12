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

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.RecordBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.impl.config.AbstractConfigEntry;

/**
 * We need a codec is that is resilient to errors in the input data.
 */
public record ConfigGroupCodec(Map<String, AbstractConfigEntry> configEntries) implements Codec<Map<String, AbstractConfigEntry>> {
	private static final Logger LOGGER = LoggerFactory.getLogger("ConfigGroupCodec");

	@Override
	public <T> DataResult<T> encode(final Map<String, AbstractConfigEntry> input, final DynamicOps<T> ops, final T prefix) {
		final RecordBuilder<T> mapBuilder = ops.mapBuilder();

		for (final Map.Entry<String, AbstractConfigEntry> entry : input.entrySet()) {
			mapBuilder.add(Codec.STRING.encodeStart(ops, entry.getKey()), encodeValue(configEntries.get(entry.getKey()).codec(), entry.getValue(), ops));
		}

		return mapBuilder.build(prefix);
	}

	@SuppressWarnings("unchecked")
	private <T, V2 extends AbstractConfigEntry> DataResult<T> encodeValue(final Codec<V2> codec, final AbstractConfigEntry input, final DynamicOps<T> ops) {
		return codec.encodeStart(ops, (V2) input);
	}

	@Override
	public <T> DataResult<Pair<Map<String, AbstractConfigEntry>, T>> decode(final DynamicOps<T> ops, final T input) {
		return ops.getMap(input).flatMap(map -> {
			final ImmutableMap.Builder<String, AbstractConfigEntry> builder = ImmutableMap.builder();

			map.entries().forEach(pair -> {
				final DataResult<String> k = Codec.STRING.parse(ops, pair.getFirst());
				Optional<String> optionalK = k.result();

				if (optionalK.isEmpty()) {
					LOGGER.error("Failed to decode key {} from {}  {}", k, pair, k.resultOrPartial());
					return;
				}

				if (!configEntries.containsKey(optionalK.get())) {
					LOGGER.error("Config key '{}' not found in config entries", optionalK.get());
					return;
				}

				final DataResult<? extends AbstractConfigEntry> v = configEntries.get(optionalK.get()).codec().parse(ops, pair.getSecond());
				Optional<? extends AbstractConfigEntry> optionalV = v.result();

				if (optionalV.isEmpty()) {
					LOGGER.error("Failed to decode value {} from {}  {}", k, pair, v.resultOrPartial());
					return;
				}

				builder.put(optionalK.get(), optionalV.get());
			});

			return DataResult.success(Pair.of(ImmutableMap.copyOf(builder.build()), input));
		});
	}
}
