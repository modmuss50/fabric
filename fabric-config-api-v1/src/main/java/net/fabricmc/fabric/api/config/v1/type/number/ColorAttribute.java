package net.fabricmc.fabric.api.config.v1.type.number;


public interface ColorAttribute extends NumberAttribute<Integer> {
	static ColorAttribute color(ColorAttribute.ColorFormat format) {
		return new ColorAttribute() {
			@Override
			public ColorFormat getColorFormat() {
				return format;
			}

			@Override
			public boolean test(Integer integer) {
				return true;
			}
		};
	}

	enum ColorFormat {
		RGB,
		RGBA,
		ARGB,
	}

	ColorFormat getColorFormat();
}
