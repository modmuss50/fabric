package net.fabricmc.fabric.api.config.v1.type.list;

import net.fabricmc.fabric.api.config.v1.type.ValueAttribute;

import java.util.List;

public interface ListAttribute<T> extends ValueAttribute<List<T>> {
	static <T> ListAttribute<T> type(Class<T> type) {
		return () -> type;
	}

	Class<T> entryType();
}
