/**
 * (C) Copyright IBM Corporation 2018, 2025.
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
package io.openliberty.tools.maven.utils;

import java.io.File;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import io.openliberty.tools.common.plugins.util.PluginScenarioException;

public class SpringBootUtil {

    /**
     * Checks that the spring-boot-maven-plugin repackage goal execution exists.
     *
     */    
    public static boolean doesSpringBootRepackageGoalExecutionExist(MavenProject project) {
        return MavenProjectUtil.doesPluginGoalExecutionExist(project, "org.springframework.boot:spring-boot-maven-plugin", "repackage");
    }

    /**
     * Returns the major version number of the configured spring-boot-maven-plugin, or zero if not configured.
     *
     */    
    public static int getSpringBootMavenPluginVersion(MavenProject project) {
        return MavenProjectUtil.getMajorPluginVersion(project, "org.springframework.boot:spring-boot-maven-plugin");

    }

    /**
     * Read the value of the classifier configuration parameter from the
     * spring-boot-maven-plugin
     * 
     * @param project
     * @param log
     * @return the value if it was found, null otherwise
     */
    public static String getSpringBootMavenPluginClassifier(MavenProject project, Log log) {
        String classifier = null;
        try {
            classifier = MavenProjectUtil.getPluginGoalConfigurationString(project,
                    "org.springframework.boot:spring-boot-maven-plugin", "repackage", "classifier");
        } catch (PluginScenarioException e) {
            log.debug("No classifier found for spring-boot-maven-plugin");
        }
        return classifier;
    }

    /**
     * Get the Spring Boot Uber JAR in its expected location, validating the JAR
     * contents and handling spring-boot-maven-plugin classifier configuration as
     * well. If the JAR was not found in its expected location, then return null.
     * 
     * @param project
     * @param log
     * @return the JAR File if it was found, false otherwise
     */
    public static File getSpringBootUberJAR(MavenProject project, Log log) {

        File fatArchive = getSpringBootUberJARLocation(project, log);

        if (io.openliberty.tools.common.plugins.util.SpringBootUtil.isSpringBootUberJar(fatArchive)) {
            log.info("Found Spring Boot Uber JAR or WAR: " + fatArchive.getAbsolutePath());
            return fatArchive;
        }

        if (fatArchive.exists()) {
            log.warn("The file at the following location is not a Spring Boot Uber JAR or WAR: " + fatArchive.getAbsolutePath());
        } else {
            log.warn("Spring Boot Uber JAR or WAR was not found in expected location: " + fatArchive.getAbsolutePath());
        }
        return null;
    }

    /**
     * Get the Spring Boot Uber JAR in its expected location, taking into account the
     * spring-boot-maven-plugin classifier configuration as well.  No validation is done,
     * that is, there is no guarantee the JAR file actually exists at the returned location.
     *
     * @param project
     * @param log
     * @return the File representing the JAR location, whether a file exists there or not.
     */
    public static File getSpringBootUberJARLocation(MavenProject project, Log log) {
        String classifier = getSpringBootMavenPluginClassifier(project, log);

        if (classifier == null) {
            classifier = "";
        }
        if (!classifier.isEmpty() && !classifier.startsWith("-")) {
            classifier = "-" + classifier;
        }

        return new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + classifier
                + "." + project.getArtifact().getArtifactHandler().getExtension());

    }

    /*
     * Returns the required Open Liberty springBoot feature given the passed springBootMajorVersion.
     */
    public static String getLibertySpringBootFeature(int springBootMajorVersion) {
        if (isSpringBoot1(springBootMajorVersion)) {
            return "springBoot-1.5";
        } else if (isSpringBoot2plus(springBootMajorVersion)) {
            return "springBoot-"+springBootMajorVersion+".0";
        }
        return null;
    }

    public static boolean isSpringBoot1(int springBootMajorVersion) {
        return springBootMajorVersion == 1;
    }

    public static boolean isSpringBoot2plus(int springBootMajorVersion) {
        return springBootMajorVersion > 1;
    }
}
