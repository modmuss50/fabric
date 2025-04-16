package net.fabricmc.fabric.api.config.v1.ui;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public sealed abstract class UIControl<T> implements Supplier<T>, Consumer<T> permits UIControl.BooleanControl,
		UIControl.StringControl,
		UIControl.NumberControl,
		UIControl.ColorControl,
		UIControl.StringListControl,
		UIControl.FallbackControl,
		UIControl.CustomControl {
	private final Supplier<T> getter;
	private final Consumer<T> setter;

	protected UIControl(Supplier<T> getter, Consumer<T> setter) {
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	public T get() {
		return getter.get();
	}

	@Override
	public void accept(T t) {
		setter.accept(t);
	}

	public static final class BooleanControl extends UIControl<Boolean> {
		public static final Factory<Boolean> FACTORY = BooleanControl::new;

		private BooleanControl(Supplier<Boolean> getter, Consumer<Boolean> setter) {
			super(getter, setter);
		}
	}

	public static final class StringControl extends UIControl<String> {
		public static final Factory<String> FACTORY = StringControl::new;

		private StringControl(Supplier<String> getter, Consumer<String> setter) {
			super(getter, setter);
		}
	}

	public static final class NumberControl extends UIControl<Number> {
		public static final Factory<Byte> BYTE_FACTORY = null;
		public static final Factory<Short> SHORT_FACTORY = null;
		public static final Factory<Integer> INTEGER_FACTORY = null;
		public static final Factory<Long> LONG_FACTORY = null;
		public static final Factory<Float> FLOAT_FACTORY = null;
		public static final Factory<Double> DOUBLE_FACTORY = null;

		private NumberControl(Supplier<Number> getter, Consumer<Number> setter) {
			super(getter, setter);
		}
	}

	public static final class ColorControl extends UIControl<Integer> {
		public static final Factory<Integer> FACTORY = ColorControl::new;

		private ColorControl(Supplier<Integer> getter, Consumer<Integer> setter) {
			super(getter, setter);
		}
	}

	public static final class StringListControl extends UIControl<List<String>> {
		public static final Factory<List<String>> FACTORY = StringListControl::new;

		private StringListControl(Supplier<List<String>> getter, Consumer<List<String>> setter) {
			super(getter, setter);
		}
	}

	// The fallback type uses SNBT to serialize the value as a string, allowing the ui to edit unknown types
	public static final class FallbackControl extends UIControl<String> {
		public static final Factory<String> FACTORY = FallbackControl::new;

		private FallbackControl(Supplier<String> getter, Consumer<String> setter) {
			super(getter, setter);
		}
	}

	// If a mod wants to add its own control for its own UI.
	non-sealed abstract static class CustomControl<T> extends UIControl<T> {
		protected CustomControl(Supplier<T> getter, Consumer<T> setter) {
			super(getter, setter);
		}
	}

	public interface Factory<T> {
		@ApiStatus.Internal
		UIControl<T> create(Supplier<T> getter, Consumer<T> setter);
	}
}
