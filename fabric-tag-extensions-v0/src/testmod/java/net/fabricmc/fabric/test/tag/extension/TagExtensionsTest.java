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

package net.fabricmc.fabric.test.tag.extension;

import static net.minecraft.server.command.CommandManager.literal;

import net.minecraft.tag.RequiredTagList;
import net.minecraft.tag.Tag;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.tag.FabricRequiredTagListRegistry;

public class TagExtensionsTest implements ModInitializer {
	public static final RequiredTagList<Biome> REQUIRED_TAGS = FabricRequiredTagListRegistry.register(Registry.BIOME_KEY, "tags/test_biome");
	public static final Tag.Identified<Biome> BIOMES = REQUIRED_TAGS.add("fabric-tag-extensions-v0-testmod:example");

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(literal("biome_tag_list").executes((context -> {
			BIOMES.values().forEach(biome -> {
				Identifier biomeId = context.getSource().getWorld().getRegistryManager().get(Registry.BIOME_KEY).getId(biome);
				context.getSource().sendFeedback(new LiteralText(biomeId.toString()), false);
			});

			return 1;
		}))));
	}
}
