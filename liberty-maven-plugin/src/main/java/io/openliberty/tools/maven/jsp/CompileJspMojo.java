/**
 * (C) Copyright IBM Corporation 2017, 2025.
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
package io.openliberty.tools.maven.jsp;

import java.io.File;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import io.openliberty.tools.ant.jsp.CompileJSPs;
import io.openliberty.tools.common.plugins.util.PluginExecutionException;
import io.openliberty.tools.common.plugins.util.ServerFeatureUtil.FeaturesPlatforms;
import io.openliberty.tools.maven.InstallFeatureSupport;

/**
 * Compile the JSPs in the src/main/webapp folder.
 */
@Mojo(name = "compile-jsp", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class CompileJspMojo extends InstallFeatureSupport {

    /**
     * The version of JSP that should be compiled against. Defaults to 2.3. Can be
     * 2.2 or 2.3
     */
    @Parameter
    protected String jspVersion;

    /**
     * Timeout for JSP compile. Stop the server if the jsp compile isn't finish
     * within the given timeout (given in seconds).
     */
    @Parameter(defaultValue = "40")
    protected int timeout;
    
    @Override
    public void execute() throws MojoExecutionException {
        init();

        if (skip) {
            getLog().info("\nSkipping compile-jsp goal.\n");
            return;
        }

        doCompileJsps();
    }

    private void doCompileJsps() throws MojoExecutionException {
        CompileJSPs compile = (CompileJSPs) ant.createTask("antlib:io/openliberty/tools/ant:compileJSPs");
        if (compile == null) {
            throw new IllegalStateException(
                    MessageFormat.format(messages.getString("error.dependencies.not.found"), "compileJSPs"));
        }

        compile.setInstallDir(installDirectory);

        compile.setSrcdir(new File("src/main/webapp"));
        compile.setDestdir(new File(getProject().getBuild().getOutputDirectory()));
        compile.setTempdir(new File(getProject().getBuild().getDirectory()));
        compile.setTimeout(timeout);

        // don't delete temporary server dir
        compile.setCleanup(false);

        boolean sourceSet = false;

        List<Plugin> plugins = getProject().getBuildPlugins();
        for (Plugin plugin : plugins) {
            if ("org.apache.maven.plugins:maven-compiler-plugin".equals(plugin.getKey())) {
                Object config = plugin.getConfiguration();
                if (config instanceof Xpp3Dom) {
                    Xpp3Dom dom = (Xpp3Dom) config;
                    Xpp3Dom child = dom.getChild("release");
                    if (child != null && child.getValue() != null) {
                        String value = child.getValue();
                        getLog().debug("compile-jsp using maven.compiler.release value: "+value+" for javaSourceLevel.");
                        compile.setSource(value);
                        sourceSet = true;    
                    } else {
                        child = dom.getChild("source");
                        if (child != null && child.getValue() != null) {
                            String value = child.getValue();
                            getLog().debug("compile-jsp using maven.compiler.source value: "+value+" for javaSourceLevel.");
                            compile.setSource(value);
                            sourceSet = true;    
                        }
                    }
                }
                break;
            } else if ("org.apache.maven.plugins:maven-war-plugin".equals(plugin.getKey())) {
                Object config = plugin.getConfiguration();
                if (config instanceof Xpp3Dom) {
                    Xpp3Dom dom = (Xpp3Dom) config;
                    Xpp3Dom val = dom.getChild("warSourceDirectory");
                    if (val != null) {
                        compile.setSrcdir(new File(val.getValue()));
                    }
                }
            }
        }

        if (!sourceSet) {
            // look for Maven properties
            Properties props = getProject().getProperties();
            if (props.containsKey("maven.compiler.release")) {
                String value = props.getProperty("maven.compiler.release");
                if (value != null) {
                    getLog().debug("compile-jsp using maven.compiler.release value: "+value+" for javaSourceLevel.");
                    compile.setSource(value);
                    sourceSet = true;
                }  
            } else if (props.containsKey("maven.compiler.source")) {
                String value = props.getProperty("maven.compiler.source");
                if (value != null) {
                    getLog().debug("compile-jsp using maven.compiler.source value: "+value+" for javaSourceLevel.");
                    compile.setSource(value);
                    sourceSet = true;
                }
            }
        }

        Set<String> classpath = new TreeSet<String>();

        // first add target/classes (or whatever is configured)
        classpath.add(getProject().getBuild().getOutputDirectory());

        Set<Artifact> dependencies = getProject().getArtifacts();
        for (Artifact dep : dependencies) {
            if (!dep.isResolved()) {
                // TODO: Is transitive=true correct here?
                dep = resolveArtifact(dep);
            }
            if (dep.getFile() != null) {
                if (!classpath.add(dep.getFile().getAbsolutePath())) {
                    getLog().warn("Duplicate dependency: " + dep.getId());
                }
            } else {
                getLog().warn("Could not find: " + dep.getId());
            }
        }

        String classpathStr = join(classpath, File.pathSeparator);
        getLog().debug("Classpath: " + classpathStr);
        compile.setClasspath(classpathStr);

        if(initialize()) {
            Set<String> installedFeatures = new HashSet<String>();
            try {
            	FeaturesPlatforms fp = getSpecifiedFeatures(null);
            	if (fp!=null)
            		installedFeatures = fp.getFeatures();
            } catch (PluginExecutionException e) {
                throw new MojoExecutionException("Error getting the list of specified features.", e);
            }

            //Set JSP Feature Version
            setJspVersion(compile, installedFeatures);

            //Removing jsp and pages features as the jspVersion is already set at this point 
            Iterator<String> it = installedFeatures.iterator();
            while (it.hasNext()) {
                String nextItem = it.next();
                if (nextItem.startsWith("jsp-") || nextItem.startsWith("pages-")) {
                    it.remove();
                }
            }
            
            if(installedFeatures != null && !installedFeatures.isEmpty()) {
                compile.setFeatures(installedFeatures.toString().replace("[", "").replace("]", ""));
            }
        }

        compile.execute();
    }

    private void setJspVersion(CompileJSPs compile, Set<String> installedFeatures) {
        //If no conditions are met, defaults to 2.3 from the ant task
        if (jspVersion != null) {
            compile.setJspVersion(jspVersion);
        }
        else {
            for (String currentFeature : installedFeatures) {
                if(currentFeature.startsWith("jsp-") || currentFeature.startsWith("pages-")) {
                    String version = currentFeature.substring(currentFeature.indexOf("-")+1);
                    compile.setJspVersion(version);
                    break;
                }
            }
        }
    }

    private String join(Set<String> depPathes, String sep) {
        StringBuilder sb = new StringBuilder();
        for (String str : depPathes) {
            if (sb.length() != 0) {
                sb.append(sep);
            }
            sb.append(str);
        }
        return sb.toString();
    }

    protected void init() throws MojoExecutionException {
        boolean doInstall = (installDirectory == null);

        super.init();

        if (doInstall) {
            try {
                installServerAssembly();
            } catch (Exception e) {
                throw new MojoExecutionException("Failure installing server", e);
            }
        }
    }
}
