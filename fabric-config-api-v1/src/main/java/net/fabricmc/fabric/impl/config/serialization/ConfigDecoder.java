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

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtParsing;

import net.fabricmc.fabric.impl.config.ConfigGroupImpl;

public class ConfigDecoder {
	public static void decodeInto(String content, ConfigGroupImpl config) {
		try {
			NbtElement nbt = SnbtParsing.createParser(NbtOps.INSTANCE).parse(new StringReader(content));
			config.codec().decode(NbtOps.INSTANCE, nbt);
		} catch (CommandSyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
