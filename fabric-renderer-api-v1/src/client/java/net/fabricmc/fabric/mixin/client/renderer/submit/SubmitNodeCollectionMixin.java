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

package net.fabricmc.fabric.mixin.client.renderer.submit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MeshView;
import net.fabricmc.fabric.api.client.renderer.v1.render.FabricSubmitNodeCollection;
import net.fabricmc.fabric.impl.client.renderer.submit.BlockMeshSubmitRenderer;
import net.fabricmc.fabric.impl.client.renderer.submit.ItemMeshSubmitRenderer;

@Mixin(SubmitNodeCollection.class)
abstract class SubmitNodeCollectionMixin implements OrderedSubmitNodeCollector, FabricSubmitNodeCollection {
	@Shadow
	public abstract List<FeatureRenderPhase<?>> allPhases();

	@Unique
	private final List<ExtendedBlockModelSubmit> extendedBlockModelSubmits = new ArrayList<>();
	@Unique
	private final List<ExtendedItemSubmit> extendedItemSubmits = new ArrayList<>();

	@Override
	public void submitBlockModel(PoseStack poseStack, Function<ChunkSectionLayer, RenderType> renderTypeFunction, boolean translucent, List<BlockStateModelPart> parts, @Nullable Mesh mesh, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
		resetCompatibilitySubmitsIfNeeded();
		extendedBlockModelSubmits.add(new ExtendedBlockModelSubmit(poseStack.last().copy(), renderTypeFunction, translucent, parts, mesh, tintLayers, lightCoords, overlayCoords, outlineColor));

		for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
			RenderType renderType = renderTypeFunction.apply(layer);

			if (renderType == null) {
				continue;
			}

			submitCustomGeometry(poseStack, renderType, (pose, buffer) -> BlockMeshSubmitRenderer.renderLayer(parts, mesh, layer, tintLayers, lightCoords, overlayCoords, pose, buffer));

			if (outlineColor != 0) {
				RenderType outlineRenderType = getOutlineRenderType(renderType);

				if (outlineRenderType != null) {
					submitCustomGeometry(poseStack, outlineRenderType, (pose, buffer) -> BlockMeshSubmitRenderer.renderOutlineLayer(parts, mesh, layer, outlineColor, lightCoords, overlayCoords, pose, buffer));
				}
			}
		}
	}

	@Override
	public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, List<BakedQuad> quads, MeshView mesh, ItemStackRenderState.FoilType foilType) {
		resetCompatibilitySubmitsIfNeeded();
		extendedItemSubmits.add(new ExtendedItemSubmit(poseStack.last().copy(), displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, quads, mesh, foilType));
		submitItem(poseStack, displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, quads, foilType);
		submitItemMesh(poseStack, displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, mesh, foilType);
	}

	@Override
	public List<ExtendedBlockModelSubmit> getExtendedBlockModelSubmits() {
		return extendedBlockModelSubmits;
	}

	@Override
	public List<ExtendedItemSubmit> getExtendedItemSubmits() {
		return extendedItemSubmits;
	}

	@Unique
	private static @Nullable RenderType getOutlineRenderType(RenderType renderType) {
		if (renderType.isOutline()) {
			return renderType;
		}

		return renderType.outline().orElse(null);
	}

	@Unique
	private void submitItemMesh(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, MeshView mesh, ItemStackRenderState.FoilType foilType) {
		Set<RenderType> renderTypes = ItemMeshSubmitRenderer.collectRenderTypes(mesh);
		boolean hasFoil = ItemMeshSubmitRenderer.hasFoil(mesh, foilType);

		for (RenderType renderType : renderTypes) {
			submitCustomGeometry(poseStack, renderType, (pose, buffer) -> ItemMeshSubmitRenderer.renderLayer(mesh, renderType, tintLayers, lightCoords, overlayCoords, pose, buffer));

			if (outlineColor != 0) {
				RenderType outlineRenderType = getOutlineRenderType(renderType);

				if (outlineRenderType != null) {
					submitCustomGeometry(poseStack, outlineRenderType, (pose, buffer) -> ItemMeshSubmitRenderer.renderOutlineLayer(mesh, renderType, outlineColor, lightCoords, overlayCoords, pose, buffer));
				}
			}

			if (hasFoil) {
				submitCustomGeometry(poseStack, ItemMeshSubmitRenderer.getFoilRenderType(renderType), (pose, buffer) -> ItemMeshSubmitRenderer.renderFoilLayer(mesh, renderType, displayContext, tintLayers, lightCoords, overlayCoords, foilType, pose, buffer));
			}
		}
	}

	@Unique
	private void resetCompatibilitySubmitsIfNeeded() {
		if ((extendedBlockModelSubmits.isEmpty() && extendedItemSubmits.isEmpty()) || !allPhases().stream().allMatch(FeatureRenderPhase::isEmpty)) {
			return;
		}

		extendedBlockModelSubmits.clear();
		extendedItemSubmits.clear();
	}
}
