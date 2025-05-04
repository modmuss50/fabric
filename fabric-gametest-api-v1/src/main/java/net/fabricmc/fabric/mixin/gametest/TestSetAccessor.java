package net.fabricmc.fabric.mixin.gametest;

import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.test.GameTestState;
import net.minecraft.test.TestSet;

@Mixin(TestSet.class)
public interface TestSetAccessor {
	@Accessor
	Collection<GameTestState> getTests();
}
