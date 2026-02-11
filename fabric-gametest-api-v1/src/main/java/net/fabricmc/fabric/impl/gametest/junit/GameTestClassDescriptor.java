package net.fabricmc.fabric.impl.gametest.junit;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;

public class GameTestClassDescriptor extends AbstractTestDescriptor {
	private final Class<?> testClass;

	public GameTestClassDescriptor(UniqueId uniqueId, Class<?> testClass) {
		super(uniqueId, testClass.getSimpleName(), ClassSource.from(testClass));
		this.testClass = testClass;
	}

	@Override
	public Type getType() {
		return Type.CONTAINER;
	}

	public Class<?> getTestClass() {
		return testClass;
	}
}
