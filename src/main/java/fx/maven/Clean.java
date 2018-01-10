package fx.maven;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.apache.commons.io.FileUtils;

@Mojo(name = "clean")
public class Clean extends AbstractMojo {

    @Parameter(property = "dependenciesDirectory", readonly = true, required = true, defaultValue="${project.basedir}/dependencies")
    private String dependenciesDirectory;

    public void execute() throws MojoExecutionException {
        try{
            File dependencyFolder = new File(dependenciesDirectory);
            if( dependencyFolder.exists() ) {
                getLog().info("Deleting: " + dependencyFolder.getAbsolutePath());
                FileUtils.deleteDirectory(dependencyFolder);
            }
        } catch( IOException exception ) {
            throw new MojoExecutionException("An IO error occured: " + exception.getMessage());
        }
    }

}
