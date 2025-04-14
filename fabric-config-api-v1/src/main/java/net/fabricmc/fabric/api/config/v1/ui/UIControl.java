package net.fabricmc.fabric.api.config.v1.ui;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.fabricmc.fabric.api.config.v1.constraint.Constraint;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public sealed interface UIControl<T> extends Supplier<T>, Consumer<T> permits UIControl.BooleanControl,
		UIControl.StringControl,
		UIControl.NumberControl,
		UIControl.ColorControl,
		UIControl.StringListControl,
		UIControl.FallbackControl,
		UIControl.CustomControl {
	Collection<Constraint<T>> getConstraints();

	@ApiStatus.NonExtendable
	non-sealed interface BooleanControl extends UIControl<Boolean> {
		Factory<Boolean> FACTORY = null;
	}

	@ApiStatus.NonExtendable
	non-sealed interface StringControl extends UIControl<String> {
		Factory<String> FACTORY = null;
	}

	@ApiStatus.NonExtendable
	non-sealed interface NumberControl extends UIControl<Number> {
		Factory<Byte> BYTE_FACTORY = null;
		Factory<Short> SHORT_FACTORY = null;
		Factory<Integer> INTEGER_FACTORY = null;
		Factory<Long> LONG_FACTORY = null;
		Factory<Float> FLOAT_FACTORY = null;
		Factory<Double> DOUBLE_FACTORY = null;
	}

	@ApiStatus.NonExtendable
	non-sealed interface ColorControl extends UIControl<Integer> {
		Factory<Integer> FACTORY = null;
	}

	@ApiStatus.NonExtendable
	non-sealed interface StringListControl extends UIControl<List<String>> {
		Factory<List<String>> FACTORY = null;
	}

	// The fallback type uses SNBT to serialize the value as a string, allowing the ui to edit unknown types
	@ApiStatus.NonExtendable
	non-sealed interface FallbackControl extends UIControl<String> {
		Factory<String> FACTORY = null;
	}

	// If a mod wants to add its own control for its own UI.
	non-sealed interface CustomControl<T> extends UIControl<T> {
	}

	interface Factory<T> {
		@ApiStatus.Internal
		UIControl<T> create(Supplier<T> getter, Consumer<T> setter);
	}
}
