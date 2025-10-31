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

package net.fabricmc.fabric.test.registry.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.fabricmc.fabric.impl.registry.sync.packet.DirectRegistryPacketHandler;

public class DirectRegistryPacketHandlerTest {
	@BeforeAll
	static void beforeAll() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void emptyRegistrySync() {
		DirectRegistryPacketHandler handler = new DirectRegistryPacketHandler();

		Map<ResourceLocation, Object2IntMap<ResourceLocation>> registry = new HashMap<>();

		var payloads = new ArrayList<DirectRegistryPacketHandler.Payload>();
		handler.sendPacket(payloads::add, registry);

		assertEquals(2, payloads.size());
		// Last payload is empty, indicating all the data has been sent.
		assertEquals(0, payloads.get(1).data().length);

		for (DirectRegistryPacketHandler.Payload payload : payloads) {
			handler.receivePayload(payload);
		}

		assertMatchesDeep(registry, handler.getSyncedPacketData().idMap());
	}

	@Test
	void singlePacketRegistrySync() {
		DirectRegistryPacketHandler handler = new DirectRegistryPacketHandler();

		Map<ResourceLocation, Object2IntMap<ResourceLocation>> registry = new HashMap<>();
		registry.put(ResourceLocation.parse("test"), createRegistry(150));

		var payloads = new ArrayList<DirectRegistryPacketHandler.Payload>();
		handler.sendPacket(payloads::add, registry);

		assertEquals(2, payloads.size());
		// Last payload is empty, indicating all the data has been sent.
		assertEquals(0, payloads.get(1).data().length);

		for (DirectRegistryPacketHandler.Payload payload : payloads) {
			handler.receivePayload(payload);
		}

		assertMatchesDeep(registry, handler.getSyncedPacketData().idMap());
	}

	@Test
	void splitPacketRegistrySync() {
		DirectRegistryPacketHandler handler = new DirectRegistryPacketHandler();
		Map<ResourceLocation, Object2IntMap<ResourceLocation>> registry = new HashMap<>();

		for (int i = 0; i < 50; i++) {
			registry.put(ResourceLocation.fromNamespaceAndPath("test", "namespace_" + i), createRegistry(15000));
		}

		var payloads = new ArrayList<DirectRegistryPacketHandler.Payload>();
		handler.sendPacket(payloads::add, registry);

		// Expect 10 packets to be sent
		assertEquals(10, payloads.size());
		// Last payload is empty, indicating all the data has been sent.
		assertEquals(0, payloads.get(9).data().length);

		for (DirectRegistryPacketHandler.Payload payload : payloads) {
			handler.receivePayload(payload);
		}

		assertMatchesDeep(registry, handler.getSyncedPacketData().idMap());
	}

	private static Object2IntMap<ResourceLocation> createRegistry(int size) {
		Object2IntMap<ResourceLocation> entries = new Object2IntOpenHashMap<>();

		for (int i = 0; i < size; i++) {
			entries.put(ResourceLocation.fromNamespaceAndPath("test", "entry_" + i), i);
		}

		return entries;
	}

	// Deep comparison of two maps of maps
	private static void assertMatchesDeep(Map<ResourceLocation, Object2IntMap<ResourceLocation>> expected, Map<ResourceLocation, Object2IntMap<ResourceLocation>> actual) {
		assertEquals(expected.size(), actual.size());

		for (Map.Entry<ResourceLocation, Object2IntMap<ResourceLocation>> entry : expected.entrySet()) {
			Object2IntMap<ResourceLocation> actualValue = actual.get(entry.getKey());
			assertEquals(entry.getValue(), actualValue);
		}
	}
}
