package net.fabricmc.fabric.api.config.v1;

public interface StringConfigValue extends ConfigValue<String> {
	StringConfigValue pattern(String pattern);

	StringConfigValue maxLength(int maxLength);

	StringConfigValue minLength(int minLength);
}
