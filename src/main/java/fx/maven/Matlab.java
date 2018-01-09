package fx.maven;

import java.lang.ProcessBuilder;
import java.lang.InterruptedException;
import java.lang.Throwable;

import java.util.Map;
import java.util.List;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;

import org.apache.maven.plugin.logging.Log;

public class Matlab {

    private Charset DefaultCharset = Charset.defaultCharset();

    private String MatlabFilePath = "maven_matlab.m";
    private String ErrorFilePath = "maven_matlab.err";
    private String LogFilePath = "maven_matlab.log";
    private String PreferenceFolderPath = "MALTAB_PREFERENCES";

    private Log MavenLog;

    protected void runMatlabCommand(String matlabPath, String matlabCommand, boolean preserveLog, Log mavenLog) throws IOException, InterruptedException, MatlabException {
        MavenLog = mavenLog;
        File matlabFile = createMatlabFile(matlabCommand);
        String separator = "    fprintf(1,'%s\\n\\n',repmat('=',1,20));\n";
        String matlabWrapper = "try\n" +
                               separator +
                               "    fprintf(1,'MATLAB Version: %s\\n\\n',version);\n" +
                               separator +
                               "    fprintf(1,'Prefdir:   %s\\n\\n',prefdir);\n" +
                               "    s = settings;\n" +
                               separator +
                               "    s.matlab.addons.InstallationFolder.TemporaryValue=fullfile(pwd,'MATLAB_AddOns');\n" +
                               "    fprintf(1,'AddOn Dir: %s\\n\\n',s.matlab.addons.InstallationFolder.ActiveValue);\n" +
                               separator +
                               "    fprintf(1,'Restore default path\\n\\n');\n" +
                               "    restoredefaultpath;\n" +
                               separator +
                               "    fprintf(1,'Ver output:\\n\\n',version);\n" +
                               "    ver;\n" +
                               "    fprintf(1,'\\n');\n" +
                               separator +
                               "    fprintf(1,'Running:\\n\\n');\n" +
                               "    type('" + matlabFile.getAbsolutePath() + "');\n" +
                               "    fprintf(1,'\\n');\n" +
                               separator +
                               "    fprintf(1,'Output:\\n\\n');\n" +
                               "    run('" + matlabFile.getAbsolutePath() + "');\n" +
                               "    exit(0);\n" +
                               "catch matlabException\n" +
                               "    file=fopen('" + ErrorFilePath + "','w');\n" +
                               "    fprintf(file,'%s',matlabException.getReport('extended','hyperlinks','off'));\n" +
                               separator +
                               "    fprintf(2,'%s',matlabException.getReport('extended','hyperlinks','off'));\n" +
                               "    fclose(file);\n" +
                               "    exit(1);\n" +
                               "end\n";
        String[] matlabLines = matlabWrapper.split("\n");
        MavenLog.debug("=======================================================================");
        MavenLog.debug("Executing in MALTAB:");
        for(String line : matlabLines) {
            MavenLog.debug(line);
        }
        MavenLog.debug("=======================================================================");
        try{
            runMatlab(matlabPath, matlabWrapper, preserveLog);
        } catch (IOException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            throw exception;
        } catch (MatlabException exception) {
            throw exception;
        } finally {
            matlabFile.delete();
        }
    }

    private File createMatlabFile(String fileContent) throws IOException {
        File matlabFile = new File(MatlabFilePath);
        try{
            FileWriter writer = new FileWriter(matlabFile);
            writer.write(fileContent);
            writer.close();
        } catch(IOException exception) {
            showStackTrace(exception);
            throw exception;
        }
        return matlabFile;
    }

    private void runMatlab(String matlabPath, String matlabCommand, boolean preserveLog) throws IOException, InterruptedException, MatlabException {
        ProcessBuilder processBuilder = new ProcessBuilder(matlabPath, "-nodesktop", "-minimize", "-nodisplay", "-wait", "-logfile", LogFilePath, "-r", matlabCommand);
        File preferenceFolder = new File(PreferenceFolderPath);
        preferenceFolder.mkdir();
        MavenLog.debug("Using blank preferences: " + preferenceFolder.getAbsolutePath());
        Map<String, String> environment = processBuilder.environment();
        environment.put("MATLAB_PREFDIR", preferenceFolder.getAbsolutePath());
        try{
            Process process = processBuilder.start();
            int errCode = process.waitFor();
            showProcessOutput(errCode);
        } catch (IOException exception) {
            showStackTrace(exception);
            throw exception;
        } catch (InterruptedException exception) {
            showStackTrace(exception);
            throw exception;
        } catch (MatlabException exception) {
            throw exception;
        } finally {
            try{
                if(!preserveLog) {
                    (new File(LogFilePath)).delete();
                }
                FileUtils.deleteDirectory(preferenceFolder);
            } catch (IOException exception) {
                showStackTrace(exception);
                throw exception;
            }
        }
    }

    private void showProcessOutput(int errCode) throws IOException, MatlabException {
        if(errCode != 0) {
            List<String> lines = Files.readAllLines(Paths.get(ErrorFilePath), DefaultCharset);
            MavenLog.debug("=======================================================================");
            MavenLog.debug("Matlab Error:");
            for(String line : lines) {
                MavenLog.debug(line);
            }
            (new File(ErrorFilePath)).delete();
            throw new MatlabException(lines.get(0));
        }
    }

    private void showStackTrace(Throwable exception){
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exception.printStackTrace(printWriter);
        String stackTrace = stringWriter.toString();
        String[] traceLines = stackTrace.split("\n");
        MavenLog.debug("=======================================================================");
        MavenLog.debug("Java Error:");
        for(String line : traceLines) {
            MavenLog.debug(line);
        }
    }

}
