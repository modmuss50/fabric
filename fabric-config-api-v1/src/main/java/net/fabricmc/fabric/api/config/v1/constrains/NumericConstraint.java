package net.fabricmc.fabric.api.config.v1.constrains;

public interface NumericConstraint<T extends Number> extends Constraint<T> {
	static <T extends Number> NumericConstraint<T> create(Class<T> number) {
		return null;
	}

	NumericConstraint<T> min(T min);

	NumericConstraint<T> max(T max);

	default NumericConstraint<T> range(T min, T max) {
		return this.min(min).max(max);
	}

	NumericConstraint<T> step(T step);
}
