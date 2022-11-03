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

package net.fabricmc.fabric.mixin.registry.sync;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.resource.ResourceManager;
import net.minecraft.util.dynamic.RegistryOps;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.RegistryLoader;

import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;

@Mixin(RegistryLoader.class)
public class RegistryLoaderMixin {
	@Inject(
			method = "load(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/registry/DynamicRegistryManager;Ljava/util/List;)Lnet/minecraft/util/registry/DynamicRegistryManager$Immutable;",
			at = @At(
					value = "INVOKE",
					target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V",
					ordinal = 0
			),
			locals = LocalCapture.CAPTURE_FAILHARD
	)
	private static void beforeLoad(ResourceManager resourceManager, DynamicRegistryManager baseRegistryManager, List<RegistryLoader.Entry<?>> entries, CallbackInfoReturnable<DynamicRegistryManager.Immutable> cir, Map a, List<Pair<MutableRegistry<?>, ?>> registriesList, RegistryOps.class_7863 registryManager) {
		Map<RegistryKey<? extends Registry<?>>, Registry<?>> registries = new IdentityHashMap<>(registriesList.size());

		for (Pair<MutableRegistry<?>, ?> pair : registriesList) {
			registries.put(pair.getFirst().getKey(), pair.getFirst());
		}

		DynamicRegistryManager drm = new DynamicRegistryManager.Immutable() {
			@SuppressWarnings("unchecked")
			public <T> Optional<Registry<T>> getOptional(RegistryKey<? extends Registry<? extends T>> key) {
				return Optional.ofNullable((Registry<T>) registries.get(key));
			}

			public Stream<Entry<?>> streamAllRegistries() {
				return registries.values().stream()
						.map(this::entry);
			}

			private <T> Entry<T> entry(Registry<T> registry) {
				return new Entry<>(registry.getKey(), registry);
			}

			public Immutable toImmutable() {
				return this;
			}
		};

		DynamicRegistrySetupCallback.EVENT.invoker().onRegistrySetup(drm);
	}
}
