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

package net.fabricmc.fabric.impl.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;

import net.fabricmc.fabric.api.config.v1.ConfigValue;
import net.fabricmc.fabric.api.config.v1.constraint.Constraint;
import net.fabricmc.fabric.api.config.v1.ui.UIControl;

public class ConfigValueImpl<T> extends AbstractConfigEntry implements ConfigValue<T> {
	private final String key;
	private final Codec<T> codec;
	private final T defaultValue;

	private boolean requiresRestart = false;
	private List<Constraint<T>> constraints = new ArrayList<>();
	private UIControl<?> uiControl = null;

	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private T value = null;

	public ConfigValueImpl(String key, Codec<T> codec, T defaultValue) {
		this.key = key;
		this.codec = codec;
		this.defaultValue = defaultValue;
	}

	@Override
	public T get() {
		lock.readLock().lock();

		try {
			if (value == null) {
				return defaultValue;
			}

			return value;
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void accept(T value) {
		lock.writeLock().lock();

		try {
			this.value = value;
		} finally {
			lock.writeLock().unlock();
		}
	}

	public NbtElement toNbt() {
		DataResult<NbtElement> result = codec.encodeStart(NbtOps.INSTANCE, get());
		return result.getOrThrow();
	}

	@Override
	public Codec<ConfigValueImpl<T>> codec() {
		return codec.xmap(newVal -> {
			accept(newVal);
			return this;
		}, ConfigValueImpl::get);
	}

	@Override
	public ConfigValueImpl<T> comment(String comment) {
		super.comment(comment);
		return this;
	}

	@Override
	public ConfigValue<T> requiresRestart() {
		validateSpecChange();
		this.requiresRestart = true;
		return this;
	}

	@Override
	public ConfigValue<T> constraint(Constraint<T> constraintConsumer) {
		validateSpecChange();
		this.constraints.add(constraintConsumer);
		return this;
	}

	@Override
	public <J> ConfigValue<T> uiControl(UIControl.Factory<J> uiControlFactory, Function<T, J> toUIValue, Function<J, T> fromUIValue) {
		validateSpecChange();
		this.uiControl = uiControlFactory.create(() -> toUIValue.apply(this.get()), j -> this.accept(fromUIValue.apply(j)));
		return this;
	}

	@Override
	public UIControl<?> getUIControl() {
		Objects.requireNonNull(this.uiControl, "UIControl not set");
		return this.uiControl;
	}
}
