package net.fabricmc.fabric.impl.gametest.junit;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;

import net.fabricmc.fabric.impl.gametest.TestAnnotationLocator;

public class GameTestMethodDescriptor extends AbstractTestDescriptor {
	private final TestAnnotationLocator.TestMethod testMethod;

	public GameTestMethodDescriptor(UniqueId uniqueId, TestAnnotationLocator.TestMethod testMethod) {
		super(
				uniqueId,
				testMethod.method().getName(),
				MethodSource.from(testMethod.method().getDeclaringClass().getName(), testMethod.method().getName())
		);
		this.testMethod = testMethod;
	}

	@Override
	public Type getType() {
		return Type.TEST;
	}

	public TestAnnotationLocator.TestMethod getTestMethod() {
		return testMethod;
	}
}
