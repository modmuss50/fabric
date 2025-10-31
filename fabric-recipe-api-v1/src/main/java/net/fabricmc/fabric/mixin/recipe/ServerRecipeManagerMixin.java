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

package net.fabricmc.fabric.mixin.recipe;

import java.util.Collection;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.fabricmc.fabric.api.recipe.v1.FabricServerRecipeManager;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

@Mixin(RecipeManager.class)
public abstract class ServerRecipeManagerMixin implements FabricServerRecipeManager {
	@Shadow
	private RecipeMap recipes;

	@Override
	public <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> getAllOfType(RecipeType<T> type) {
		return this.recipes.byType(type);
	}

	@Override
	public <I extends RecipeInput, T extends Recipe<I>> Stream<RecipeHolder<T>> getAllMatches(RecipeType<T> type, I input, Level world) {
		return this.recipes.getRecipesFor(type, input, world);
	}
}
