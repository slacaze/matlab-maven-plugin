package fx.fcam;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.commons.io.FileUtils;

import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.lang.StringBuilder;
import java.lang.ProcessBuilder;
import java.lang.ProcessBuilder.Redirect;
import java.lang.InterruptedException;

@Mojo(name = "run")
public class RunMatlab extends AbstractMojo {

    private String errorFile = "maven-matlab.err";
    private String logFile = "maven-matlab.log";
    private Charset charset = Charset.defaultCharset();

    @Parameter( property = "matlabCommand", required = true, readonly = true )
    private String matlabCommand;

    @Parameter( property = "preserveLog", required = true, readonly = true, defaultValue = "false" )
    private boolean preserveLog;

    public void execute() throws MojoExecutionException {
        getLog().info("Running MALTAB: " + matlabCommand);
        File matlabFile = createMatlabFile(matlabCommand);
        String errorHandling = "file=fopen('" + errorFile + "','w');fprintf(file,'%s',e.getReport('extended','hyperlinks','off'));fprintf(2,'%s',e.getReport('extended','hyperlinks','off'));fclose(file);";
        String matlabWrapper = "try;run('" + matlabFile.getAbsolutePath() + "');exit(0);catch e;" + errorHandling + "exit(1);end;";
        runMatlab(matlabWrapper);
        matlabFile.delete();
    }

    private static File createMatlabFile(String fileContent){
        File tmpFile = new File("toRun.m");
        try{
            FileWriter writer = new FileWriter(tmpFile);
            writer.write(fileContent);
            writer.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
        return tmpFile;
    }

    private void runMatlab(String matlabCommand){
        ProcessBuilder processBuilder = new ProcessBuilder("C:\\Program Files\\MATLAB\\R2017b\\bin\\matlab.exe", "-nodesktop", "-minimize", "-nodisplay", "-wait", "-logfile", logFile, "-r", matlabCommand);
        File prefFolder = new File("MALTAB_PREF");
        prefFolder.mkdir();
        getLog().info("Using blank preferences: " + prefFolder.getAbsolutePath());
        Map<String, String> environment = processBuilder.environment();
        environment.put("MATLAB_PREFDIR", prefFolder.getAbsolutePath());
        try{
            Process process = processBuilder.start();
            int errCode = process.waitFor();
            showProcessOutput(errCode);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try{
            FileUtils.deleteDirectory(prefFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showProcessOutput(int errCode) throws IOException {
        if(errCode != 0) {
            List<String> lines = Files.readAllLines(Paths.get(errorFile), charset);
            for(String line : lines) {
                getLog().error(line);
            }
            (new File(errorFile)).delete();
        } else {
            if(!preserveLog) {
                (new File(logFile)).delete();
            }
        }
    }

}
