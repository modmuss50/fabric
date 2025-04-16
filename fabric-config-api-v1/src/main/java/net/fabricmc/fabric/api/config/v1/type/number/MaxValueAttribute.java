package net.fabricmc.fabric.api.config.v1.type.number;

public interface MaxValueAttribute<T extends Number> extends NumberAttribute<T> {
	static <T extends Number & Comparable<T>> MaxValueAttribute<T> max(T maxValue) {
		return new MaxValueAttribute<>() {
			@Override
			public T getMaxValue() {
				return maxValue;
			}

			@Override
			public boolean test(T value) {
				return value.compareTo(maxValue) <= 0;
			}
		};
	}

	T getMaxValue();
}
