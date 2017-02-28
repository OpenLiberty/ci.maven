package net.wasdev.wlp.maven.plugins.jsp;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import net.wasdev.wlp.ant.jsp.CompileJSPs;
import net.wasdev.wlp.maven.plugins.BasicSupport;

/**
 * Compile the JSPs in the src/main/webapp folder.
 *
 * @goal compile-jsp
 *
 * @phase compile
 *
 * @requiresDependencyResolution compile
 *
 */
public class CompileJspMojo extends BasicSupport {

    /**
     * The version of JSP that should be compiled against. Defaults to 2.3. Can be 2.2 or 2.3
     *
     * @parameter
     */
    protected String jspVersion;

    /**
     * Timeout for JSP compile. Stop the server if the jsp compile isn't finish within the given
     * timeout (given in seconds).
     *
     * @parameter default-value="30"
     */
    protected int timeout;

    @Override
    protected void doExecute() throws Exception {
        CompileJSPs compile = (CompileJSPs) ant.createTask("antlib:net/wasdev/wlp/ant:compileJSPs");
        if (compile == null) {
            throw new NullPointerException("compileJSPs task not found");
        }

        compile.setInstallDir(installDirectory);

        compile.setSrcdir(new File("src/main/webapp"));
        compile.setDestdir(new File(getProject().getBuild().getOutputDirectory()));
        compile.setTempdir(new File(getProject().getBuild().getDirectory()));
        compile.setTimeout(timeout);

        // don't delete temporary server dir
        compile.setCleanup(false);

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

        Set<String> classpath = new TreeSet<String>();

        // first add target/classes (or whatever is configured)
        classpath.add(getProject().getBuild().getOutputDirectory());

        @SuppressWarnings("unchecked")
        Set<Artifact> dependencies = getProject().getArtifacts();
        for (Artifact dep : dependencies) {
            if (!dep.isResolved()) {
                // TODO: Is transitive=true correct here?
                dep = resolveArtifact(dep, true);
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
        log.debug("Classpath: " + classpathStr);
        compile.setClasspath(classpathStr);

        // TODO should we try to calculate this from a pom dependency?
        if (jspVersion != null) {
            compile.setJspVersion(jspVersion);
        }

        // TODO do we need to add features?
        compile.execute();
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
