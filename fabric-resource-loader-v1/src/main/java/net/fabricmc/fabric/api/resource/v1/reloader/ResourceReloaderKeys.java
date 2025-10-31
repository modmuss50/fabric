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

package net.fabricmc.fabric.api.resource.v1.reloader;

import net.minecraft.resources.ResourceLocation;

/**
 * This class contains default keys for various Minecraft resource reloaders.
 *
 * @see net.minecraft.server.packs.resources.PreparableReloadListener
 */
public final class ResourceReloaderKeys {
	/**
	 * Represents the application phase before Vanilla resource reloaders are invoked.
	 *
	 * <p>No resource reloaders are assigned to this identifier.
	 *
	 * @see net.fabricmc.fabric.api.resource.v1.ResourceLoader#addReloaderOrdering(ResourceLocation, ResourceLocation)
	 */
	public static final ResourceLocation BEFORE_VANILLA = ResourceLocation.fromNamespaceAndPath("fabric", "before_vanilla");
	/**
	 * Represents the application phase after Vanilla resource reloaders are invoked.
	 *
	 * <p>No resource reloaders are assigned to this identifier.
	 *
	 * @see net.fabricmc.fabric.api.resource.v1.ResourceLoader#addReloaderOrdering(ResourceLocation, ResourceLocation)
	 */
	public static final ResourceLocation AFTER_VANILLA = ResourceLocation.fromNamespaceAndPath("fabric", "after_vanilla");

	private ResourceReloaderKeys() { }

	/**
	 * Keys for various client resource reloaders.
	 */
	public static final class Client {
		public static final ResourceLocation BLOCK_ENTITY_RENDERERS = ResourceLocation.withDefaultNamespace("block_entity_renderers");
		public static final ResourceLocation BLOCK_RENDER_MANAGER = ResourceLocation.withDefaultNamespace("block_render_manager");
		public static final ResourceLocation CLOUD_CELLS = ResourceLocation.withDefaultNamespace("cloud_cells");
		public static final ResourceLocation EQUIPMENT_MODELS = ResourceLocation.withDefaultNamespace("equipment_models");
		public static final ResourceLocation ENTITY_RENDERERS = ResourceLocation.withDefaultNamespace("entity_renderers");
		public static final ResourceLocation DRY_FOLIAGE_COLORMAP = ResourceLocation.withDefaultNamespace("dry_foliage_colormap");
		public static final ResourceLocation FOLIAGE_COLORMAP = ResourceLocation.withDefaultNamespace("foliage_colormap");
		public static final ResourceLocation FONTS = ResourceLocation.withDefaultNamespace("fonts");
		public static final ResourceLocation GRASS_COLORMAP = ResourceLocation.withDefaultNamespace("grass_colormap");
		public static final ResourceLocation ATLAS = ResourceLocation.withDefaultNamespace("atlas");
		public static final ResourceLocation LANGUAGES = ResourceLocation.withDefaultNamespace("languages");
		public static final ResourceLocation MODELS = ResourceLocation.withDefaultNamespace("models");
		public static final ResourceLocation PARTICLES = ResourceLocation.withDefaultNamespace("particles");
		public static final ResourceLocation SHADERS = ResourceLocation.withDefaultNamespace("shaders");
		public static final ResourceLocation SOUNDS = ResourceLocation.withDefaultNamespace("sounds");
		public static final ResourceLocation SPLASH_TEXTS = ResourceLocation.withDefaultNamespace("splash_texts");
		public static final ResourceLocation TEXTURES = ResourceLocation.withDefaultNamespace("textures");
		public static final ResourceLocation WAYPOINT_STYLE_ASSETS = ResourceLocation.withDefaultNamespace("waypoint_style_assets");

		private Client() {
		}
	}

	/**
	 * Keys for various server resource reloaders.
	 */
	public static final class Server {
		public static final ResourceLocation ADVANCEMENTS = ResourceLocation.withDefaultNamespace("advancements");
		public static final ResourceLocation FUNCTIONS = ResourceLocation.withDefaultNamespace("functions");
		public static final ResourceLocation RECIPES = ResourceLocation.withDefaultNamespace("recipes");

		private Server() {
		}
	}
}
