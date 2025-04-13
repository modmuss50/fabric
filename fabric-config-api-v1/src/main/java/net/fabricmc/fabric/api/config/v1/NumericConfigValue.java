package net.fabricmc.fabric.api.config.v1;

public interface NumericConfigValue<T extends Number> extends ConfigValue<T> {
	NumericConfigValue<T> min(T min);

	NumericConfigValue<T> max(T max);

	NumericConfigValue<T> step(T step);
}
