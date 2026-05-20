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

import java.util.LinkedHashSet;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.MatrixUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;

import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MeshView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;

public final class ItemMeshSubmitRenderer {
	private ItemMeshSubmitRenderer() {
	}

	public static Set<RenderType> collectRenderTypes(MeshView mesh) {
		Set<RenderType> renderTypes = new LinkedHashSet<>();
		mesh.forEach(quad -> renderTypes.add(quad.itemRenderType()));
		return renderTypes;
	}

	public static boolean hasFoil(MeshView mesh, ItemStackRenderState.FoilType defaultFoilType) {
		final boolean[] hasFoil = new boolean[1];
		mesh.forEach(quad -> {
			if (foilType(quad, defaultFoilType) != ItemStackRenderState.FoilType.NONE) {
				hasFoil[0] = true;
			}
		});
		return hasFoil[0];
	}

	public static void renderLayer(MeshView mesh, RenderType renderType, int[] tintLayers, int lightCoords, int overlayCoords, PoseStack.Pose pose, VertexConsumer buffer) {
		QuadEmitter emitter = Renderer.get().quadEmitter(quad -> {
			if (quad.itemRenderType() != renderType) {
				return;
			}

			applyLighting(quad, lightCoords);
			applyTint(quad, tintLayers);
			quad.buffer(overlayCoords, pose, buffer);
		});
		mesh.outputTo(emitter);
	}

	public static void renderOutlineLayer(MeshView mesh, RenderType renderType, int outlineColor, int lightCoords, int overlayCoords, PoseStack.Pose pose, VertexConsumer buffer) {
		QuadEmitter emitter = Renderer.get().quadEmitter(quad -> {
			if (quad.itemRenderType() != renderType) {
				return;
			}

			applyLighting(quad, lightCoords);
			quad.color(outlineColor, outlineColor, outlineColor, outlineColor);
			quad.buffer(overlayCoords, pose, buffer);
		});
		mesh.outputTo(emitter);
	}

	public static void renderFoilLayer(MeshView mesh, RenderType renderType, ItemDisplayContext displayContext, int[] tintLayers, int lightCoords, int overlayCoords, ItemStackRenderState.FoilType defaultFoilType, PoseStack.Pose pose, VertexConsumer buffer) {
		PoseStack.Pose specialFoilPose = computeFoilDecalPose(displayContext, pose);
		VertexConsumer specialFoilBuffer = new SheetedDecalTextureGenerator(buffer, specialFoilPose, 0.0078125F);
		QuadEmitter emitter = Renderer.get().quadEmitter(quad -> {
			if (quad.itemRenderType() != renderType) {
				return;
			}

			ItemStackRenderState.FoilType foilType = foilType(quad, defaultFoilType);

			if (foilType == ItemStackRenderState.FoilType.NONE) {
				return;
			}

			applyLighting(quad, lightCoords);
			applyTint(quad, tintLayers);
			quad.buffer(overlayCoords, pose, foilType == ItemStackRenderState.FoilType.SPECIAL ? specialFoilBuffer : buffer);
		});
		mesh.outputTo(emitter);
	}

	public static RenderType getFoilRenderType(RenderType renderType) {
		return useTransparentGlint(renderType) ? RenderTypes.glintTranslucent() : RenderTypes.glint();
	}

	private static void applyLighting(MutableQuadView quad, int lightCoords) {
		if (quad.emissive()) {
			quad.lightmap(LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT);
		} else {
			quad.minLightmap(lightCoords);
		}
	}

	private static void applyTint(MutableQuadView quad, int[] tintLayers) {
		int tintIndex = quad.tintIndex();

		if (tintIndex >= 0 && tintIndex < tintLayers.length) {
			quad.multiplyColor(tintLayers[tintIndex]);
		}
	}

	private static ItemStackRenderState.FoilType foilType(QuadView quad, ItemStackRenderState.FoilType defaultFoilType) {
		return quad.foilType() == null ? defaultFoilType : quad.foilType();
	}

	private static PoseStack.Pose computeFoilDecalPose(ItemDisplayContext displayContext, PoseStack.Pose pose) {
		PoseStack.Pose foilDecalPose = pose.copy();

		if (displayContext == ItemDisplayContext.GUI) {
			MatrixUtil.mulComponentWise(foilDecalPose.pose(), 0.5F);
		} else if (displayContext.firstPerson()) {
			MatrixUtil.mulComponentWise(foilDecalPose.pose(), 0.75F);
		}

		return foilDecalPose;
	}

	private static boolean useTransparentGlint(RenderType renderType) {
		return Minecraft.getInstance().gameRenderer.gameRenderState().useShaderTransparency() && renderType.outputTarget() == OutputTarget.ITEM_ENTITY_TARGET;
	}
}
