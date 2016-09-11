package com.artemis;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;

/**
 * The artemis plugin performs bytecode-weaving on annotated components
 * and related classes.
 */
@Mojo(name = "generate", defaultPhase = GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ArtemisFluidMaven extends AbstractMojo {

    /**
     * Root folder for class files.
     */
    @Parameter(property = "project.build.directory", readonly = true)
    private File outputDirectory;

    /**
     * Root source folder.
     */
    @Parameter(property = "project.build.sourceDirectory", readonly = true)
    private File sourceDirectory;

    private Log log = getLog();

    @Parameter(property = "project.compileClasspathElements", required = true, readonly = true)
    private List<String> classpathElements;

    @Parameter(required = true, property = "project")
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        prepareGeneratedSourcesFolder();
        includeGeneratedSourcesInCompilation();

        new FluidGenerator().generate(
                classpathAsUrls(),
                generatedSourcesDirectory(), createLogAdapter());
    }

    /**
     * bridge maven/internal logging.
     */
    private com.artemis.generator.util.Log createLogAdapter() {
        return new com.artemis.generator.util.Log() {
            @Override
            public void info(String msg) {
                log.info(msg);
            }
        };
    }

    /**
     * Setup generated sources folder if missing.
     */
    private void prepareGeneratedSourcesFolder() {
        if (!generatedSourcesDirectory().exists() && !generatedSourcesDirectory().mkdirs()) {
            log.error("Could not create " + generatedSourcesDirectory());
        }
    }

    /**
     * Must include manually, or maven buids will fail.
     */
    private void includeGeneratedSourcesInCompilation() {
        this.project.addCompileSourceRoot(generatedSourcesDirectory().getPath());
    }

    private Set<URL> classpathAsUrls() {
        try {
            Set<URL> urls = new HashSet<URL>();
            for (String element : classpathElements) {
                URL url;
                url = new File(element).toURI().toURL();
                urls.add(url);
                log.info("Including: " + url);
            }
            return urls;
        } catch (MalformedURLException e) {
            throw new RuntimeException("failed to complete classpathAsUrls.", e);
        }
    }

    private File generatedSourcesDirectory() {
        return new File(outputDirectory, "generated-sources/fluid");
    }
}
