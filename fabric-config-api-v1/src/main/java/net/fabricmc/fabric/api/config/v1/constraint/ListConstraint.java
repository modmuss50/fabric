package net.fabricmc.fabric.api.config.v1.constraint;

import java.util.List;

public interface ListConstraint<T> extends Constraint<List<T>> {
	ListConstraint<T> maxLength(int maxLength);

	ListConstraint<T> entryConstraint(Constraint<T> constraintConsumer);
}
