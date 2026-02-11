package net.fabricmc.fabric.impl.gametest.junit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

import net.fabricmc.fabric.impl.gametest.TestAnnotationLocator;
import net.fabricmc.loader.api.FabricLoader;

public class JunitTestLocator {
	public static TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
		EngineDescriptor engineDescriptor = new EngineDescriptor(uniqueId, "Fabric GameTest");

		TestAnnotationLocator locator = new TestAnnotationLocator(FabricLoader.getInstance());
		List<TestAnnotationLocator.TestMethod> testMethods = locator.getTestMethods();

		// Extract selectors from discovery request to filter tests
		List<ClassSelector> classSelectors = discoveryRequest.getSelectorsByType(ClassSelector.class);
		List<MethodSelector> methodSelectors = discoveryRequest.getSelectorsByType(MethodSelector.class);

		// Group test methods by class to create a proper hierarchy
		Map<Class<?>, GameTestClassDescriptor> classDescriptors = new HashMap<>();

		for (TestAnnotationLocator.TestMethod testMethod : testMethods) {
			Class<?> testClass = testMethod.method().getDeclaringClass();

			// Apply filtering based on discovery selectors
			if (!shouldIncludeTest(testMethod, classSelectors, methodSelectors)) {
				continue;
			}

			// Get or create class descriptor
			GameTestClassDescriptor classDescriptor = classDescriptors.computeIfAbsent(testClass, clazz -> {
				UniqueId classUniqueId = uniqueId.append("class", clazz.getName());
				GameTestClassDescriptor descriptor = new GameTestClassDescriptor(classUniqueId, clazz);
				engineDescriptor.addChild(descriptor);
				return descriptor;
			});

			// Create method descriptor
			UniqueId methodUniqueId = classDescriptor.getUniqueId().append("method", testMethod.method().getName());
			GameTestMethodDescriptor methodDescriptor = new GameTestMethodDescriptor(methodUniqueId, testMethod);
			classDescriptor.addChild(methodDescriptor);
		}

		return engineDescriptor;
	}

	private static boolean shouldIncludeTest(TestAnnotationLocator.TestMethod testMethod,
									  List<ClassSelector> classSelectors,
									  List<MethodSelector> methodSelectors) {
		// If no selectors are specified, include all tests
		if (classSelectors.isEmpty() && methodSelectors.isEmpty()) {
			return true;
		}

		Class<?> testClass = testMethod.method().getDeclaringClass();
		String methodName = testMethod.method().getName();

		// Check if this specific method is selected
		for (MethodSelector methodSelector : methodSelectors) {
			if (methodSelector.getJavaClass().equals(testClass)
					&& methodSelector.getMethodName().equals(methodName)) {
				return true;
			}
		}

		// Check if the entire class is selected
		for (ClassSelector classSelector : classSelectors) {
			if (classSelector.getJavaClass().equals(testClass)) {
				return true;
			}
		}

		return false;
	}
}
