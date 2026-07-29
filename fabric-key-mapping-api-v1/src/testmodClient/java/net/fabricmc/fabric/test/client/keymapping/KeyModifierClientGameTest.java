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

package net.fabricmc.fabric.test.client.keymapping;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyModifier;

public final class KeyModifierClientGameTest implements FabricClientGameTest {
	private static final int SHIFT_CONTROL = InputConstants.MOD_SHIFT | InputConstants.MOD_CONTROL;
	private static final int ALL_LOCKS = InputConstants.MOD_CAPS_LOCK | InputConstants.MOD_NUM_LOCK;

	@Override
	public void runTest(ClientGameTestContext context) {
		Screen originalScreen = context.computeOnClient(client -> client.gui.screen());
		OptionsFileSnapshot optionsFile = context.computeOnClient(client -> OptionsFileSnapshot.capture(client.options.getFile().toPath()));

		try {
			resetTestMappings(context);
			testDirectMatching(context);

			try (TestSingleplayerContext ignored = context.worldBuilder().create()) {
				testKeyboardDispatch(context);
				testMouseDispatch(context);
				testFocusRestoration(context);
				testDebugDispatch(context);
			}

			Screen controlsParent = context.computeOnClient(client -> client.gui.screen());
			KeyBindsScreen controls = testControlsScreen(context, controlsParent);
			testPersistence(context, controls, optionsFile.path());
		} finally {
			context.runOnClient(client -> {
				resetTestMappings();
				client.gui.setScreen(originalScreen);
				optionsFile.restore();
				client.options.load();
				resetTestMappings();
			});
		}
	}

	private static void testDirectMatching(ClientGameTestContext context) {
		context.runOnClient(client -> {
			assertTrue(Arrays.asList(client.options.keyMappings).contains(KeyMappingsTest.shiftB), "registered mapping was not added to Options");
			assertEquals(Set.of(), KeyMappingHelper.getBoundModifiersOf(KeyMappingsTest.plainB), "plain mapping modifiers");
			assertEquals(Set.of(KeyModifier.SHIFT), KeyMappingHelper.getBoundModifiersOf(KeyMappingsTest.shiftB), "Shift+B modifiers");
			assertEquals(Set.of(KeyModifier.CONTROL), KeyMappingHelper.getBoundModifiersOf(KeyMappingsTest.controlB), "Control+B modifiers");

			KeyEvent shiftB = keyEvent(InputConstants.KEY_B, InputConstants.MOD_SHIFT | ALL_LOCKS);
			assertTrue(KeyMappingsTest.shiftB.matches(shiftB), "Shift+B should ignore lock modifiers");
			assertFalse(KeyMappingsTest.shiftB.matches(keyEvent(InputConstants.KEY_B, 0)), "Shift+B matched without Shift");
			assertFalse(KeyMappingsTest.shiftB.matches(keyEvent(InputConstants.KEY_B, SHIFT_CONTROL)), "Shift+B matched with an extra modifier");
			assertTrue(KeyMappingsTest.plainB.matches(keyEvent(InputConstants.KEY_B, SHIFT_CONTROL)), "plain B lost vanilla wildcard matching");

			MouseButtonEvent mouse4 = new MouseButtonEvent(0, 0, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_4, SHIFT_CONTROL));
			MouseButtonEvent extraMouseModifier = new MouseButtonEvent(
					0,
					0,
					new MouseButtonInfo(InputConstants.MOUSE_BUTTON_4, SHIFT_CONTROL | InputConstants.MOD_ALT)
			);
			assertTrue(KeyMappingsTest.shiftControlMouse4.matchesMouse(mouse4), "Shift+Control+Mouse 4 did not match");
			assertFalse(KeyMappingsTest.shiftControlMouse4.matchesMouse(extraMouseModifier), "mouse chord matched with an extra modifier");

			assertTrue(KeyMappingsTest.plainB.same(KeyMappingsTest.shiftB), "plain B and Shift+B must be reported as conflicting");
			assertTrue(KeyMappingsTest.shiftB.same(KeyMappingsTest.plainB), "plain/chord collision must be symmetric");
			assertFalse(KeyMappingsTest.shiftB.same(KeyMappingsTest.controlB), "different exact chords must not conflict");

			String bName = KeyMappingHelper.getBoundKeyOf(KeyMappingsTest.plainB).getDisplayName().getString();
			assertEquals(bName, KeyMappingsTest.plainB.getTranslatedKeyMessage().getString(), "plain key label changed");
			assertEquals("Shift + " + bName, KeyMappingsTest.shiftB.getTranslatedKeyMessage().getString(), "chord label");
		});
	}

	private static void testKeyboardDispatch(ClientGameTestContext context) {
		context.runOnClient(client -> {
			releaseAndDrain();

			sendKey(client, InputConstants.KEY_B, InputConstants.PRESS, 0);
			assertTrue(KeyMappingsTest.plainB.isDown(), "plain B was not held");
			assertFalse(KeyMappingsTest.shiftB.isDown(), "Shift+B was held without Shift");
			assertFalse(KeyMappingsTest.controlB.isDown(), "Control+B was held without Control");
			assertClick(KeyMappingsTest.plainB, "plain B press");
			assertNoClick(KeyMappingsTest.shiftB, "Shift+B without Shift");
			sendKey(client, InputConstants.KEY_B, InputConstants.RELEASE, 0);
			assertFalse(KeyMappingsTest.plainB.isDown(), "plain B did not release");

			sendKey(client, InputConstants.KEY_B, InputConstants.PRESS, InputConstants.MOD_SHIFT | ALL_LOCKS);
			assertTrue(KeyMappingsTest.plainB.isDown(), "plain B did not retain vanilla behavior with Shift");
			assertTrue(KeyMappingsTest.shiftB.isDown(), "Shift+B was not held");
			assertFalse(KeyMappingsTest.controlB.isDown(), "Control+B accepted Shift");
			assertClick(KeyMappingsTest.plainB, "plain B with Shift");
			assertClick(KeyMappingsTest.shiftB, "Shift+B");
			sendKey(client, InputConstants.KEY_B, InputConstants.REPEAT, InputConstants.MOD_SHIFT);
			assertClick(KeyMappingsTest.plainB, "plain B repeat");
			assertClick(KeyMappingsTest.shiftB, "Shift+B repeat");
			sendKey(client, InputConstants.KEY_B, InputConstants.RELEASE, InputConstants.MOD_SHIFT);
			assertFalse(KeyMappingsTest.shiftB.isDown(), "Shift+B did not release with its base key");

			sendKey(client, InputConstants.KEY_B, InputConstants.PRESS, SHIFT_CONTROL);
			assertTrue(KeyMappingsTest.plainB.isDown(), "plain B rejected extra modifiers");
			assertFalse(KeyMappingsTest.shiftB.isDown(), "Shift+B accepted extra Control");
			assertFalse(KeyMappingsTest.controlB.isDown(), "Control+B accepted extra Shift");
			assertClick(KeyMappingsTest.plainB, "plain B with Shift+Control");
			assertNoClick(KeyMappingsTest.shiftB, "Shift+B with extra Control");
			assertNoClick(KeyMappingsTest.controlB, "Control+B with extra Shift");
			sendKey(client, InputConstants.KEY_B, InputConstants.RELEASE, 0);

			sendKey(client, InputConstants.KEY_B, InputConstants.PRESS, InputConstants.MOD_SHIFT);
			assertClick(KeyMappingsTest.plainB, "plain B before modifier release");
			assertClick(KeyMappingsTest.shiftB, "Shift+B before modifier release");
			sendKey(client, InputConstants.KEY_LSHIFT, InputConstants.RELEASE, 0);
			assertFalse(KeyMappingsTest.shiftB.isDown(), "releasing Shift did not deactivate Shift+B");
			assertTrue(KeyMappingsTest.plainB.isDown(), "releasing Shift deactivated plain B");
			assertNoClick(KeyMappingsTest.shiftB, "modifier release synthesized a click");
			sendKey(client, InputConstants.KEY_LSHIFT, InputConstants.PRESS, InputConstants.MOD_SHIFT);
			assertFalse(KeyMappingsTest.shiftB.isDown(), "pressing Shift activated a previously held B");
			assertNoClick(KeyMappingsTest.shiftB, "pressing Shift synthesized a B click");
			sendKey(client, InputConstants.KEY_B, InputConstants.REPEAT, InputConstants.MOD_SHIFT);
			assertFalse(KeyMappingsTest.shiftB.isDown(), "a repeat activated B after Shift was pressed");
			assertNoClick(KeyMappingsTest.shiftB, "a repeat clicked B after Shift was pressed");
			assertClick(KeyMappingsTest.plainB, "plain B repeat after Shift was pressed");
			sendKey(client, InputConstants.KEY_B, InputConstants.RELEASE, InputConstants.MOD_SHIFT);
			sendKey(client, InputConstants.KEY_LSHIFT, InputConstants.RELEASE, 0);

			sendKey(client, InputConstants.KEY_F13, InputConstants.PRESS, InputConstants.MOD_SHIFT);
			assertTrue(KeyMappingsTest.shiftToggle.isDown(), "Shift+F13 did not toggle on");
			assertClick(KeyMappingsTest.shiftToggle, "Shift+F13");
			sendKey(client, InputConstants.KEY_F13, InputConstants.RELEASE, InputConstants.MOD_SHIFT);
			sendKey(client, InputConstants.KEY_LSHIFT, InputConstants.RELEASE, 0);
			assertTrue(KeyMappingsTest.shiftToggle.isDown(), "modifier release reset a toggle mapping");

			sendKey(client, InputConstants.KEY_F13, InputConstants.PRESS, SHIFT_CONTROL);
			assertTrue(KeyMappingsTest.shiftToggle.isDown(), "an invalid chord changed the toggle state");
			assertNoClick(KeyMappingsTest.shiftToggle, "an invalid chord clicked the toggle mapping");
			sendKey(client, InputConstants.KEY_F13, InputConstants.RELEASE, SHIFT_CONTROL);
			sendKey(client, InputConstants.KEY_F13, InputConstants.PRESS, InputConstants.MOD_SHIFT);
			assertFalse(KeyMappingsTest.shiftToggle.isDown(), "second valid Shift+F13 did not toggle off");
			sendKey(client, InputConstants.KEY_F13, InputConstants.RELEASE, InputConstants.MOD_SHIFT);

			KeyMappingsTest.conditionalToggleMode = false;
			sendKey(client, InputConstants.KEY_F14, InputConstants.PRESS, InputConstants.MOD_SHIFT);
			assertTrue(KeyMappingsTest.shiftConditionalToggle.isDown(), "hold-mode Shift+F14 was not held");
			assertClick(KeyMappingsTest.shiftConditionalToggle, "hold-mode Shift+F14");
			sendKey(client, InputConstants.KEY_LSHIFT, InputConstants.RELEASE, 0);
			assertFalse(KeyMappingsTest.shiftConditionalToggle.isDown(), "modifier release did not clear a hold-mode ToggleKeyMapping");
			sendKey(client, InputConstants.KEY_F14, InputConstants.RELEASE, 0);
		});
	}

	private static void testMouseDispatch(ClientGameTestContext context) {
		context.runOnClient(client -> {
			releaseAndDrain();
			sendMouse(client, InputConstants.MOUSE_BUTTON_4, InputConstants.PRESS, SHIFT_CONTROL);
			assertTrue(KeyMappingsTest.shiftControlMouse4.isDown(), "mouse chord was not held");
			assertClick(KeyMappingsTest.shiftControlMouse4, "mouse chord");
			sendMouse(client, InputConstants.MOUSE_BUTTON_4, InputConstants.RELEASE, 0);
			assertFalse(KeyMappingsTest.shiftControlMouse4.isDown(), "mouse chord did not release without its modifiers");

			sendMouse(client, InputConstants.MOUSE_BUTTON_4, InputConstants.PRESS, SHIFT_CONTROL | InputConstants.MOD_ALT);
			assertFalse(KeyMappingsTest.shiftControlMouse4.isDown(), "mouse chord accepted an extra modifier");
			assertNoClick(KeyMappingsTest.shiftControlMouse4, "mouse chord with extra modifier");
			sendMouse(client, InputConstants.MOUSE_BUTTON_4, InputConstants.RELEASE, 0);
		});
	}

	private static void testFocusRestoration(ClientGameTestContext context) {
		context.getInput().holdKey(InputConstants.KEY_LSHIFT);
		context.getInput().holdKey(InputConstants.KEY_B);

		try {
			context.runOnClient(client -> {
				KeyMappingsTest.shiftB.setDown(false);
				KeyMapping.setAll();
				assertTrue(KeyMappingsTest.shiftB.isDown(), "setAll did not restore a physically held Shift+B");
				assertTrue(
						KeyMappingsTest.shiftB.matches(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_B)),
						"direct key matching did not poll held modifiers"
				);
				assertFalse(
						KeyMappingsTest.controlB.matches(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_B)),
						"direct key matching accepted the wrong held modifier"
				);
			});

			context.getInput().releaseKey(InputConstants.KEY_LSHIFT);
			context.runOnClient(client -> {
				assertFalse(KeyMappingsTest.shiftB.isDown(), "modifier release after focus restoration did not deactivate the chord");
				KeyMapping.setAll();
				assertFalse(KeyMappingsTest.shiftB.isDown(), "setAll restored B without its required modifier");
			});
		} finally {
			context.getInput().releaseKey(InputConstants.KEY_B);
			context.getInput().releaseKey(InputConstants.KEY_LSHIFT);
		}
	}

	private static void testDebugDispatch(ClientGameTestContext context) {
		try {
			KeyBindsScreen controls = context.computeOnClient(client -> new KeyBindsScreen(client.gui.screen(), client.options));
			context.setScreen(() -> controls);
			context.runOnClient(client -> {
				bindChord(client, controls, client.options.keyDebugModifier, InputConstants.KEY_F3, InputConstants.MOD_SHIFT);
				bindChord(client, controls, client.options.keyDebugOverlay, InputConstants.KEY_F3, InputConstants.MOD_SHIFT);
			});
			context.runOnClient(client -> {
				client.gui.setScreen(null);
				setDebugOverlay(client, false);

				sendKey(client, InputConstants.KEY_F3, InputConstants.PRESS, 0);
				sendKey(client, InputConstants.KEY_LSHIFT, InputConstants.PRESS, InputConstants.MOD_SHIFT);
				sendKey(client, InputConstants.KEY_F3, InputConstants.RELEASE, InputConstants.MOD_SHIFT);
				assertFalse(client.getDebugOverlay().showDebugScreen(), "an unarmed Shift+F3 release toggled the debug overlay");
				sendKey(client, InputConstants.KEY_LSHIFT, InputConstants.RELEASE, 0);

				sendKey(client, InputConstants.KEY_LSHIFT, InputConstants.PRESS, InputConstants.MOD_SHIFT);
				sendKey(client, InputConstants.KEY_F3, InputConstants.PRESS, InputConstants.MOD_SHIFT);
				sendKey(client, InputConstants.KEY_F3, InputConstants.RELEASE, InputConstants.MOD_SHIFT);
				assertTrue(client.getDebugOverlay().showDebugScreen(), "a fresh Shift+F3 chord did not toggle the debug overlay");
				sendKey(client, InputConstants.KEY_LSHIFT, InputConstants.RELEASE, 0);
			});
		} finally {
			context.runOnClient(client -> {
				client.options.keyDebugModifier.setKey(client.options.keyDebugModifier.getDefaultKey());
				client.options.keyDebugOverlay.setKey(client.options.keyDebugOverlay.getDefaultKey());
				KeyMapping.resetMapping();
				KeyMapping.releaseAll();
				setDebugOverlay(client, false);
			});
		}
	}

	private static void bindChord(Minecraft client, KeyBindsScreen controls, KeyMapping mapping, int key, int modifiers) {
		controls.selectedKey = mapping;
		sendKey(client, InputConstants.KEY_LSHIFT, InputConstants.PRESS, InputConstants.MOD_SHIFT);
		sendKey(client, key, InputConstants.PRESS, modifiers);
		sendKey(client, key, InputConstants.RELEASE, modifiers);
		sendKey(client, InputConstants.KEY_LSHIFT, InputConstants.RELEASE, 0);
		assertNull(controls.selectedKey, "Controls did not finish binding " + mapping.getName());
		assertEquals(InputConstants.Type.KEYBOARD.getOrCreate(key), KeyMappingHelper.getBoundKeyOf(mapping), "debug chord base key");
		assertEquals(Set.of(KeyModifier.SHIFT), KeyMappingHelper.getBoundModifiersOf(mapping), "debug chord modifiers");
	}

	private static void setDebugOverlay(Minecraft client, boolean value) {
		if (client.getDebugOverlay().showDebugScreen() != value) {
			client.debugEntries.toggleDebugOverlay();
		}
	}

	private static KeyBindsScreen testControlsScreen(ClientGameTestContext context, Screen parent) {
		KeyBindsScreen controls = context.computeOnClient(client -> new KeyBindsScreen(parent, client.options));
		context.setScreen(() -> controls);

		context.runOnClient(client -> {
			assertDefaultCollisionIndicator(controls);

			controls.selectedKey = KeyMappingsTest.editable;
			sendKey(client, InputConstants.KEY_LSHIFT, InputConstants.PRESS, InputConstants.MOD_SHIFT);
			assertSame(KeyMappingsTest.editable, controls.selectedKey, "modifier press completed selection too early");
			assertEquals(InputConstants.KEY_G, KeyMappingHelper.getBoundKeyOf(KeyMappingsTest.editable).getValue(), "modifier press changed the base key");
			sendKey(client, InputConstants.KEY_B, InputConstants.PRESS, InputConstants.MOD_SHIFT);
			assertNull(controls.selectedKey, "Shift+B did not complete selection");
			assertEquals(InputConstants.KEY_B, KeyMappingHelper.getBoundKeyOf(KeyMappingsTest.editable).getValue(), "Controls base key");
			assertEquals(Set.of(KeyModifier.SHIFT), KeyMappingHelper.getBoundModifiersOf(KeyMappingsTest.editable), "Controls chord modifiers");
			assertEquals("Shift + B", KeyMappingsTest.editable.getTranslatedKeyMessage().getString(), "Controls chord label");
			sendKey(client, InputConstants.KEY_B, InputConstants.RELEASE, InputConstants.MOD_SHIFT);
			sendKey(client, InputConstants.KEY_LSHIFT, InputConstants.RELEASE, 0);

			controls.selectedKey = KeyMappingsTest.editable;
			sendKey(client, InputConstants.KEY_LSHIFT, InputConstants.PRESS, InputConstants.MOD_SHIFT);
			sendKey(client, InputConstants.KEY_LCONTROL, InputConstants.PRESS, SHIFT_CONTROL);
			assertSame(KeyMappingsTest.editable, controls.selectedKey, "second modifier press completed selection");
			sendKey(client, InputConstants.KEY_B, InputConstants.PRESS, SHIFT_CONTROL);
			assertNull(controls.selectedKey, "Shift+Control+B did not complete selection");
			assertEquals(InputConstants.KEY_B, KeyMappingHelper.getBoundKeyOf(KeyMappingsTest.editable).getValue(), "multi-modifier base key");
			assertEquals(
					Set.of(KeyModifier.SHIFT, KeyModifier.CONTROL),
					KeyMappingHelper.getBoundModifiersOf(KeyMappingsTest.editable),
					"multi-modifier Controls chord"
			);
			assertEquals("Shift + Control + B", KeyMappingsTest.editable.getTranslatedKeyMessage().getString(), "multi-modifier Controls label");
			sendKey(client, InputConstants.KEY_B, InputConstants.RELEASE, SHIFT_CONTROL);
			sendKey(client, InputConstants.KEY_LCONTROL, InputConstants.RELEASE, InputConstants.MOD_SHIFT);
			sendKey(client, InputConstants.KEY_LSHIFT, InputConstants.RELEASE, 0);

			controls.selectedKey = KeyMappingsTest.editable;
			sendKey(client, InputConstants.KEY_LSHIFT, InputConstants.PRESS, InputConstants.MOD_SHIFT);
			sendKey(client, InputConstants.KEY_LCONTROL, InputConstants.PRESS, SHIFT_CONTROL);
			sendMouse(client, InputConstants.MOUSE_BUTTON_4, InputConstants.PRESS, SHIFT_CONTROL);
			assertNull(controls.selectedKey, "Shift+Control+Mouse 4 did not complete selection");
			assertEquals(
					InputConstants.Type.MOUSE.getOrCreate(InputConstants.MOUSE_BUTTON_4),
					KeyMappingHelper.getBoundKeyOf(KeyMappingsTest.editable),
					"mouse chord base key"
			);
			assertEquals(
					Set.of(KeyModifier.SHIFT, KeyModifier.CONTROL),
					KeyMappingHelper.getBoundModifiersOf(KeyMappingsTest.editable),
					"mouse Controls chord"
			);
			sendMouse(client, InputConstants.MOUSE_BUTTON_4, InputConstants.RELEASE, SHIFT_CONTROL);
			sendKey(client, InputConstants.KEY_LCONTROL, InputConstants.RELEASE, InputConstants.MOD_SHIFT);
			sendKey(client, InputConstants.KEY_LSHIFT, InputConstants.RELEASE, 0);

			controls.selectedKey = KeyMappingsTest.editable;
			sendKey(client, InputConstants.KEY_LSHIFT, InputConstants.PRESS, InputConstants.MOD_SHIFT);
			sendKey(client, InputConstants.KEY_LSHIFT, InputConstants.RELEASE, 0);
			assertNull(controls.selectedKey, "standalone Shift did not complete on release");
			assertEquals(InputConstants.KEY_LSHIFT, KeyMappingHelper.getBoundKeyOf(KeyMappingsTest.editable).getValue(), "standalone modifier base key");
			assertEquals(Set.of(), KeyMappingHelper.getBoundModifiersOf(KeyMappingsTest.editable), "standalone modifier retained itself");

			controls.selectedKey = KeyMappingsTest.editable;
			sendKey(client, InputConstants.KEY_LCONTROL, InputConstants.PRESS, InputConstants.MOD_CONTROL);
			sendKey(client, InputConstants.KEY_LSHIFT, InputConstants.PRESS, SHIFT_CONTROL);
			sendKey(client, InputConstants.KEY_LCONTROL, InputConstants.RELEASE, InputConstants.MOD_SHIFT);
			assertSame(KeyMappingsTest.editable, controls.selectedKey, "releasing a secondary modifier completed selection");
			sendKey(client, InputConstants.KEY_LSHIFT, InputConstants.RELEASE, 0);
			assertNull(controls.selectedKey, "modifier-primary chord did not complete on primary release");
			assertEquals(InputConstants.KEY_LSHIFT, KeyMappingHelper.getBoundKeyOf(KeyMappingsTest.editable).getValue(), "modifier-primary base key");
			assertEquals(Set.of(KeyModifier.CONTROL), KeyMappingHelper.getBoundModifiersOf(KeyMappingsTest.editable), "modifier-primary chord");

			controls.selectedKey = KeyMappingsTest.editable;
			sendKey(client, InputConstants.KEY_ESCAPE, InputConstants.PRESS, 0);
			assertNull(controls.selectedKey, "Escape did not complete selection");
			assertTrue(KeyMappingsTest.editable.isUnbound(), "Escape did not unbind the mapping");
			assertEquals(Set.of(), KeyMappingHelper.getBoundModifiersOf(KeyMappingsTest.editable), "unbound mapping retained modifiers");
			sendKey(client, InputConstants.KEY_ESCAPE, InputConstants.RELEASE, 0);

			controls.selectedKey = KeyMappingsTest.shiftB;
			sendKey(client, InputConstants.KEY_B, InputConstants.PRESS, 0);
			assertEquals(Set.of(), KeyMappingHelper.getBoundModifiersOf(KeyMappingsTest.shiftB), "Controls did not clear default modifiers");
			assertFalse(KeyMappingsTest.shiftB.isDefault(), "cleared default chord was reported as default");
			sendKey(client, InputConstants.KEY_B, InputConstants.RELEASE, 0);

			// This is the same operation used by each keybind row's Reset button.
			KeyMappingsTest.shiftB.setKey(KeyMappingsTest.shiftB.getDefaultKey());
			KeyMapping.resetMapping();
			assertEquals(Set.of(KeyModifier.SHIFT), KeyMappingHelper.getBoundModifiersOf(KeyMappingsTest.shiftB), "row reset did not restore modifiers");
			assertTrue(KeyMappingsTest.shiftB.isDefault(), "row reset did not restore the default chord");

			controls.selectedKey = KeyMappingsTest.editable;
			sendKey(client, InputConstants.KEY_B, InputConstants.PRESS, InputConstants.MOD_SHIFT);
			sendKey(client, InputConstants.KEY_B, InputConstants.RELEASE, InputConstants.MOD_SHIFT);
			controls.selectedKey = KeyMappingsTest.shiftB;
			sendKey(client, InputConstants.KEY_B, InputConstants.PRESS, 0);
			sendKey(client, InputConstants.KEY_B, InputConstants.RELEASE, 0);
		});

		context.clickScreenButton("controls.resetAll");
		context.runOnClient(client -> {
			assertEquals(KeyMappingsTest.editable.getDefaultKey(), KeyMappingHelper.getBoundKeyOf(KeyMappingsTest.editable), "Reset All base key");
			assertEquals(Set.of(), KeyMappingHelper.getBoundModifiersOf(KeyMappingsTest.editable), "Reset All plain modifiers");
			assertEquals(KeyMappingsTest.shiftB.getDefaultKey(), KeyMappingHelper.getBoundKeyOf(KeyMappingsTest.shiftB), "Reset All chord base key");
			assertEquals(Set.of(KeyModifier.SHIFT), KeyMappingHelper.getBoundModifiersOf(KeyMappingsTest.shiftB), "Reset All chord modifiers");
		});
		return controls;
	}

	private static void assertDefaultCollisionIndicator(KeyBindsScreen controls) {
		KeyBindsList list = controls.children().stream()
				.filter(KeyBindsList.class::isInstance)
				.map(KeyBindsList.class::cast)
				.findFirst()
				.orElseThrow(() -> new AssertionError("Controls screen has no keybind list"));
		boolean found = list.children().stream()
				.flatMap(entry -> entry.narratables().stream())
				.filter(Button.class::isInstance)
				.map(Button.class::cast)
				.map(button -> button.getMessage().getString())
				.anyMatch("[ Shift + B ]"::equals);
		assertTrue(found, "plain B and default Shift+B did not show a collision indicator");
	}

	private static void testPersistence(ClientGameTestContext context, KeyBindsScreen controls, Path optionsFile) {
		context.runOnClient(client -> client.options.save());
		assertNoOption(readOptions(optionsFile), "fabricKeyModifiers", "default mappings wrote modifier overrides");

		context.runOnClient(client -> {
			controls.selectedKey = KeyMappingsTest.shiftB;
			sendKey(client, InputConstants.KEY_C, InputConstants.PRESS, InputConstants.MOD_SHIFT);
			sendKey(client, InputConstants.KEY_C, InputConstants.RELEASE, InputConstants.MOD_SHIFT);
			client.options.save();
		});

		String changedBaseOptions = readOptions(optionsFile);
		assertEquals(
				KeyMappingHelper.getBoundKeyOf(KeyMappingsTest.shiftB).getName(),
				optionValue(changedBaseOptions, "key_" + KeyMappingsTest.shiftB.getName()),
				"vanilla key option contained more than the base key"
		);
		assertEquals(
				"{\"" + KeyMappingsTest.shiftB.getName() + "\":[\"shift\"]}",
				optionValue(changedBaseOptions, "fabricKeyModifiers"),
				"changed-base modifier override"
		);

		context.runOnClient(client -> {
			KeyMappingsTest.shiftB.setKey(KeyMappingsTest.shiftB.getDefaultKey());
			KeyMapping.resetMapping();
			client.options.load();
			assertEquals(InputConstants.KEY_C, KeyMappingHelper.getBoundKeyOf(KeyMappingsTest.shiftB).getValue(), "load did not restore changed base key");
			assertEquals(Set.of(KeyModifier.SHIFT), KeyMappingHelper.getBoundModifiersOf(KeyMappingsTest.shiftB), "load did not restore changed-base modifiers");

			controls.selectedKey = KeyMappingsTest.shiftB;
			sendKey(client, InputConstants.KEY_B, InputConstants.PRESS, 0);
			sendKey(client, InputConstants.KEY_B, InputConstants.RELEASE, 0);
			client.options.save();
		});

		String clearedDefaultOptions = readOptions(optionsFile);
		assertEquals(
				KeyMappingsTest.shiftB.getDefaultKey().getName(),
				optionValue(clearedDefaultOptions, "key_" + KeyMappingsTest.shiftB.getName()),
				"cleared default chord changed its vanilla key option"
		);
		assertEquals(
				"{\"" + KeyMappingsTest.shiftB.getName() + "\":[]}",
				optionValue(clearedDefaultOptions, "fabricKeyModifiers"),
				"cleared default chord was not persisted explicitly"
		);

		context.runOnClient(client -> {
			KeyMappingsTest.shiftB.setKey(KeyMappingsTest.shiftB.getDefaultKey());
			KeyMapping.resetMapping();
			client.options.load();
			assertEquals(Set.of(), KeyMappingHelper.getBoundModifiersOf(KeyMappingsTest.shiftB), "load did not preserve cleared default modifiers");
			assertFalse(KeyMappingsTest.shiftB.isDefault(), "cleared default modifiers were reported as default");

			KeyMappingsTest.shiftB.setKey(KeyMappingsTest.shiftB.getDefaultKey());
			KeyMapping.resetMapping();
			client.options.save();
		});
		assertNoOption(readOptions(optionsFile), "fabricKeyModifiers", "reset mappings retained modifier overrides");
	}

	private static void resetTestMappings(ClientGameTestContext context) {
		context.runOnClient(client -> resetTestMappings());
	}

	private static void resetTestMappings() {
		KeyMappingsTest.conditionalToggleMode = false;

		for (KeyMapping mapping : testMappings()) {
			mapping.setKey(mapping.getDefaultKey());
		}

		KeyMapping.resetMapping();
		releaseAndDrain();
	}

	private static KeyMapping[] testMappings() {
		assertNotNull(KeyMappingsTest.plainB, "client initializer did not register test mappings");
		return new KeyMapping[]{
				KeyMappingsTest.plainB,
				KeyMappingsTest.shiftB,
				KeyMappingsTest.controlB,
				KeyMappingsTest.shiftControlMouse4,
				KeyMappingsTest.shiftToggle,
				KeyMappingsTest.shiftConditionalToggle,
				KeyMappingsTest.editable
		};
	}

	private static void releaseAndDrain() {
		KeyMapping.releaseAll();

		for (KeyMapping mapping : testMappings()) {
			while (mapping.consumeClick()) {
			}
		}
	}

	private static void sendKey(Minecraft client, int key, int action, int modifiers) {
		client.keyboardHandler.keyPress(client.getWindow().handle(), action, keyEvent(key, modifiers));
	}

	private static KeyEvent keyEvent(int key, int modifiers) {
		return new KeyEvent(key, 0, modifiers);
	}

	private static void sendMouse(Minecraft client, int button, int action, int modifiers) {
		client.mouseHandler.onButton(client.getWindow().handle(), new MouseButtonInfo(button, modifiers), action);
	}

	private static void assertClick(KeyMapping mapping, String action) {
		assertTrue(mapping.consumeClick(), action + " did not create a click");
		assertFalse(mapping.consumeClick(), action + " created more than one click");
	}

	private static void assertNoClick(KeyMapping mapping, String action) {
		assertFalse(mapping.consumeClick(), action + " unexpectedly created a click");
	}

	private static String readOptions(Path path) {
		try {
			return Files.readString(path, StandardCharsets.UTF_8);
		} catch (IOException exception) {
			throw new UncheckedIOException("Failed to read " + path, exception);
		}
	}

	private static String optionValue(String options, String name) {
		String prefix = name + ":";

		for (String line : options.lines().toList()) {
			if (line.startsWith(prefix)) {
				return line.substring(prefix.length());
			}
		}

		throw new AssertionError("Missing option " + name);
	}

	private static void assertNoOption(String options, String name, String message) {
		String prefix = name + ":";

		if (options.lines().anyMatch(line -> line.startsWith(prefix))) {
			throw new AssertionError(message);
		}
	}

	private static void assertTrue(boolean value, String message) {
		if (!value) {
			throw new AssertionError(message);
		}
	}

	private static void assertFalse(boolean value, String message) {
		assertTrue(!value, message);
	}

	private static void assertNull(Object value, String message) {
		if (value != null) {
			throw new AssertionError(message + ": " + value);
		}
	}

	private static void assertNotNull(Object value, String message) {
		if (value == null) {
			throw new AssertionError(message);
		}
	}

	private static void assertSame(Object expected, Object actual, String message) {
		if (expected != actual) {
			throw new AssertionError(message + ": expected same instance");
		}
	}

	private static void assertEquals(Object expected, Object actual, String message) {
		if (!expected.equals(actual)) {
			throw new AssertionError(message + ": expected " + expected + ", got " + actual);
		}
	}

	private record OptionsFileSnapshot(Path path, boolean existed, byte[] contents) {
		static OptionsFileSnapshot capture(Path path) {
			try {
				boolean existed = Files.exists(path);
				return new OptionsFileSnapshot(path, existed, existed ? Files.readAllBytes(path) : new byte[0]);
			} catch (IOException exception) {
				throw new UncheckedIOException("Failed to snapshot " + path, exception);
			}
		}

		void restore() {
			try {
				if (existed) {
					Files.write(path, contents);
				} else {
					Files.deleteIfExists(path);
				}
			} catch (IOException exception) {
				throw new UncheckedIOException("Failed to restore " + path, exception);
			}
		}
	}
}
