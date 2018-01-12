package fx.maven;

import java.lang.InterruptedException;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import org.apache.commons.io.FileUtils;

@Mojo(name = "package")
public class Package extends AbstractMojo {

    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;

    @Parameter(property = "project.build.directory", readonly = true)
    private String outputDirectory;

    @Parameter(property = "project.build.finalName", readonly = true)
    private String finalName;

    @Parameter(property = "project.artifactId", readonly = true)
    private String artifactId;

    @Parameter( property = "matlabPath", required = true, readonly = true, defaultValue = "C:\\Program Files\\MATLAB\\R2017b\\bin\\matlab.exe" )
    private String matlabPath;

    @Parameter( property = "preserveLog", required = true, readonly = true, defaultValue = "false" )
    private boolean preserveLog;

    @Parameter(property = "dependenciesDirectory", readonly = true, required = true, defaultValue="${project.basedir}/dependencies")
    private String dependenciesDirectory;

    private Matlab Matlab = new Matlab();

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().debug("Packaging: " + artifactId);
        String finalFilePath = (Paths.get(outputDirectory,finalName + ".mltbx")).toString();
        String matlabCommand = Matlab.addDependencies(dependenciesDirectory, "compile") +
                               "mtlbxFile = '" + finalFilePath + "';\n" +
                               "if( ~isempty( which( 'fx.maven.command.addsandbox' ) ) )\n" +
                               "    fprintf( 1, 'Found MATLAB Maven Toolbox\\n' )\n" +
                               "    fx.maven.command.packagesandbox( pwd, 'OutputFile', mtlbxFile );\n" +
                               "else\n" +
                               "    fprintf( 1, 'Did not find MATLAB Maven Toolbox, using minimal\\n' )\n" +
                               "    addpath( fullfile( pwd, 'code', '" + artifactId + "' ) );\n" +
                               "    drawnow();pause(0.1);fprintf(' \\b');\n" +
                               "    prjFile = fullfile( pwd, '" + artifactId + ".prj' );\n" +
                               "    fprintf( 1, 'Attempting to package \"%s\" into \"%s\"\\n', prjFile, mtlbxFile );\n" +
                               "    matlab.addons.toolbox.packageToolbox( prjFile, mtlbxFile );\n" +
                               "end";
        try{
            // Make sure target exists
            Files.createDirectories(Paths.get(outputDirectory));
            Matlab.runMatlabCommand(matlabPath, matlabCommand, preserveLog, getLog());
            copyRuntimeDependencies();
        } catch(IOException exception) {
            throw new MojoExecutionException("An IO error occured: " + exception.getMessage());
        } catch(InterruptedException exception) {
            throw new MojoExecutionException("The process was interupted: " + exception.getMessage());
        } catch(MatlabException exception) {
            throw new MojoFailureException("MATLAB did not run to completion: " + exception.getMessage());
        }
        this.project.getArtifact().setFile(new File(finalFilePath));
    }

    private void copyRuntimeDependencies() throws IOException {
        File runtimeDependencyFolder = new File((Paths.get(dependenciesDirectory,"runtime")).toString());
        File outputFolder = new File((Paths.get(outputDirectory,"for_redistribution")).toString());
        File mltbxFile = new File((Paths.get(outputDirectory,finalName + ".mltbx")).toString());
        File redistMltbxFile = new File((Paths.get(outputFolder.getAbsolutePath(),finalName + ".mltbx")).toString());
        FileUtils.copyFile(mltbxFile,redistMltbxFile);
        if( runtimeDependencyFolder.exists() ) {
            FileUtils.copyDirectory(runtimeDependencyFolder,outputFolder);
        }
    }

}
