/**
 * (C) Copyright IBM Corporation 2022, 2023.
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
package io.openliberty.tools.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AbstractLibertySupportTest {

    private static final ArtifactHandler HANDLER = new DefaultArtifactHandler();

    @Test
    public void matching_artifact_matches_wildcard_startsWith() {
        // given
        Artifact projectArtifact = new DefaultArtifact("junit", "junit", "1.0.0", "compile", "jar", null, HANDLER);
        String groupId = "junit";
        boolean isWildcard = true;
        String compareArtifactId = "j";

        // when
        boolean isMatchingDep = AbstractLibertySupport.isMatchingProjectDependency(projectArtifact, groupId, isWildcard, compareArtifactId, false, null);

        // then
        assertTrue("expected match but did not", isMatchingDep);
    }

    @Test
    public void is_exactly_matching_dependency_without_classifier() {
        // given
        Artifact projectArtifact = new DefaultArtifact("junit", "junit", "1.0.0", "compile", "jar", null, HANDLER);
        String groupId = "junit";
        boolean isWildcard = false;
        String compareArtifactId = "junit";

        // when
        boolean isMatchingDep = AbstractLibertySupport.isMatchingProjectDependency(projectArtifact, groupId, isWildcard, compareArtifactId, false, null);

        // then
        assertTrue("expected match but did not", isMatchingDep);
    }

    @Test
    public void is_exactly_matching_dependency_with_empty_classifier() {
        // given
        Artifact projectArtifact = new DefaultArtifact("junit", "junit", "1.0.0", "compile", "jar", "", HANDLER);
        String groupId = "junit";
        boolean isWildcard = false;
        String compareArtifactId = "junit";

        // when
        boolean isMatchingDep = AbstractLibertySupport.isMatchingProjectDependency(projectArtifact, groupId, isWildcard, compareArtifactId, false, null);

        // then
        assertTrue("expected match but did not", isMatchingDep);
    }

    @Test
    public void is_wildcard_artifact() {
        // given
        Artifact projectArtifact = new DefaultArtifact("junit", "junit", "1.0.0", "compile", "jar", "", HANDLER);
        String groupId = "junit";
        boolean isWildcard = true;
        String compareArtifactId = null;

        // when
        boolean isMatchingDep = AbstractLibertySupport.isMatchingProjectDependency(projectArtifact, groupId, isWildcard, compareArtifactId, false, null);

        // then
        assertTrue("expected match but did not", isMatchingDep);
    }

    @Test
    public void is_not_match_different_groupId() {
        // given
        Artifact projectArtifact = new DefaultArtifact("org.apache.maven.plugins", "maven-compiler-plugin", "1.0.0", "compile", "jar", "", HANDLER);
        String groupId = "junit";
        boolean isWildcard = false;
        String compareArtifactId = "junit";

        // when
        boolean isMatchingDep = AbstractLibertySupport.isMatchingProjectDependency(projectArtifact, groupId, isWildcard, compareArtifactId, false, null);

        // then
        assertFalse("expected no match but did", isMatchingDep);
    }

    @Test
    public void classifier_matches_on_exact_artifactId() {
        // given
        Artifact projectArtifact = new DefaultArtifact("junit", "junit", "1.0.0", "compile", "jar", "classes", HANDLER);
        String groupId = "junit";
        boolean isWildcard = false;
        String compareArtifactId = "junit";

        // when
        boolean isMatchingDep = AbstractLibertySupport.isMatchingProjectDependency(projectArtifact, groupId, isWildcard, compareArtifactId, false, "classes");

        // then
        assertTrue("expected match but did not", isMatchingDep);
    }

    @Test
    public void classifier_matches_on_wildcard_artifactId() {
        // given
        Artifact projectArtifact = new DefaultArtifact("junit", "junit", "1.0.0", "compile", "jar", "classes", HANDLER);
        String groupId = "junit";
        boolean isWildcard = true;
        String compareArtifactId = "ju";

        // when
        boolean isMatchingDep = AbstractLibertySupport.isMatchingProjectDependency(projectArtifact, groupId, isWildcard, compareArtifactId, false, "classes");

        // then
        assertTrue("expected match but did not", isMatchingDep);
    }

    @Test
    public void classifier_does_not_match() {
        // given
        Artifact projectArtifact = new DefaultArtifact("junit", "junit", "1.0.0", "compile", "jar", "classes", HANDLER);
        String groupId = "junit";
        boolean isWildcard = false;
        String compareArtifactId = "junit";

        // when
        boolean isMatchingDep = AbstractLibertySupport.isMatchingProjectDependency(projectArtifact, groupId, isWildcard, compareArtifactId, false, null);

        // then
        assertFalse("expected no match but did", isMatchingDep);
    }

    @Test
    public void classifier_wildcard_any_matches() {
        // given
        Artifact projectArtifact = new DefaultArtifact("junit", "junit", "1.0.0", "compile", "jar", "classes", HANDLER);
        String groupId = "junit";
        boolean isWildcard = true;
        String compareArtifactId = "ju";
        boolean isClassifierWildcard = true;
        String compareClassifier = null;

        // when
        boolean isMatchingDep = AbstractLibertySupport.isMatchingProjectDependency(projectArtifact, groupId, isWildcard, compareArtifactId, isClassifierWildcard, compareClassifier);

        // then
        assertTrue("expected match but did not", isMatchingDep);
    }

    @Test
    public void classifier_wildcard_startsWith_matches() {
        // given
        Artifact projectArtifact = new DefaultArtifact("junit", "junit", "1.0.0", "compile", "jar", "classes", HANDLER);
        String groupId = "junit";
        boolean isWildcard = true;
        String compareArtifactId = "ju";
        boolean isClassifierWildcard = true;
        String compareClassifier = "cl";

        // when
        boolean isMatchingDep = AbstractLibertySupport.isMatchingProjectDependency(projectArtifact, groupId, isWildcard, compareArtifactId, isClassifierWildcard, compareClassifier);

        // then
        assertTrue("expected match but did not", isMatchingDep);
    }

}
