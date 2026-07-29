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

package net.fabricmc.fabric.api.client.keymapping.v1;

/**
 * A logical modifier that can be required by a key mapping.
 *
 * <p>The left and right variants of a modifier key are treated as equivalent. Lock-key state is not a modifier.
 */
public enum KeyModifier {
	SHIFT,
	CONTROL,
	ALT,
	SUPER
}
