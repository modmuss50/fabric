package net.fabricmc.fabric.impl.gametest.junit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.fabricmc.fabric.impl.gametest.GameTestSystemProperties;

import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

import net.fabricmc.fabric.impl.gametest.TestAnnotationLocator;
import net.fabricmc.loader.api.FabricLoader;

public class GameTestEngine implements TestEngine {
	static {
		System.setProperty("fabric-api.gametest", "true");
	}
	@Override
	public String getId() {
		return "fabric-gametest";
	}

	@Override
	public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
		// Weird classloader shenanigans
		ClassLoader knot = Thread.currentThread().getContextClassLoader();

		try {
			Class<?> testAnnotationLoactor = knot.loadClass("net.fabricmc.fabric.impl.gametest.junit.JunitTestLocator");
			Method discover = testAnnotationLoactor.getDeclaredMethod("discover", EngineDiscoveryRequest.class, UniqueId.class);
			Object invoke = discover.invoke(null, discoveryRequest, uniqueId);
			return (TestDescriptor) invoke;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void execute(ExecutionRequest request) {
		// Weird classloader shenanigans
		ClassLoader knot = Thread.currentThread().getContextClassLoader();

		try {
			Class gameTestRunner = knot.loadClass("net.fabricmc.fabric.impl.gametest.junit.JUnitGameTestRunner");
			Method execute = gameTestRunner.getDeclaredMethod("execute", ExecutionRequest.class);
			execute.invoke(null, request);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
