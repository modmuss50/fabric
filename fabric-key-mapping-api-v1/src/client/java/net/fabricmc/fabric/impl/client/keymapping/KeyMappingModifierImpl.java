/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.impl.client.keymapping;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyModifier;
import net.fabricmc.fabric.mixin.client.keymapping.KeyMappingAccessor;

public final class KeyMappingModifierImpl {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int ALL_MODIFIERS = (1 << KeyModifier.values().length) - 1;
	private static final Map<KeyMapping, ModifierState> MODIFIERS = new IdentityHashMap<>();
	private static final ThreadLocal<Integer> EVENT_MODIFIERS = new ThreadLocal<>();
	private static final ThreadLocal<Integer> EVENT_ACTION = new ThreadLocal<>();
	private static final ThreadLocal<Set<KeyMapping>> EVENT_RELEASED_ARMED = new ThreadLocal<>();

	private KeyMappingModifierImpl() {
	}

	public static void registerDefaultModifiers(KeyMapping keyMapping, KeyModifier... modifiers) {
		Objects.requireNonNull(keyMapping, "key mapping cannot be null");
		Objects.requireNonNull(modifiers, "modifiers cannot be null");

		int mask = 0;

		for (KeyModifier modifier : modifiers) {
			mask |= bit(Objects.requireNonNull(modifier, "modifier cannot be null"));
		}

		mask = withoutPrimaryModifier(mask, keyMapping.getDefaultKey());

		if (mask == 0) {
			MODIFIERS.remove(keyMapping);
		} else {
			MODIFIERS.put(keyMapping, new ModifierState(mask, mask));
		}
	}

	public static EnumSet<KeyModifier> getBoundModifiers(KeyMapping keyMapping) {
		int mask = getModifierMask(keyMapping);
		EnumSet<KeyModifier> modifiers = EnumSet.noneOf(KeyModifier.class);

		for (KeyModifier modifier : KeyModifier.values()) {
			if ((mask & bit(modifier)) != 0) {
				modifiers.add(modifier);
			}
		}

		return modifiers;
	}

	public static int getModifierMask(KeyMapping keyMapping) {
		ModifierState state = MODIFIERS.get(keyMapping);
		return state == null ? 0 : state.modifiers;
	}

	public static int getDefaultModifierMask(KeyMapping keyMapping) {
		ModifierState state = MODIFIERS.get(keyMapping);
		return state == null ? 0 : state.defaultModifiers;
	}

	public static void setModifierMask(KeyMapping keyMapping, int modifiers) {
		checkMask(modifiers);
		modifiers = withoutPrimaryModifier(modifiers, boundKey(keyMapping));
		ModifierState state = MODIFIERS.get(keyMapping);
		int defaultModifiers = state == null ? 0 : state.defaultModifiers;

		if (state != null) {
			state.armed = false;
			state.dispatched = false;
		}

		if (defaultModifiers == 0 && modifiers == 0) {
			MODIFIERS.remove(keyMapping);
		} else if (state == null) {
			MODIFIERS.put(keyMapping, new ModifierState(defaultModifiers, modifiers));
		} else {
			state.modifiers = (byte) modifiers;
		}
	}

	public static void onSetKey(KeyMapping keyMapping, InputConstants.Key key) {
		setModifierMask(keyMapping, key.equals(keyMapping.getDefaultKey()) ? getDefaultModifierMask(keyMapping) : 0);
	}

	public static boolean shouldProcess(KeyMapping keyMapping, InputConstants.Key primary, boolean release) {
		ModifierState state = MODIFIERS.get(keyMapping);

		if (state == null) {
			return true;
		}

		if (release) {
			state.armed = false;
			state.dispatched = false;
			return true;
		}

		if (state.modifiers == 0) {
			return true;
		}

		boolean matches = state.modifiers == modifierMask(currentRawModifiers(), primary);
		Integer eventAction = EVENT_ACTION.get();

		if (eventAction == null) {
			return matches;
		}

		if (eventAction == InputConstants.PRESS) {
			state.armed = matches;
			state.dispatched = matches;
			return matches;
		}

		if (!matches) {
			state.armed = false;
			state.dispatched = false;
		}

		return eventAction != InputConstants.RELEASE && matches && state.armed && state.dispatched;
	}

	public static boolean matches(KeyMapping keyMapping, int rawModifiers) {
		int modifiers = getModifierMask(keyMapping);
		return modifiers == 0 || modifiers == modifierMask(rawModifiers, boundKey(keyMapping));
	}

	public static boolean matchesInputEvent(KeyMapping keyMapping, int rawModifiers) {
		if (!matches(keyMapping, rawModifiers)) {
			return false;
		}

		ModifierState state = MODIFIERS.get(keyMapping);
		Integer eventAction = EVENT_ACTION.get();

		if (state == null || state.modifiers == 0 || eventAction == null) {
			return true;
		}

		if (eventAction == InputConstants.RELEASE) {
			Set<KeyMapping> releasedArmed = EVENT_RELEASED_ARMED.get();
			return releasedArmed != null && releasedArmed.contains(keyMapping);
		}

		return state.armed;
	}

	public static boolean matchesMouseInputEvent(KeyMapping keyMapping, int rawModifiers) {
		if (!matches(keyMapping, rawModifiers)) {
			return false;
		}

		ModifierState state = MODIFIERS.get(keyMapping);
		Integer eventAction = EVENT_ACTION.get();

		if (state == null || state.modifiers == 0 || eventAction == null) {
			return true;
		}

		if (eventAction == InputConstants.PRESS) {
			state.armed = true;
			state.dispatched = false;
			return true;
		}

		if (eventAction == InputConstants.RELEASE) {
			Set<KeyMapping> releasedArmed = EVENT_RELEASED_ARMED.get();
			boolean wasArmed = releasedArmed != null && releasedArmed.contains(keyMapping);

			if (state.armed) {
				wasArmed = true;
				state.armed = false;
				state.dispatched = false;
				markReleasedArmed(keyMapping);
			}

			return wasArmed;
		}

		return state.armed;
	}

	public static boolean matchesCurrentModifiers(KeyMapping keyMapping) {
		int modifiers = getModifierMask(keyMapping);
		return modifiers == 0 || modifiers == modifierMask(currentRawModifiers(), boundKey(keyMapping));
	}

	public static boolean matchesCurrentInputEvent(KeyMapping keyMapping) {
		return matchesInputEvent(keyMapping, currentRawModifiers());
	}

	public static boolean matchesArmedCurrentModifiers(KeyMapping keyMapping) {
		if (!matchesCurrentModifiers(keyMapping)) {
			return false;
		}

		ModifierState state = MODIFIERS.get(keyMapping);
		return state == null || state.modifiers == 0 || EVENT_ACTION.get() == null || state.armed;
	}

	public static boolean same(KeyMapping keyMapping, KeyMapping other, boolean vanillaSame) {
		if (!vanillaSame) {
			return false;
		}

		int modifiers = getModifierMask(keyMapping);
		int otherModifiers = getModifierMask(other);
		return modifiers == 0 || otherModifiers == 0 || modifiers == otherModifiers;
	}

	public static boolean haveSameModifiers(KeyMapping keyMapping, KeyMapping other, boolean vanillaSame) {
		return vanillaSame && getModifierMask(keyMapping) == getModifierMask(other);
	}

	public static boolean isDefault(KeyMapping keyMapping, boolean vanillaDefault) {
		return vanillaDefault && getModifierMask(keyMapping) == getDefaultModifierMask(keyMapping);
	}

	public static Component getTranslatedKeyMessage(KeyMapping keyMapping, Component vanillaMessage) {
		int modifiers = getModifierMask(keyMapping);

		if (modifiers == 0) {
			return vanillaMessage;
		}

		Component message = vanillaMessage;
		KeyModifier[] values = KeyModifier.values();

		for (int i = values.length - 1; i >= 0; i--) {
			KeyModifier modifier = values[i];

			if ((modifiers & bit(modifier)) != 0) {
				message = Component.translatable(
						"fabric.key_modifier.combination",
						Component.translatable("fabric.key_modifier." + modifier.name().toLowerCase(Locale.ROOT)),
						message
				);
			}
		}

		return message;
	}

	public static int modifierMask(int rawModifiers, InputConstants.Key primary) {
		int modifiers = 0;

		if ((rawModifiers & InputConstants.MOD_SHIFT) != 0) {
			modifiers |= bit(KeyModifier.SHIFT);
		}

		if ((rawModifiers & InputConstants.MOD_CONTROL) != 0) {
			modifiers |= bit(KeyModifier.CONTROL);
		}

		if ((rawModifiers & InputConstants.MOD_ALT) != 0) {
			modifiers |= bit(KeyModifier.ALT);
		}

		if ((rawModifiers & InputConstants.MOD_SUPER) != 0) {
			modifiers |= bit(KeyModifier.SUPER);
		}

		return withoutPrimaryModifier(modifiers, primary);
	}

	public static int modifierForKey(InputConstants.Key key) {
		if (key.getType() != InputConstants.Type.KEYBOARD) {
			return 0;
		}

		return switch (key.getValue()) {
		case InputConstants.KEY_LSHIFT, InputConstants.KEY_RSHIFT -> bit(KeyModifier.SHIFT);
		case InputConstants.KEY_LCONTROL, InputConstants.KEY_RCONTROL -> bit(KeyModifier.CONTROL);
		case InputConstants.KEY_LALT, InputConstants.KEY_RALT -> bit(KeyModifier.ALT);
		case InputConstants.KEY_LGUI, InputConstants.KEY_RGUI -> bit(KeyModifier.SUPER);
		default -> 0;
		};
	}

	public static boolean isModifierKey(InputConstants.Key key) {
		return modifierForKey(key) != 0;
	}

	public static void withInputEvent(int rawModifiers, int eventAction, Runnable action) {
		Objects.requireNonNull(action, "action cannot be null");
		Integer previousModifiers = EVENT_MODIFIERS.get();
		Integer previousAction = EVENT_ACTION.get();
		Set<KeyMapping> previousReleasedArmed = EVENT_RELEASED_ARMED.get();
		EVENT_MODIFIERS.set(rawModifiers);
		EVENT_ACTION.set(eventAction);
		EVENT_RELEASED_ARMED.remove();

		try {
			action.run();
		} finally {
			if (previousModifiers == null) {
				EVENT_MODIFIERS.remove();
			} else {
				EVENT_MODIFIERS.set(previousModifiers);
			}

			if (previousAction == null) {
				EVENT_ACTION.remove();
			} else {
				EVENT_ACTION.set(previousAction);
			}

			if (previousReleasedArmed == null) {
				EVENT_RELEASED_ARMED.remove();
			} else {
				EVENT_RELEASED_ARMED.set(previousReleasedArmed);
			}
		}
	}

	public static void prepareInputEvent(InputConstants.Key primary, int rawModifiers, int eventAction) {
		releaseMismatchedMappings(rawModifiers);

		if (eventAction != InputConstants.PRESS && eventAction != InputConstants.RELEASE) {
			return;
		}

		for (Map.Entry<KeyMapping, ModifierState> entry : MODIFIERS.entrySet()) {
			if (!boundKey(entry.getKey()).equals(primary)) {
				continue;
			}

			ModifierState state = entry.getValue();

			if (eventAction == InputConstants.PRESS) {
				state.armed = state.modifiers != 0
						&& state.modifiers == modifierMask(rawModifiers, primary);
				state.dispatched = false;
			} else {
				if (state.armed) {
					markReleasedArmed(entry.getKey());
				}

				state.armed = false;
				state.dispatched = false;
			}
		}
	}

	public static void releaseMismatchedMappings(int rawModifiers) {
		for (Map.Entry<KeyMapping, ModifierState> entry : MODIFIERS.entrySet()) {
			KeyMapping keyMapping = entry.getKey();
			ModifierState state = entry.getValue();

			if (state.modifiers != 0 && !matches(keyMapping, rawModifiers)) {
				state.armed = false;
				state.dispatched = false;

				if (keyMapping.isDown()) {
					keyMapping.setDown(false);
				}
			}
		}
	}

	public static String serializeOverrides(KeyMapping[] keyMappings) {
		JsonObject overrides = new JsonObject();

		for (KeyMapping keyMapping : keyMappings) {
			int modifiers = getModifierMask(keyMapping);

			if (modifiers == implicitModifierMask(keyMapping)) {
				continue;
			}

			JsonArray names = new JsonArray();

			for (KeyModifier modifier : KeyModifier.values()) {
				if ((modifiers & bit(modifier)) != 0) {
					names.add(modifier.name().toLowerCase(Locale.ROOT));
				}
			}

			overrides.add(keyMapping.getName(), names);
		}

		return overrides.isEmpty() ? "" : overrides.toString();
	}

	public static void applyOverrides(String serialized, KeyMapping[] keyMappings) {
		try {
			Map<String, KeyMapping> keyMappingsByName = new HashMap<>();

			for (KeyMapping keyMapping : keyMappings) {
				resetToImplicit(keyMapping);
				keyMappingsByName.put(keyMapping.getName(), keyMapping);
			}

			if (serialized == null || serialized.isBlank()) {
				return;
			}

			JsonElement parsed = JsonParser.parseString(serialized);

			if (!parsed.isJsonObject()) {
				LOGGER.warn("Ignoring fabricKeyModifiers option because it is not a JSON object");
				return;
			}

			for (Map.Entry<String, JsonElement> entry : parsed.getAsJsonObject().entrySet()) {
				KeyMapping keyMapping = keyMappingsByName.get(entry.getKey());

				if (keyMapping == null) {
					LOGGER.warn("Ignoring modifiers for unknown key mapping '{}'", entry.getKey());
					continue;
				}

				Integer modifiers = parseModifiers(entry.getKey(), entry.getValue());

				if (modifiers != null) {
					setModifierMask(keyMapping, modifiers);
				}
			}
		} catch (RuntimeException exception) {
			LOGGER.warn("Ignoring malformed fabricKeyModifiers option", exception);
		}
	}

	private static Integer parseModifiers(String keyMappingName, JsonElement element) {
		if (!element.isJsonArray()) {
			LOGGER.warn("Ignoring invalid modifiers for key mapping '{}': expected an array", keyMappingName);
			return null;
		}

		int modifiers = 0;

		for (JsonElement nameElement : element.getAsJsonArray()) {
			if (!nameElement.isJsonPrimitive() || !nameElement.getAsJsonPrimitive().isString()) {
				LOGGER.warn("Ignoring invalid modifiers for key mapping '{}': expected modifier names", keyMappingName);
				return null;
			}

			String name = nameElement.getAsString();

			try {
				modifiers |= bit(KeyModifier.valueOf(name.toUpperCase(Locale.ROOT)));
			} catch (IllegalArgumentException exception) {
				LOGGER.warn("Ignoring invalid modifier '{}' for key mapping '{}'", name, keyMappingName);
				return null;
			}
		}

		return modifiers;
	}

	private static void resetToImplicit(KeyMapping keyMapping) {
		setModifierMask(keyMapping, implicitModifierMask(keyMapping));
	}

	private static int implicitModifierMask(KeyMapping keyMapping) {
		return boundKey(keyMapping).equals(keyMapping.getDefaultKey()) ? getDefaultModifierMask(keyMapping) : 0;
	}

	private static int currentRawModifiers() {
		Integer eventModifiers = EVENT_MODIFIERS.get();

		if (eventModifiers != null) {
			return eventModifiers;
		}

		int modifiers = 0;

		if (InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT)) {
			modifiers |= InputConstants.MOD_SHIFT;
		}

		if (InputConstants.isKeyDown(InputConstants.KEY_LCONTROL) || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL)) {
			modifiers |= InputConstants.MOD_CONTROL;
		}

		if (InputConstants.isKeyDown(InputConstants.KEY_LALT) || InputConstants.isKeyDown(InputConstants.KEY_RALT)) {
			modifiers |= InputConstants.MOD_ALT;
		}

		if (InputConstants.isKeyDown(InputConstants.KEY_LGUI) || InputConstants.isKeyDown(InputConstants.KEY_RGUI)) {
			modifiers |= InputConstants.MOD_SUPER;
		}

		return modifiers;
	}

	private static void markReleasedArmed(KeyMapping keyMapping) {
		Set<KeyMapping> releasedArmed = EVENT_RELEASED_ARMED.get();

		if (releasedArmed == null) {
			releasedArmed = Collections.newSetFromMap(new IdentityHashMap<>());
			EVENT_RELEASED_ARMED.set(releasedArmed);
		}

		releasedArmed.add(keyMapping);
	}

	private static InputConstants.Key boundKey(KeyMapping keyMapping) {
		return ((KeyMappingAccessor) keyMapping).fabric_getBoundKey();
	}

	private static int withoutPrimaryModifier(int modifiers, InputConstants.Key primary) {
		return modifiers & ~modifierForKey(primary);
	}

	private static int bit(KeyModifier modifier) {
		return 1 << modifier.ordinal();
	}

	private static void checkMask(int modifiers) {
		if ((modifiers & ~ALL_MODIFIERS) != 0) {
			throw new IllegalArgumentException("Unknown key modifier bits: " + modifiers);
		}
	}

	private static final class ModifierState {
		private final byte defaultModifiers;
		private byte modifiers;
		// Armed means the physical primary press matched; dispatched means that press reached static KeyMapping dispatch.
		private boolean armed;
		private boolean dispatched;

		private ModifierState(int defaultModifiers, int modifiers) {
			this.defaultModifiers = (byte) defaultModifiers;
			this.modifiers = (byte) modifiers;
		}
	}
}
