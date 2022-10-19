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

package net.fabricmc.fabric.impl.itemgroup;

import java.util.Arrays;
import java.util.stream.Stream;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.class_7706;
import net.minecraft.item.ItemGroup;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.mixin.itemgroup.ItemGroupAccessor;
import net.fabricmc.fabric.mixin.itemgroup.class_7706Accessor;

@ApiStatus.Internal
public final class ItemGroupHelper {
	private ItemGroupHelper() {
	}

	public static void appendItemGroup(FabricItemGroup itemGroup) {
		final int index = class_7706.field_40207.length + 1;
		final ItemGroup[] itemGroups = Stream.concat(Arrays.stream(class_7706.field_40207), Stream.of(itemGroup))
				.toArray(ItemGroup[]::new);

		((ItemGroupAccessor) itemGroup).setIndex(index);
		class_7706Accessor.setItemGroups(class_7706Accessor.invokeBuildArray(itemGroups));
	}
}
