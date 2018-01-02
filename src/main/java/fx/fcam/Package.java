package fx.fcam;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;

@Mojo(name = "package")
public class Package extends AbstractMojo {

    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;

    @Parameter(property = "project.build.directory", readonly = true)
    private String outputDirectory;

    @Parameter(property = "project.build.finalName", readonly = true)
    private String finalName;

    public void execute() throws MojoExecutionException {
        getLog().info("Packaging - TODO");
        File artifact = new File(this.outputDirectory +"/"+ this.finalName + ".mltbx");
        try {
            getLog().info("Trying to create "+artifact.toString());
            artifact.getParentFile().mkdirs();
            artifact.createNewFile();
        }catch( java.io.IOException exception ){
            getLog().info("Failed creating artifact: "+exception.getMessage());
        }
        this.project.getArtifact().setFile(artifact);
    }
}
