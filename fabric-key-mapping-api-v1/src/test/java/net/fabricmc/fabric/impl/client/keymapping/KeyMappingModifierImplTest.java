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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.mojang.blaze3d.platform.InputConstants;
import org.junit.jupiter.api.Test;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyModifier;
import net.fabricmc.fabric.mixin.client.keymapping.KeyMappingAccessor;

class KeyMappingModifierImplTest {
	private static final int SHIFT = 1 << KeyModifier.SHIFT.ordinal();
	private static final int CONTROL = 1 << KeyModifier.CONTROL.ordinal();
	private static final int ALT = 1 << KeyModifier.ALT.ordinal();
	private static final int SUPER = 1 << KeyModifier.SUPER.ordinal();

	@Test
	void normalizesModifiers() {
		InputConstants.Key key = keyboard(InputConstants.KEY_B);
		int allRawModifiers = InputConstants.MOD_SHIFT | InputConstants.MOD_CONTROL | InputConstants.MOD_ALT
				| InputConstants.MOD_SUPER | InputConstants.MOD_CAPS_LOCK | InputConstants.MOD_NUM_LOCK;

		assertEquals(SHIFT | CONTROL | ALT | SUPER, KeyMappingModifierImpl.modifierMask(allRawModifiers, key));
		assertEquals(CONTROL, KeyMappingModifierImpl.modifierMask(
				InputConstants.MOD_SHIFT | InputConstants.MOD_CONTROL,
				keyboard(InputConstants.KEY_LSHIFT)
		));
		assertEquals(CONTROL, KeyMappingModifierImpl.modifierMask(
				InputConstants.MOD_SHIFT | InputConstants.MOD_CONTROL,
				keyboard(InputConstants.KEY_RSHIFT)
		));
		assertEquals(SHIFT, KeyMappingModifierImpl.modifierMask(
				InputConstants.MOD_SHIFT | InputConstants.MOD_CONTROL,
				keyboard(InputConstants.KEY_LCONTROL)
		));
	}

	@Test
	void configuredChordsMatchExactlyWhilePlainMappingsRemainVanilla() {
		TestMapping plain = mapping("plain", InputConstants.KEY_B);
		TestMapping shift = mapping("shift", InputConstants.KEY_B);
		TestMapping control = mapping("control", InputConstants.KEY_B);
		KeyMappingModifierImpl.registerDefaultModifiers(shift.mapping(), KeyModifier.SHIFT);
		KeyMappingModifierImpl.registerDefaultModifiers(control.mapping(), KeyModifier.CONTROL);

		assertTrue(KeyMappingModifierImpl.matches(plain.mapping(), InputConstants.MOD_SHIFT | InputConstants.MOD_CONTROL));
		assertTrue(KeyMappingModifierImpl.matches(shift.mapping(), InputConstants.MOD_SHIFT));
		assertFalse(KeyMappingModifierImpl.matches(shift.mapping(), InputConstants.MOD_SHIFT | InputConstants.MOD_CONTROL));
		assertTrue(KeyMappingModifierImpl.same(plain.mapping(), shift.mapping(), true));
		assertFalse(KeyMappingModifierImpl.same(shift.mapping(), control.mapping(), true));
		assertFalse(KeyMappingModifierImpl.same(plain.mapping(), shift.mapping(), false));
		assertFalse(KeyMappingModifierImpl.haveSameModifiers(plain.mapping(), shift.mapping(), true));
		assertTrue(KeyMappingModifierImpl.haveSameModifiers(shift.mapping(), shift.mapping(), true));
	}

	@Test
	void repeatsRequireTheChordToMatchOnThePrimaryPress() {
		InputConstants.Key key = keyboard(InputConstants.KEY_B);
		TestMapping mapping = mapping("key.test.armed_repeat", InputConstants.KEY_B);
		KeyMappingModifierImpl.registerDefaultModifiers(mapping.mapping(), KeyModifier.SHIFT);

		assertFalse(matchInputEvent(mapping.mapping(), key, 0, InputConstants.PRESS));
		assertFalse(matchInputEvent(
				mapping.mapping(),
				keyboard(InputConstants.KEY_LSHIFT),
				InputConstants.MOD_SHIFT,
				InputConstants.PRESS
		));
		assertFalse(matchInputEvent(mapping.mapping(), key, InputConstants.MOD_SHIFT, InputConstants.REPEAT));
		assertFalse(matchInputEvent(mapping.mapping(), key, InputConstants.MOD_SHIFT, InputConstants.RELEASE));

		assertTrue(matchInputEvent(mapping.mapping(), key, InputConstants.MOD_SHIFT, InputConstants.PRESS));
		assertFalse(process(mapping.mapping(), key, InputConstants.MOD_SHIFT, InputConstants.REPEAT, false));
		assertTrue(matchInputEvent(mapping.mapping(), key, InputConstants.MOD_SHIFT, InputConstants.RELEASE));

		assertTrue(process(mapping.mapping(), key, InputConstants.MOD_SHIFT, InputConstants.PRESS, false));
		assertTrue(process(mapping.mapping(), key, InputConstants.MOD_SHIFT, InputConstants.REPEAT, false));

		KeyMappingModifierImpl.releaseMismatchedMappings(0);
		assertFalse(process(mapping.mapping(), key, InputConstants.MOD_SHIFT, InputConstants.REPEAT, false));

		assertTrue(process(mapping.mapping(), key, InputConstants.MOD_SHIFT, InputConstants.PRESS, false));
		assertTrue(process(mapping.mapping(), key, InputConstants.MOD_SHIFT, InputConstants.RELEASE, true));
		assertFalse(process(mapping.mapping(), key, InputConstants.MOD_SHIFT, InputConstants.REPEAT, false));
	}

	@Test
	void setKeyDerivesModifiersAndSerializationStoresOnlyOverrides() {
		TestMapping mapping = mapping("key.test.default_chord", InputConstants.KEY_B);
		KeyMappingModifierImpl.registerDefaultModifiers(mapping.mapping(), KeyModifier.SHIFT);

		assertEquals("", KeyMappingModifierImpl.serializeOverrides(new KeyMapping[]{mapping.mapping()}));

		mapping.setBoundKey(InputConstants.KEY_C);
		KeyMappingModifierImpl.onSetKey(mapping.mapping(), mapping.boundKey());
		assertEquals(Set.of(), KeyMappingHelper.getBoundModifiersOf(mapping.mapping()));
		assertEquals("", KeyMappingModifierImpl.serializeOverrides(new KeyMapping[]{mapping.mapping()}));

		KeyMappingModifierImpl.setModifierMask(mapping.mapping(), SHIFT);
		String changedBaseOverride = KeyMappingModifierImpl.serializeOverrides(new KeyMapping[]{mapping.mapping()});
		assertEquals("{\"key.test.default_chord\":[\"shift\"]}", changedBaseOverride);

		KeyMappingModifierImpl.onSetKey(mapping.mapping(), mapping.boundKey());
		KeyMappingModifierImpl.applyOverrides(changedBaseOverride, new KeyMapping[]{mapping.mapping()});
		assertEquals(Set.of(KeyModifier.SHIFT), KeyMappingHelper.getBoundModifiersOf(mapping.mapping()));

		mapping.setBoundKey(InputConstants.KEY_B);
		KeyMappingModifierImpl.onSetKey(mapping.mapping(), mapping.boundKey());
		KeyMappingModifierImpl.setModifierMask(mapping.mapping(), 0);
		String clearedDefaultOverride = KeyMappingModifierImpl.serializeOverrides(new KeyMapping[]{mapping.mapping()});
		assertEquals("{\"key.test.default_chord\":[]}", clearedDefaultOverride);

		KeyMappingModifierImpl.onSetKey(mapping.mapping(), mapping.boundKey());
		KeyMappingModifierImpl.applyOverrides(clearedDefaultOverride, new KeyMapping[]{mapping.mapping()});
		assertEquals(Set.of(), KeyMappingHelper.getBoundModifiersOf(mapping.mapping()));
		assertFalse(KeyMappingModifierImpl.isDefault(mapping.mapping(), true));
	}

	@Test
	void serializesModifiersInStableEnumOrder() {
		TestMapping mapping = mapping("key.test.stable_order", InputConstants.KEY_B);
		KeyMappingModifierImpl.setModifierMask(mapping.mapping(), SHIFT | CONTROL | ALT | SUPER);
		assertEquals(
				"{\"key.test.stable_order\":[\"shift\",\"control\",\"alt\",\"super\"]}",
				KeyMappingModifierImpl.serializeOverrides(new KeyMapping[]{mapping.mapping()})
		);
	}

	@Test
	void malformedOverridesFallBackWithoutAffectingPlainBehavior() {
		TestMapping mapping = mapping("key.test.malformed", InputConstants.KEY_B);
		TestMapping validSibling = mapping("key.test.valid_sibling", InputConstants.KEY_C);
		KeyMappingModifierImpl.registerDefaultModifiers(mapping.mapping(), KeyModifier.SHIFT);
		KeyMappingModifierImpl.setModifierMask(mapping.mapping(), CONTROL);

		KeyMappingModifierImpl.applyOverrides("{", new KeyMapping[]{mapping.mapping()});
		assertEquals(Set.of(KeyModifier.SHIFT), KeyMappingHelper.getBoundModifiersOf(mapping.mapping()));

		KeyMappingModifierImpl.applyOverrides(
				"{\"key.test.malformed\":[\"unknown\"],\"key.test.valid_sibling\":[\"control\"],\"missing\":[\"shift\"]}",
				new KeyMapping[]{mapping.mapping(), validSibling.mapping()}
		);
		assertEquals(Set.of(KeyModifier.SHIFT), KeyMappingHelper.getBoundModifiersOf(mapping.mapping()));
		assertEquals(Set.of(KeyModifier.CONTROL), KeyMappingHelper.getBoundModifiersOf(validSibling.mapping()));
	}

	@Test
	void publicModifierSetIsImmutableAndPlainLabelsAreUnchanged() {
		TestMapping mapping = mapping("key.test.api", InputConstants.KEY_B);
		KeyMappingModifierImpl.registerDefaultModifiers(mapping.mapping(), KeyModifier.CONTROL, KeyModifier.SHIFT);

		Set<KeyModifier> modifiers = KeyMappingHelper.getBoundModifiersOf(mapping.mapping());
		assertEquals(Set.of(KeyModifier.SHIFT, KeyModifier.CONTROL), modifiers);
		assertThrows(UnsupportedOperationException.class, () -> modifiers.add(KeyModifier.ALT));

		TestMapping plain = mapping("key.test.plain_label", InputConstants.KEY_C);
		Component vanilla = Component.literal("C");
		assertSame(vanilla, KeyMappingModifierImpl.getTranslatedKeyMessage(plain.mapping(), vanilla));
	}

	private static TestMapping mapping(String name, int defaultKeyCode) {
		InputConstants.Key defaultKey = keyboard(defaultKeyCode);
		AtomicReference<InputConstants.Key> boundKey = new AtomicReference<>(defaultKey);
		KeyMapping mapping = mock(KeyMapping.class, withSettings().extraInterfaces(KeyMappingAccessor.class));
		when(mapping.getName()).thenReturn(name);
		when(mapping.getDefaultKey()).thenReturn(defaultKey);
		when(((KeyMappingAccessor) mapping).fabric_getBoundKey()).thenAnswer(_ -> boundKey.get());
		return new TestMapping(mapping, boundKey);
	}

	private static InputConstants.Key keyboard(int keyCode) {
		return InputConstants.Type.KEYBOARD.getOrCreate(keyCode);
	}

	private static boolean process(KeyMapping mapping, InputConstants.Key key, int modifiers, int action, boolean release) {
		AtomicReference<Boolean> result = new AtomicReference<>();
		KeyMappingModifierImpl.withInputEvent(
				modifiers,
				action,
				() -> result.set(KeyMappingModifierImpl.shouldProcess(mapping, key, release))
		);
		return result.get();
	}

	private static boolean matchInputEvent(KeyMapping mapping, InputConstants.Key key, int modifiers, int action) {
		AtomicReference<Boolean> result = new AtomicReference<>();
		KeyMappingModifierImpl.withInputEvent(modifiers, action, () -> {
			KeyMappingModifierImpl.prepareInputEvent(key, modifiers, action);
			result.set(KeyMappingModifierImpl.matchesInputEvent(mapping, modifiers));
		});
		return result.get();
	}

	private record TestMapping(KeyMapping mapping, AtomicReference<InputConstants.Key> boundKeyRef) {
		InputConstants.Key boundKey() {
			return boundKeyRef.get();
		}

		void setBoundKey(int keyCode) {
			boundKeyRef.set(keyboard(keyCode));
		}
	}
}
