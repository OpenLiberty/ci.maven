/**
 * (C) Copyright IBM Corporation 2014, 2017.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.wasdev.wlp.maven.plugins.applications;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.pluginsupport.util.ArtifactItem;

import net.wasdev.wlp.maven.plugins.ApplicationXmlDocument;

/**
 * Copy applications to the specified directory of the Liberty server.
 */
@Mojo(name = "install-apps", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class InstallAppsMojo extends InstallAppMojoSupport {

	protected void doExecute() throws Exception {
		if (skip) {
			return;
		}
		checkServerHomeExists();
		checkServerDirectoryExists();

		// update target server configuration
		copyConfigFiles();
		exportParametersToXml();

		boolean installDependencies = false;
		boolean installProject = false;
		boolean installThinProject = false;

		switch (getInstallAppPackages()) {
		case "all":
			installDependencies = true;
			installProject = true;
			installThinProject = true;
			break;
		case "dependencies":
			installDependencies = true;
			break;
		case "project":
			installProject = true;
			break;
		case "thin-project":
			installThinProject = true;
			break;
		default:
			return;
		}
		if (installDependencies) {
			installDependencies(false);
		}
		if (installProject) {
			installProject(false);
		}
		if (installThinProject) {
			installProject(true);
		}

		// create application configuration in configDropins if it is not configured
		if (applicationXml.hasChildElements()) {
			log.warn(messages.getString("warn.install.app.add.configuration"));
			applicationXml.writeApplicationXmlDocument(serverDirectory);
		} else {
			if (ApplicationXmlDocument.getApplicationXmlFile(serverDirectory).exists()) {
				ApplicationXmlDocument.getApplicationXmlFile(serverDirectory).delete();
			}
		}
	}

	private void installDependencies(boolean thin) throws Exception {
		Set<Artifact> artifacts = project.getArtifacts();
		log.debug("Number of compile dependencies for " + project.getArtifactId() + " : " + artifacts.size());

		for (Artifact artifact : artifacts) {
			// skip if not an application type supported by Liberty
			if (!isSupportedType(artifact.getType())) {
				continue;
			}
			// skip assemblyArtifact if specified as a dependency
			if (assemblyArtifact != null && matches(artifact, assemblyArtifact)) {
				continue;
			}
			if (artifact.getScope().equals("compile")) {
				if (isSupportedType(artifact.getType())) {
					if (looseApplication && isReactorMavenProject(artifact)) {
						MavenProject dependProj = getReactorMavenProject(artifact);
						installLooseApplication(dependProj, thin);
					} else {
						installApp(resolveArtifact(artifact), thin);
					}
				} else {
					log.warn(MessageFormat.format(messages.getString("error.application.not.supported"),
							project.getId()));
				}
			}
		}
	}

	protected void installProject(boolean thin) throws Exception {
		if (isSupportedType(project.getPackaging())) {
			if (looseApplication) {
				installLooseApplication(project, thin);
			} else {
				installApp(project.getArtifact(), thin);
			}
		} else {
			throw new MojoExecutionException(
					MessageFormat.format(messages.getString("error.application.not.supported"), project.getId()));
		}
	}

	private void installLooseApplication(MavenProject proj, boolean thin) throws Exception {
		String looseConfigFileName = getLooseConfigFileName(proj);
		String application = looseConfigFileName.substring(0, looseConfigFileName.length() - 4);
		File destDir = new File(serverDirectory, getAppsDirectory());
		File looseConfigFile = new File(destDir, looseConfigFileName);
		LooseConfigData config = new LooseConfigData();
		switch (proj.getPackaging()) {
		case "war":
			validateAppConfig(application, proj.getArtifactId());
			log.info(MessageFormat.format(messages.getString("info.install.app"), looseConfigFileName));
			installLooseConfigWar(proj, config);
			deleteApplication(new File(serverDirectory, "apps"), looseConfigFile);
			deleteApplication(new File(serverDirectory, "dropins"), looseConfigFile);
			config.toXmlFile(looseConfigFile);
			break;
		case "ear":
			validateAppConfig(application, proj.getArtifactId());
			log.info(MessageFormat.format(messages.getString("info.install.app"), looseConfigFileName));
			installLooseConfigEar(proj, config);
			deleteApplication(new File(serverDirectory, "apps"), looseConfigFile);
			deleteApplication(new File(serverDirectory, "dropins"), looseConfigFile);
			config.toXmlFile(looseConfigFile);
			break;
		case "liberty-assembly":
			if (mavenWarPluginExists(proj) || new File(proj.getBasedir(), "src/main/webapp").exists()) {
				validateAppConfig(application, proj.getArtifactId());
				log.info(MessageFormat.format(messages.getString("info.install.app"), looseConfigFileName));
				installLooseConfigWar(proj, config);
				deleteApplication(new File(serverDirectory, "apps"), looseConfigFile);
				deleteApplication(new File(serverDirectory, "dropins"), looseConfigFile);
				config.toXmlFile(looseConfigFile);
			} else {
				log.debug(
						"The liberty-assembly project does not contain the maven-war-plugin or src/main/webapp does not exist.");
			}
			break;
		default:
			log.info(MessageFormat.format(messages.getString("info.loose.application.not.supported"),
					proj.getPackaging()));
			installApp(proj.getArtifact(), thin);
			break;
		}
	}

	private boolean mavenWarPluginExists(MavenProject proj) {
		MavenProject currentProject = proj;
		while (currentProject != null) {
			List<Object> plugins = new ArrayList<Object>(currentProject.getBuildPlugins());
			plugins.addAll(currentProject.getPluginManagement().getPlugins());
			for (Object o : plugins) {
				if (o instanceof Plugin) {
					Plugin plugin = (Plugin) o;
					if (plugin.getGroupId().equals("org.apache.maven.plugins")
							&& plugin.getArtifactId().equals("maven-war-plugin")) {
						return true;
					}
				}
			}
			currentProject = currentProject.getParent();
		}
		return false;
	}

	private boolean matches(Artifact artifact, ArtifactItem assemblyArtifact) {
		return artifact.getGroupId().equals(assemblyArtifact.getGroupId())
				&& artifact.getArtifactId().equals(assemblyArtifact.getArtifactId())
				&& artifact.getType().equals(assemblyArtifact.getType());
	}

	private boolean isSupportedType(String type) {
		boolean supported = false;
		switch (type) {
		case "ear":
		case "war":
		case "rar":
		case "eba":
		case "esa":
		case "liberty-assembly":
			supported = true;
			break;
		default:
			break;
		}
		return supported;
	}

}
