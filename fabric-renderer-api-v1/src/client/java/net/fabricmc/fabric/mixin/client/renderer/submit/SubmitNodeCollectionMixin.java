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
import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.MatrixUtil;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;

import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MeshView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.render.FabricSubmitNodeCollection;

@Mixin(SubmitNodeCollection.class)
abstract class SubmitNodeCollectionMixin implements OrderedSubmitNodeCollector, FabricSubmitNodeCollection {
	@Unique
	private static final Direction[] DIRECTIONS = Direction.values();

	@Unique
	private final List<ExtendedBlockModelSubmit> extendedBlockModelSubmits = new ArrayList<>();
	@Unique
	private final List<ExtendedItemSubmit> extendedItemSubmits = new ArrayList<>();

	@Override
	public void submitBlockModel(PoseStack poseStack, Function<ChunkSectionLayer, RenderType> renderTypeFunction, boolean translucent, List<BlockStateModelPart> parts, @Nullable Mesh mesh, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
		extendedBlockModelSubmits.add(new ExtendedBlockModelSubmit(poseStack.last().copy(), renderTypeFunction, translucent, parts, mesh, tintLayers, lightCoords, overlayCoords, outlineColor));

		for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
			RenderType renderType = renderTypeFunction.apply(layer);

			if (renderType == null) {
				continue;
			}

			submitCustomGeometry(poseStack, renderType, (pose, buffer) -> renderBlockLayer(parts, mesh, layer, tintLayers, lightCoords, overlayCoords, pose, buffer));

			if (outlineColor != 0) {
				RenderType outlineRenderType = getOutlineRenderType(renderType);

				if (outlineRenderType != null) {
					submitCustomGeometry(poseStack, outlineRenderType, (pose, buffer) -> renderBlockOutlineLayer(parts, mesh, layer, outlineColor, lightCoords, overlayCoords, pose, buffer));
				}
			}
		}
	}

	@Override
	public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, List<BakedQuad> quads, MeshView mesh, ItemStackRenderState.FoilType foilType) {
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
	private static void renderBlockLayer(List<BlockStateModelPart> parts, @Nullable Mesh mesh, ChunkSectionLayer layer, int[] tintLayers, int lightCoords, int overlayCoords, PoseStack.Pose pose, VertexConsumer buffer) {
		QuadInstance quadInstance = new QuadInstance();
		quadInstance.setLightCoords(lightCoords);
		quadInstance.setOverlayCoords(overlayCoords);

		for (BlockStateModelPart part : parts) {
			renderPartLayer(part, layer, tintLayers, pose, buffer, quadInstance);
		}

		if (mesh != null) {
			QuadEmitter emitter = Renderer.get().quadEmitter(quad -> {
				if (quad.chunkLayer() != layer) {
					return;
				}

				if (quad.emissive()) {
					quad.lightmap(LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT);
				} else {
					quad.minLightmap(lightCoords);
				}

				int tintIndex = quad.tintIndex();

				if (tintIndex != -1 && tintIndex < tintLayers.length) {
					quad.multiplyColor(tintLayers[tintIndex]);
				}

				quad.buffer(overlayCoords, pose, buffer);
			});
			mesh.outputTo(emitter);
		}
	}

	@Unique
	private static void renderBlockOutlineLayer(List<BlockStateModelPart> parts, @Nullable Mesh mesh, ChunkSectionLayer layer, int outlineColor, int lightCoords, int overlayCoords, PoseStack.Pose pose, VertexConsumer buffer) {
		QuadInstance quadInstance = new QuadInstance();
		quadInstance.setLightCoords(lightCoords);
		quadInstance.setOverlayCoords(overlayCoords);
		quadInstance.setColor(outlineColor);

		for (BlockStateModelPart part : parts) {
			renderPartOutlineLayer(part, layer, pose, buffer, quadInstance);
		}

		if (mesh != null) {
			QuadEmitter emitter = Renderer.get().quadEmitter(quad -> {
				if (quad.chunkLayer() != layer) {
					return;
				}

				if (quad.emissive()) {
					quad.lightmap(LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT);
				} else {
					quad.minLightmap(lightCoords);
				}

				quad.color(outlineColor, outlineColor, outlineColor, outlineColor);
				quad.buffer(overlayCoords, pose, buffer);
			});
			mesh.outputTo(emitter);
		}
	}

	@Unique
	private static void renderPartLayer(BlockStateModelPart part, ChunkSectionLayer layer, int[] tintLayers, PoseStack.Pose pose, VertexConsumer buffer, QuadInstance quadInstance) {
		for (Direction direction : DIRECTIONS) {
			for (BakedQuad quad : part.getQuads(direction)) {
				renderQuadLayer(quad, layer, tintLayers, pose, buffer, quadInstance);
			}
		}

		for (BakedQuad quad : part.getQuads(null)) {
			renderQuadLayer(quad, layer, tintLayers, pose, buffer, quadInstance);
		}
	}

	@Unique
	private static void renderPartOutlineLayer(BlockStateModelPart part, ChunkSectionLayer layer, PoseStack.Pose pose, VertexConsumer buffer, QuadInstance quadInstance) {
		for (Direction direction : DIRECTIONS) {
			for (BakedQuad quad : part.getQuads(direction)) {
				renderQuadOutlineLayer(quad, layer, pose, buffer, quadInstance);
			}
		}

		for (BakedQuad quad : part.getQuads(null)) {
			renderQuadOutlineLayer(quad, layer, pose, buffer, quadInstance);
		}
	}

	@Unique
	private static void renderQuadLayer(BakedQuad quad, ChunkSectionLayer layer, int[] tintLayers, PoseStack.Pose pose, VertexConsumer buffer, QuadInstance quadInstance) {
		if (quad.materialInfo().layer() != layer) {
			return;
		}

		int tintIndex = quad.materialInfo().tintIndex();
		boolean useTintLayer = tintIndex != -1 && tintIndex < tintLayers.length;
		quadInstance.setColor(useTintLayer ? tintLayers[tintIndex] : -1);
		buffer.putBakedQuad(pose, quad, quadInstance);
	}

	@Unique
	private static void renderQuadOutlineLayer(BakedQuad quad, ChunkSectionLayer layer, PoseStack.Pose pose, VertexConsumer buffer, QuadInstance quadInstance) {
		if (quad.materialInfo().layer() != layer) {
			return;
		}

		buffer.putBakedQuad(pose, quad, quadInstance);
	}

	@Unique
	private void submitItemMesh(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, MeshView mesh, ItemStackRenderState.FoilType foilType) {
		List<RenderType> renderTypes = new ArrayList<>();

		mesh.forEach(quad -> {
			RenderType renderType = quad.itemRenderType();

			if (!renderTypes.contains(renderType)) {
				renderTypes.add(renderType);
			}
		});

		for (RenderType renderType : renderTypes) {
			submitCustomGeometry(poseStack, renderType, (pose, buffer) -> renderItemMeshLayer(mesh, renderType, tintLayers, lightCoords, overlayCoords, pose, buffer));

			if (outlineColor != 0) {
				RenderType outlineRenderType = getOutlineRenderType(renderType);

				if (outlineRenderType != null) {
					submitCustomGeometry(poseStack, outlineRenderType, (pose, buffer) -> renderItemMeshOutlineLayer(mesh, renderType, outlineColor, lightCoords, overlayCoords, pose, buffer));
				}
			}

			submitCustomGeometry(poseStack, getFoilRenderType(renderType), (pose, buffer) -> renderItemMeshFoilLayer(mesh, renderType, displayContext, tintLayers, lightCoords, overlayCoords, foilType, pose, buffer));
		}
	}

	@Unique
	private static void renderItemMeshLayer(MeshView mesh, RenderType renderType, int[] tintLayers, int lightCoords, int overlayCoords, PoseStack.Pose pose, VertexConsumer buffer) {
		QuadEmitter emitter = Renderer.get().quadEmitter(quad -> {
			if (quad.itemRenderType() != renderType) {
				return;
			}

			applyItemLighting(quad, lightCoords);
			applyItemTint(quad, tintLayers);
			quad.buffer(overlayCoords, pose, buffer);
		});
		mesh.outputTo(emitter);
	}

	@Unique
	private static void renderItemMeshOutlineLayer(MeshView mesh, RenderType renderType, int outlineColor, int lightCoords, int overlayCoords, PoseStack.Pose pose, VertexConsumer buffer) {
		QuadEmitter emitter = Renderer.get().quadEmitter(quad -> {
			if (quad.itemRenderType() != renderType) {
				return;
			}

			applyItemLighting(quad, lightCoords);
			quad.color(outlineColor, outlineColor, outlineColor, outlineColor);
			quad.buffer(overlayCoords, pose, buffer);
		});
		mesh.outputTo(emitter);
	}

	@Unique
	private static void renderItemMeshFoilLayer(MeshView mesh, RenderType renderType, ItemDisplayContext displayContext, int[] tintLayers, int lightCoords, int overlayCoords, ItemStackRenderState.FoilType defaultFoilType, PoseStack.Pose pose, VertexConsumer buffer) {
		PoseStack.Pose specialFoilPose = computeFoilDecalPose(displayContext, pose);
		VertexConsumer specialFoilBuffer = new SheetedDecalTextureGenerator(buffer, specialFoilPose, 0.0078125F);
		QuadEmitter emitter = Renderer.get().quadEmitter(quad -> {
			if (quad.itemRenderType() != renderType) {
				return;
			}

			ItemStackRenderState.FoilType foilType = quad.foilType() == null ? defaultFoilType : quad.foilType();

			if (foilType == ItemStackRenderState.FoilType.NONE) {
				return;
			}

			applyItemLighting(quad, lightCoords);
			applyItemTint(quad, tintLayers);
			quad.buffer(overlayCoords, pose, foilType == ItemStackRenderState.FoilType.SPECIAL ? specialFoilBuffer : buffer);
		});
		mesh.outputTo(emitter);
	}

	@Unique
	private static void applyItemLighting(net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView quad, int lightCoords) {
		if (quad.emissive()) {
			quad.lightmap(LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT);
		} else {
			quad.minLightmap(lightCoords);
		}
	}

	@Unique
	private static void applyItemTint(net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView quad, int[] tintLayers) {
		int tintIndex = quad.tintIndex();

		if (tintIndex >= 0 && tintIndex < tintLayers.length) {
			quad.multiplyColor(tintLayers[tintIndex]);
		}
	}

	@Unique
	private static RenderType getFoilRenderType(RenderType renderType) {
		return useTransparentGlint(renderType) ? RenderTypes.glintTranslucent() : RenderTypes.glint();
	}

	@Unique
	private static PoseStack.Pose computeFoilDecalPose(ItemDisplayContext displayContext, PoseStack.Pose pose) {
		PoseStack.Pose foilDecalPose = pose.copy();

		if (displayContext == ItemDisplayContext.GUI) {
			MatrixUtil.mulComponentWise(foilDecalPose.pose(), 0.5F);
		} else if (displayContext.firstPerson()) {
			MatrixUtil.mulComponentWise(foilDecalPose.pose(), 0.75F);
		}

		return foilDecalPose;
	}

	@Unique
	private static boolean useTransparentGlint(RenderType renderType) {
		return Minecraft.getInstance().gameRenderer.gameRenderState().useShaderTransparency() && renderType.outputTarget() == OutputTarget.ITEM_ENTITY_TARGET;
	}
}
