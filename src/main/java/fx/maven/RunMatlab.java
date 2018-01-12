package fx.maven;

import java.lang.InterruptedException;

import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "run")
public class RunMatlab extends AbstractMojo {

    private Matlab Matlab = new Matlab();

    @Parameter( property = "matlabPath", required = true, readonly = true, defaultValue = "C:\\Program Files\\MATLAB\\R2017b\\bin\\matlab.exe" )
    private String matlabPath;

    @Parameter( property = "matlabCommand", required = true, readonly = true )
    private String matlabCommand;

    @Parameter( property = "preserveLog", required = true, readonly = true, defaultValue = "false" )
    private boolean preserveLog;

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().debug("Running MALTAB: " + matlabCommand);
        try{
            Matlab.runMatlabCommand(matlabPath, matlabCommand, preserveLog, getLog());
        } catch(IOException exception) {
            throw new MojoExecutionException("An IO error occured: " + exception.getMessage());
        } catch(InterruptedException exception) {
            throw new MojoExecutionException("The process was interupted: " + exception.getMessage());
        } catch(MatlabException exception) {
            throw new MojoFailureException("MATLAB did not run to completion: " + exception.getMessage());
        }
    }



}
