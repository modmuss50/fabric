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

package net.fabricmc.fabric.impl.build;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import groovy.util.Node;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.artifacts.ResolvableConfiguration;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;

import net.fabricmc.loom.api.LoomGradleExtensionAPI;

public class FabricApiModuleExtension {
	private final Project project;
	private final Set<String> registeredModProjects = new HashSet<>();

	public FabricApiModuleExtension(Project project) {
		this.project = project;
	}

	public void moduleDependencies(String... dependencyNames) {
		addDependencies("api", "clientImplementation", dependencyNames);
		List<Map<String, String>> dependencyNodes = new ArrayList<>();

		for (String dependencyName : dependencyNames) {
			String moduleName = FabricApiBuildUtils.projectPath(dependencyName);
			moduleName = moduleName.substring(moduleName.lastIndexOf(':') + 1);
			var dependencyNode = new LinkedHashMap<String, String>();
			dependencyNode.put("groupId", project.getGroup().toString());
			dependencyNode.put("artifactId", moduleName);
			dependencyNode.put("version", FabricApiBuildUtils.moduleVersion(project, moduleName));
			dependencyNode.put("scope", "compile");
			dependencyNodes.add(dependencyNode);
		}

		project.getPluginManager().withPlugin("maven-publish", plugin -> project.getExtensions().configure(PublishingExtension.class, publishing -> {
			publishing.getPublications().named("mavenJava", MavenPublication.class).configure(publication -> {
				publication.getPom().withXml(xml -> FabricApiBuildUtils.appendPomDependencies((Node) xml.asNode(), dependencyNodes));
			});
		}));
	}

	public void testDependencies(String... dependencyNames) {
		addDependencies("testmodImplementation", "testmodClientImplementation", dependencyNames);
	}

	private void addDependencies(String commonConfiguration, String clientConfiguration, String[] dependencyNames) {
		NamedDomainObjectProvider<DependencyScopeConfiguration> clientDependencies = project.getConfigurations().dependencyScope(commonConfiguration + "ModuleClientDependencies");
		NamedDomainObjectProvider<ResolvableConfiguration> clientOutputs = project.getConfigurations().resolvable(commonConfiguration + "ModuleClientOutputs", configuration -> {
			configuration.extendsFrom(clientDependencies.get());
			configuration.setTransitive(false);
			FabricApiBuildUtils.configureJavaRuntimeClassesVariant(configuration, "client");
		});
		project.getDependencies().add(clientConfiguration, project.files(clientOutputs));
		LoomGradleExtensionAPI loom = project.getExtensions().getByType(LoomGradleExtensionAPI.class);

		for (String dependencyName : dependencyNames) {
			String dependencyPath = FabricApiBuildUtils.projectPath(dependencyName);

			if (dependencyPath.equals(project.getPath())) {
				continue;
			}

			project.getDependencies().add(commonConfiguration, project.getDependencies().project(Map.of("path", dependencyPath)));
			project.getDependencies().add(clientDependencies.getName(), project.getDependencies().project(Map.of("path", dependencyPath, "configuration", "fabricApiClientOutputElements")));

			if (!registeredModProjects.add(dependencyPath)) {
				continue;
			}

			String moduleName = dependencyPath.substring(dependencyPath.lastIndexOf(':') + 1);
			loom.getMods().register(moduleName, mod -> {
				mod.sourceSet("main", dependencyPath);
				mod.sourceSet("client", dependencyPath);
			});
			loom.getMods().register(moduleName + "-testmod", mod -> {
				mod.sourceSet("testmod", dependencyPath);
				mod.sourceSet("testmodClient", dependencyPath);
			});
		}
	}
}
