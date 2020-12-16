package net.fabricmc.fabric.api.tag;

import net.minecraft.tag.RequiredTagList;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import net.fabricmc.fabric.impl.tag.extension.FabricRequiredTagListRegistryImpl;

public final class FabricRequiredTagListRegistry {
	public static <T> RequiredTagList<T> register(RegistryKey<? extends Registry<T>> registryKey, String path) {
		return FabricRequiredTagListRegistryImpl.register(registryKey, path);
	}
}
