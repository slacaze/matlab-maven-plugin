package fx.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

@Mojo(name = "copy-dependencies", requiresDependencyResolution = ResolutionScope.RUNTIME)
public class CopyDependencies extends AbstractMojo {

    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject mavenProject;

    @Parameter( defaultValue = "${session}", readonly = true )
    private MavenSession mavenSession;

    @Component
    private BuildPluginManager pluginManager;

    @Parameter(property = "dependenciesDirectory", readonly = true)
    private String dependenciesDirectory;

    public void execute() throws MojoExecutionException {
        executeMojo(
            plugin(
                groupId("org.apache.maven.plugins"),
                artifactId("maven-dependency-plugin"),
                version("3.0.2")
                ),
            goal("copy-dependencies"),
            configuration(
                element(name("outputDirectory"), dependenciesDirectory)
                ),
            executionEnvironment(
                mavenProject,
                mavenSession,
                pluginManager
                )
            );
    }
}
