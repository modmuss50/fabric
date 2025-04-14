package net.fabricmc.fabric.api.config.v1.constraint;

import java.util.Collection;
import java.util.function.Predicate;

public interface Constraint<T> {
	static <T extends Number> NumericConstraint<T> numeric(Class<T> number) {
		return null;
	}

	static StringConstraint string() {
		return null;
	}

	static <T> ListConstraint<T> list(Class<T> entryType) {
		return null;
	}

	static <T> Constraint<T> generic(Class<T> type) {
		return null;
	}

	Constraint<T> values(Collection<T> values);

	Constraint<T> predicate(Predicate<T> predicate, String requirement);
}
