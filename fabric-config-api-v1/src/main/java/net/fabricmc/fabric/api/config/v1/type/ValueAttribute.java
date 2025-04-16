package net.fabricmc.fabric.api.config.v1.type;

import java.util.function.Predicate;

public interface ValueAttribute<T> extends Predicate<T> {
	@Override
	default boolean test(T t) {
		return true;
	}
}
