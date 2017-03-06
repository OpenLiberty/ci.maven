package net.wasdev.wlp.maven.plugins.jsp;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import net.wasdev.wlp.ant.jsp.CompileJSPs;
import net.wasdev.wlp.maven.plugins.BasicSupport;

/**
 * Compile the JSPs in the src/main/webapp folder.
 */
@Mojo( name = "compile-jsp", defaultPhase = LifecyclePhase.COMPILE, 
       requiresDependencyResolution = ResolutionScope.COMPILE )  
public class CompileJspMojo extends BasicSupport {

    /**
     * The version of JSP that should be compiled against. Defaults to 2.3. Can be 2.2 or 2.3
     */
    @Parameter( property="compile-jsp.jspVersion" )
    protected String jspVersion;
    
    protected void doExecute() throws Exception {
        CompileJSPs compile = (CompileJSPs)ant.createTask("antlib:net/wasdev/wlp/ant:compileJSPs");
        if (compile == null) {
            throw new NullPointerException("server task not found");
        }

        compile.setInstallDir(installDirectory);
        compile.setSrcdir(new File("src/main/webapp"));
        compile.setDestdir(new File("target/classes"));

        @SuppressWarnings("unchecked")
        List<Plugin> plugins = getProject().getBuildPlugins();
        for (Plugin plugin : plugins) {
            if ("org.apache.maven.plugins:maven-compiler-plugin".equals(plugin.getKey())) {
                Object config = plugin.getConfiguration();
                if (config instanceof Xpp3Dom) {
                    Xpp3Dom dom = (Xpp3Dom) config;
                    Xpp3Dom val = dom.getChild("source");
                    if (val != null) {
                        compile.setSource(val.getValue());
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

        StringBuilder builder = new StringBuilder(new File("target/classes").getAbsolutePath());
        @SuppressWarnings("unchecked")
        Set<Artifact> dependencies = getProjectArtifacts(true);
        for (Artifact dep : dependencies) {
            File onDisk = dep.getFile();
            if (onDisk != null) {
                builder.append(File.pathSeparator);
                builder.append(dep.getFile().getAbsolutePath());
            } else {
                System.err.println("Could not find: " + dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion());
            }
        }
        
        // TODO should we try to calculate this from a pom dependency?
        if (jspVersion != null) {
            compile.setJspVersion(jspVersion);
        }

        compile.setClasspath(builder.toString());
        
        // TODO do we need to add features?

        compile.execute();
    }

    @Override
    protected void init() throws MojoExecutionException, MojoFailureException {
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
