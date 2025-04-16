package net.fabricmc.fabric.api.config.v1.type.list;

import java.util.List;

public interface AccptedValuesListAttribute<T> extends ListAttribute<T> {
	static <T> AccptedValuesListAttribute<T> acceptedValues(Class<T> entryType, List<T> acceptedValues) {
		return new AccptedValuesListAttribute<T>() {
			@Override
			public Class<T> entryType() {
				return entryType;
			}

			@Override
			public List<T> getAcceptedValues() {
				return acceptedValues;
			}

			@Override
			public boolean test(List<T> ts) {
				return ts.stream().allMatch(acceptedValues::contains);
			}
		};
	}

	List<T> getAcceptedValues();
}
