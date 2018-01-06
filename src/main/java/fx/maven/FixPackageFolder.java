package fx.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Mojo(name = "fix-package-folder-names")
public class FixPackageFolder extends AbstractMojo {

    @Parameter( defaultValue = "${project.artifactId}", readonly = true )
    private String artifactId;

    @Parameter( defaultValue = "${project.basedir}", readonly = true )
    private File baseDir;

    public void execute() throws MojoExecutionException {
        getLog().info("Fixing code folder");
        Path rootFolder = Paths.get(baseDir.getAbsolutePath(),"code", artifactId);
        if( rootFolder.toFile().exists() ) {
            addAPlusToFolderName(rootFolder.toFile());
        }else{
            getLog().debug("Could not find " + rootFolder.toString() + "; skipping.");
        }
        getLog().info("Fixing test folder");
        Path testFolder = Paths.get(baseDir.getAbsolutePath(),"test");
        if( testFolder.toFile().exists() ) {
            addAPlusToFolderName(testFolder.toFile());
        }else{
            getLog().debug("Could not find " + testFolder.toString() + "; skipping.");
        }
    }

    private void addAPlusToFolderName(File folder){
        File[] subDirectories = folder.listFiles();
        if( subDirectories != null ) {
            for (File entry : subDirectories) {
                if (entry.isDirectory()) {
                    getLog().debug("Passing " + entry.getAbsolutePath());
                    String directoryName = entry.getName();
                    if (!directoryName.startsWith("+")) {
                        File newFile = Paths.get(entry.getParentFile().getAbsolutePath(), "+" + directoryName).toFile();
                        getLog().debug("    Renaming to " + newFile.getAbsolutePath());
                        entry.renameTo(newFile);
                        entry = newFile;
                    }
                    addAPlusToFolderName(entry);
                }
            }
        }
    }
}
