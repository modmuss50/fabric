package net.fabricmc.fabric.api.config.v1.type.string;

import net.fabricmc.fabric.api.config.v1.type.ValueAttribute;

public interface PatternAttribute extends ValueAttribute<String> {
	static PatternAttribute of(String pattern) {
		return new PatternAttribute() {
			@Override
			public String getPattern() {
				return pattern;
			}

			@Override
			public boolean test(String value) {
				return value.matches(pattern);
			}
		};
	}

	static PatternAttribute uppercase() {
		return of("[A-Z]+");
	}

	static PatternAttribute lowercase() {
		return of("[a-z]+");
	}

	String getPattern();
}
