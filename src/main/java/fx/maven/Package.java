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
        getLog().debug("Trying to package: " + artifactId);
        String finalFilePath = outputDirectory +"/"+ finalName + ".mltbx";
        String matlabCommand = "compileDependenciesPath = fullfile( '" + dependenciesDirectory + "', 'compile' );\n" +
                               "fprintf( 1, 'Looking for dependencies in \"%s\"\\n', compileDependenciesPath )\n" +
                               "compileDependencies = dir( fullfile( compileDependenciesPath, '*.mltbx' ) );\n" +
                               "for dependencyIndex = 1:numel( compileDependencies )\n" +
                               "    thisDependency = compileDependencies(dependencyIndex);\n" +
                               "    fprintf( 1, 'Installing \"%s\"\\n', thisDependency.name )\n" +
                               "    matlab.addons.toolbox.installToolbox( fullfile( thisDependency.folder, thisDependency.name ) );\n" +
                               "end\n" +
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
        } catch(IOException exception) {
            throw new MojoExecutionException("An IO error occured: " + exception.getMessage());
        } catch(InterruptedException exception) {
            throw new MojoExecutionException("The process was interupted: " + exception.getMessage());
        } catch(MatlabException exception) {
            throw new MojoFailureException("MATLAB did not run to completion: " + exception.getMessage());
        }
        this.project.getArtifact().setFile(new File(finalFilePath));
    }
}
