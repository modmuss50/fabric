package net.fabricmc.fabric.api.config.v1.type.number;

public interface MinValueAttribute<T extends Number & Comparable<T>> extends NumberAttribute<T> {
	static <T extends Number & Comparable<T>> MinValueAttribute<T> min(T minValue) {
		return new MinValueAttribute<>() {
			@Override
			public T getMinValue() {
				return minValue;
			}

			@Override
			public boolean test(T value) {
				return value.compareTo(minValue) >= 0;
			}
		};
	}

	T getMinValue();
}
