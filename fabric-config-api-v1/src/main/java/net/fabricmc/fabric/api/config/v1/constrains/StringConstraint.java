package net.fabricmc.fabric.api.config.v1.constrains;

public interface StringConstraint extends Constraint<String> {
	static StringConstraint create() {
		return null;
	}

	StringConstraint pattern(String pattern);

	default StringConstraint lowercase() {
		return pattern("[a-z]*");
	}

	default StringConstraint uppercase() {
		return pattern("[A-Z]*");
	}

	StringConstraint maxLength(int maxLength);

	StringConstraint minLength(int minLength);
}
