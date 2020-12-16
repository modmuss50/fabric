package net.fabricmc.fabric.mixin.tag.extension;

import java.util.HashSet;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.tag.RequiredTagList;
import net.minecraft.tag.RequiredTagListRegistry;

import net.fabricmc.fabric.impl.tag.extension.FabricRequiredTagListRegistryImpl;

@Mixin(RequiredTagListRegistry.class)
public class MixinRequiredTagListRegistry {
	@Inject(method = "method_33153", at = @At("RETURN"), cancellable = true)
	private static void addFabricTags(CallbackInfoReturnable<Set<RequiredTagList<?>>> cir) {
		Set<RequiredTagList<?>> set = new HashSet<>(cir.getReturnValue());
		set.addAll(FabricRequiredTagListRegistryImpl.getFabricRequiredTags());
		cir.setReturnValue(set);
	}
}
