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

package net.fabricmc.fabric.impl.client.renderer;

import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.VisibleForTesting;

import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.RendererProvider;
import net.fabricmc.fabric.impl.base.toposort.NodeSorting;
import net.fabricmc.fabric.impl.base.toposort.SortableNode;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

public final class RendererManager {
	private static EntrypointContainer<RendererProvider> chosenRendererProvider;
	private static Renderer activeRenderer;

	private RendererManager() {
	}

	public static Renderer getRenderer() {
		if (activeRenderer != null) {
			return activeRenderer;
		}

		activeRenderer = getOrLoadRendererProvider().getEntrypoint().getRenderer();
		return activeRenderer;
	}

	public static EntrypointContainer<RendererProvider> getOrLoadRendererProvider() {
		if (chosenRendererProvider != null) {
			return chosenRendererProvider;
		}

		List<EntrypointContainer<RendererProvider>> entrypoints = FabricLoader.getInstance()
				.getEntrypointContainers("fabric-renderer-api-v1:renderer_provider", RendererProvider.class);

		return chosenRendererProvider = sortRenderProviders(entrypoints);
	}

	// TODO can be unit tested easily
	@VisibleForTesting
	public static EntrypointContainer<RendererProvider> sortRenderProviders(List<EntrypointContainer<RendererProvider>> entrypoints) {
		List<SortableRenderProvider> sortedEntrypoints = entrypoints.stream()
				.map(SortableRenderProvider::new)
				.toList();

		for (SortableRenderProvider node : sortedEntrypoints) {
			for (String beforeModId : node.getRendererProvider().getLoadsBefore()) {
				// TODO sortedEntrypoints could become a map
				sortedEntrypoints.stream()
						.filter(otherNode -> otherNode.getId().equals(beforeModId))
						.forEach(otherNode -> SortableNode.link(node, otherNode));
			}
		}

		NodeSorting.sort(sortedEntrypoints, "renderer providers", Comparator.comparing(SortableRenderProvider::getId));

		if (sortedEntrypoints.isEmpty()) {
			throw new NullPointerException("A renderer plug-in has not been provided before Minecraft has loaded. This is unsupported.");
		}

		return sortedEntrypoints.getFirst().provider;
	}

	private static class SortableRenderProvider extends SortableNode<SortableRenderProvider> {
		private final EntrypointContainer<RendererProvider> provider;

		public SortableRenderProvider(EntrypointContainer<RendererProvider> provider) {
			this.provider = provider;
		}

		private String getId() {
			return provider.getProvider().getMetadata().getId();
		}

		private RendererProvider getRendererProvider() {
			return provider.getEntrypoint();
		}

		@Override
		protected String getDescription() {
			return getId();
		}
	}
}
