package net.fabricmc.fabric.impl.tag.extension;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.tag.RequiredTagList;
import net.minecraft.tag.RequiredTagListRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

public final class FabricRequiredTagListRegistryImpl {
	private static Set<RequiredTagList<?>> fabricRequiredTags = new HashSet<>();

	public static <T> RequiredTagList<T> register(RegistryKey<? extends Registry<T>> registryKey, String path) {
		RequiredTagList<T> tagList = RequiredTagListRegistry.register(registryKey, path);
		fabricRequiredTags.add(tagList);
		return tagList;
	}

	public static Set<RequiredTagList<?>> getFabricRequiredTags() {
		return fabricRequiredTags;
	}
}
