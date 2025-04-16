package net.fabricmc.fabric.api.config.v1.type.list;

import java.util.List;

public interface MaxLengthListAttribute<T> extends ListAttribute<T> {
	static <T> MaxLengthListAttribute<T> maxLength(Class<T> entryType, int maxLength) {
		return new MaxLengthListAttribute<T>() {
			@Override
			public int maxLength() {
				return maxLength;
			}

			@Override
			public Class<T> entryType() {
				return entryType;
			}

			@Override
			public boolean test(List<T> list) {
				return list.size() <= maxLength;
			}
		};
	}

	int maxLength();
}
