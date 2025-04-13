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

package net.fabricmc.fabric.test.config.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.minecraft.util.Identifier;

import org.junit.jupiter.api.Test;

import net.fabricmc.fabric.api.config.v1.Config;
import net.fabricmc.fabric.api.config.v1.ConfigGroup;
import net.fabricmc.fabric.api.config.v1.ConfigValue;
import net.fabricmc.fabric.api.config.v1.FabricConfigApi;
import net.fabricmc.fabric.impl.config.ConfigGroupImpl;
import net.fabricmc.fabric.impl.config.serialization.ConfigDecoder;
import net.fabricmc.fabric.impl.config.serialization.ConfigEncoder;

public class FabricConfigApiTest {
	private static final Identifier ID = Identifier.of("fabric", "test");

	@Test
	void writeToSnbt() {
		interface TestConfig {
			Config CONFIG = FabricConfigApi.config(ID);

			ConfigValue<String> EXAMPLE_STRING = CONFIG.stringValue("example_string", "default")
					.comment("This is an example string config value");
			ConfigValue<Integer> EXAMPLE_INT = CONFIG.intValue("example_int", 123)
					.comment("This is an example int config value")
					.syncWithClient();

			ConfigValue<Float> FLOAT = CONFIG.floatValue("float", 1.0F)
											.comment("This is an example float config value")
											.validate((f -> f > 0), "Value must be greater than 0")
											.oneOf(1.0F, 2.0F, 3.0F)
											.requiresRestart()
											.syncWithClient();

			ConfigGroup GROUP = CONFIG.group("group")
									.comment("Groups can have comments as well");

			ConfigValue<List<String>> LIST = GROUP.codec("list", Codec.STRING.listOf(), List.of("a", "b", "c"));
			ConfigValue<Map<String, String>> MAP = GROUP.codec("map", Codec.unboundedMap(Codec.STRING, Codec.STRING), Map.of("key", "value"))
					.comment("This is an example map config value");
		}

		ConfigGroupImpl impl = (ConfigGroupImpl) TestConfig.CONFIG;
		String snbt = ConfigEncoder.encodeToSNBT(impl);

		assertEquals("""
				{
					// This is an example string config value
					example_string: "default",

					// This is an example int config value
					example_int: 123,

					// Groups can have comments as well
					group: {
						list: [
							"a",
							"b",
							"c"
						],

						// This is an example map config value
						map: {
							key: "value"
						}
					}
				}
				""".trim(), snbt);
	}

	@Test
	void readUpdatedValue() {
		interface TestConfig {
			Config CONFIG = FabricConfigApi.config(ID);
			ConfigValue<String> VALUE = CONFIG.stringValue("value", "hello");
		}

		assertEquals("hello", TestConfig.VALUE.get());

		ConfigGroupImpl impl = (ConfigGroupImpl) TestConfig.CONFIG;
		String snbt = ConfigEncoder.encodeToSNBT(impl);

		assertEquals("""
				{
					value: "hello"
				}
				""".trim(), snbt);

		snbt = snbt.replace("hello", "world");
		ConfigDecoder.decodeInto(snbt, impl);

		assertEquals("world", TestConfig.VALUE.get());
	}

	@Test
	void readInvalidConfig() {
		interface TestConfig {
			Config CONFIG = FabricConfigApi.config(ID);
			ConfigValue<String> VALUE = CONFIG.stringValue("value", "hello");
		}

		ConfigGroupImpl impl = (ConfigGroupImpl) TestConfig.CONFIG;
		String snbt = """
				{
					value: "world",
					unknown: "world"
				}
				""";

		ConfigDecoder.decodeInto(snbt, impl);

		assertEquals("world", TestConfig.VALUE.get());
	}

	@Test
	void readPartialConfig() {
		interface TestConfig {
			Config CONFIG = FabricConfigApi.config(ID);
			ConfigValue<String> A = CONFIG.stringValue("a", "hello");
			ConfigValue<String> B = CONFIG.stringValue("b", "hello");
			ConfigValue<String> C = CONFIG.stringValue("c", "hello");
		}

		ConfigGroupImpl impl = (ConfigGroupImpl) TestConfig.CONFIG;
		String snbt = """
				{
					a: "world",
					c: "world"
				}
				""";

		ConfigDecoder.decodeInto(snbt, impl);

		assertEquals("world", TestConfig.A.get());
		assertEquals("hello", TestConfig.B.get());
		assertEquals("world", TestConfig.C.get());
	}
}
