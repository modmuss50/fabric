package net.fabricmc.fabric.impl.gametest.junit;

import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;

import net.minecraft.server.Main;

import net.fabricmc.fabric.impl.gametest.GameTestSystemProperties;

import java.time.Duration;

public class JUnitGameTestRunner {
	public static void execute(ExecutionRequest request) {
		System.setProperty(GameTestSystemProperties.ENABLED, "true");

		Main.main(new String[]{});

		try {
			Thread.sleep(Duration.ofDays(5));
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		for (TestDescriptor testDescriptor : request.getRootTestDescriptor().getChildren()) {
			if (testDescriptor instanceof GameTestClassDescriptor classDescriptor) {
				for (TestDescriptor testMethod : classDescriptor.getChildren()) {
					GameTestMethodDescriptor gameTestMethod = (GameTestMethodDescriptor) testMethod;
					System.out.println();
				}
			}
		}
	}
}
