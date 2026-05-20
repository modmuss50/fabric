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

package net.fabricmc.fabric.impl.client.renderer.submit;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;

import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;

public final class BlockMeshSubmitRenderer {
	private static final Direction[] DIRECTIONS = Direction.values();

	private BlockMeshSubmitRenderer() {
	}

	public static void renderLayer(List<BlockStateModelPart> parts, @Nullable Mesh mesh, ChunkSectionLayer layer, int[] tintLayers, int lightCoords, int overlayCoords, PoseStack.Pose pose, VertexConsumer buffer) {
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

	public static void renderOutlineLayer(List<BlockStateModelPart> parts, @Nullable Mesh mesh, ChunkSectionLayer layer, int outlineColor, int lightCoords, int overlayCoords, PoseStack.Pose pose, VertexConsumer buffer) {
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

	private static void renderQuadLayer(BakedQuad quad, ChunkSectionLayer layer, int[] tintLayers, PoseStack.Pose pose, VertexConsumer buffer, QuadInstance quadInstance) {
		if (quad.materialInfo().layer() != layer) {
			return;
		}

		int tintIndex = quad.materialInfo().tintIndex();
		quadInstance.setColor(tintIndex != -1 && tintIndex < tintLayers.length ? tintLayers[tintIndex] : -1);
		buffer.putBakedQuad(pose, quad, quadInstance);
	}

	private static void renderQuadOutlineLayer(BakedQuad quad, ChunkSectionLayer layer, PoseStack.Pose pose, VertexConsumer buffer, QuadInstance quadInstance) {
		if (quad.materialInfo().layer() != layer) {
			return;
		}

		buffer.putBakedQuad(pose, quad, quadInstance);
	}
}
