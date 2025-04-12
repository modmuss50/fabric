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

package net.fabricmc.fabric.impl.config.serialization;

import java.util.Iterator;
import java.util.List;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.visitor.NbtOrderedStringFormatter;

public class ConfigStringFormatter extends NbtOrderedStringFormatter {
	private final EncodeContext encodeContext;

	public ConfigStringFormatter(EncodeContext encodeContext) {
		this.encodeContext = encodeContext;
	}

	public ConfigStringFormatter(String prefix, int indentationLevel, List<String> pathParts, EncodeContext encodeContext) {
		super(prefix, indentationLevel, pathParts);
		this.encodeContext = encodeContext;
	}

	@Override
	public void visitCompound(NbtCompound compound) {
		if (compound.isEmpty()) {
			this.result = CURLY_OPEN_BRACKET + CURLY_CLOSE_BRACKET;
			return;
		}

		StringBuilder sb = new StringBuilder(CURLY_OPEN_BRACKET);
		this.pushPathPart(CURLY_OPEN_BRACKET + CURLY_CLOSE_BRACKET);

		String pathPrefix = IGNORED_PATHS.contains(this.joinPath()) ? "" : this.prefix;

		if (!pathPrefix.isEmpty()) {
			sb.append(NEW_LINE);
		}

		Iterator<String> it = compound.getKeys().iterator();

		while (it.hasNext()) {
			String key = it.next();

			if (encodeContext.comments().containsKey(key)) {
				sb.append(pathPrefix.repeat(this.indentationLevel + 1))
						.append("// ")
						.append(encodeContext.comments().get(key))
						.append(NEW_LINE);
			}

			EncodeContext subContext = encodeContext.subContext().getOrDefault(key, EncodeContext.EMPTY);

			this.pushPathPart(key);
			sb.append(pathPrefix.repeat(this.indentationLevel + 1))
					.append(escapeName(key))
					.append(KEY_VALUE_SEPARATOR)
					.append(SPACE)
					.append(new ConfigStringFormatter(pathPrefix, this.indentationLevel + 1, this.pathParts, subContext)
							.apply(compound.get(key)));
			this.popPathPart();

			if (it.hasNext()) {
				sb.append(ENTRY_SEPARATOR)
						.append(pathPrefix.isEmpty() ? SPACE : NEW_LINE + NEW_LINE);
			}
		}

		if (!pathPrefix.isEmpty()) {
			sb.append(NEW_LINE)
					.append(pathPrefix.repeat(this.indentationLevel));
		}

		sb.append("}");
		this.result = sb.toString();
		this.popPathPart();
	}

	@Override
	protected List<String> getSortedNames(NbtCompound compound) {
		throw new IllegalStateException();
	}
}
