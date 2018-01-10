package fx.maven;

import java.lang.InterruptedException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.List;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.opencsv.bean.CsvToBeanBuilder;

@Mojo(name = "test")
public class Test extends AbstractMojo {

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

    @Parameter(property = "runSandboxTests", readonly = true, required = true, defaultValue="true")
    private boolean runSandboxTests;

    @Parameter(property = "runAddOnTests", readonly = true, required = true, defaultValue="true")
    private boolean runAddOnTests;

    @Parameter(property = "testsuite", readonly = true, required = true, defaultValue="${testsuites.all}")
    private String testsuite;

    @Parameter(property = "failOnFailure", readonly = true, required = true, defaultValue="true")
    private boolean failOnFailure;

    @Parameter(property = "failOnIncomplete", readonly = true, required = true, defaultValue="false")
    private boolean failOnIncomplete;

    private Matlab Matlab = new Matlab();
    private String SandboxResultsFilePath = "maven_matlab.testresults.sandbox";
    private String AddOnResultsFilePath = "maven_matlab.testresults.addon";
    private String MatlabResultsFilePath = "maven_matlab.testresults.mat";
    private char Delimiter = ';';

    public void execute() throws MojoExecutionException, MojoFailureException {
        String matlabCommand = Matlab.addDependencies(dependenciesDirectory, "compile") +
                               Matlab.addDependencies(dependenciesDirectory, "test") +
                               "runSandboxTests = " + String.valueOf(runSandboxTests) + ";\n" +
                               "runAddOnTests = " + String.valueOf(runAddOnTests) + ";\n" +
                               "if( ~isempty( which( 'fx.maven.command.addsandbox' ) ) )\n" +
                               "    fprintf( 1, 'Found MATLAB Maven Toolbox\\n' )\n" +
                               "    fx.maven.command.addsandbox();\n" +
                               "    if runSandboxTests\n" +
                               "        sandboxTestResults = fx.maven.command.testsandbox( pwd, '" + testsuite + "' );\n" +
                               "    end\n" +
                               "    if runAddOnTests\n" +
                               "        addOnTestResults = fx.maven.command.testaddon( pwd, '" + testsuite + "' );\n" +
                               "    end\n" +
                               "else\n" +
                               "    fprintf( 1, 'Did not find MATLAB Maven Toolbox, using minimal\\n' )\n" +
                               "    addpath( fullfile( pwd, 'code', '" + artifactId + "' ) );\n" +
                               "    addpath( fullfile( pwd, 'test' ) );\n" +
                               "    drawnow();pause(0.1);fprintf(' \\b');\n" +
                               "    fprintf( 1, 'Attempting to test \"" + testsuite + "\"\\n' );\n" +
                               "    if runSandboxTests\n" +
                               "        sandboxTestResults = runtests( '" + testsuite + "', 'IncludingSubpackages', true );\n" +
                               "    end\n" +
                               "    if runAddOnTests\n" +
                               "        warning( 'Maven:NoMavenToolbox', 'Can''t run AddOn test without the MATLAB Maven Toolbox.' );\n" +
                               "    end\n" +
                               "end\n" +
                               "if runSandboxTests\n" +
                               "    writetable( sandboxTestResults.table, '" + SandboxResultsFilePath + "', 'FileType', 'text', 'Delimiter', '" + String.valueOf(Delimiter) + "' );\n" +
                               "end\n" +
                               "if runAddOnTests\n" +
                               "    writetable( addOnTestResults.table, '" + AddOnResultsFilePath + "', 'FileType', 'text', 'Delimiter', '" + String.valueOf(Delimiter) + "' );\n" +
                               "end\n" +
                               "if runSandboxTests && runAddOnTests\n" +
                               "    save( '" + MatlabResultsFilePath + "', 'sandboxTestResults', 'addOnTestResults' );\n" +
                               "elseif runSandboxTests\n" +
                               "    save( '" + MatlabResultsFilePath + "', 'sandboxTestResults' );\n" +
                               "elseif runAddOnTests\n" +
                               "    save( '" + MatlabResultsFilePath + "', 'addOnTestResults' );\n" +
                               "end\n";
        try{
            if( runSandboxTests || runAddOnTests ) {
                Matlab.runMatlabCommand(matlabPath, matlabCommand, preserveLog, getLog());
                parseTestResults();
                File resultsFile = new File(SandboxResultsFilePath);
                if( resultsFile.exists() )
                {
                    resultsFile.delete();
                }
                resultsFile = new File(AddOnResultsFilePath);
                if( resultsFile.exists() )
                {
                    resultsFile.delete();
                }
                resultsFile = new File(MatlabResultsFilePath);
                if( resultsFile.exists() )
                {
                    resultsFile.delete();
                }
            }
        } catch(IOException exception) {
            throw new MojoExecutionException("An IO error occured: " + exception.getMessage());
        } catch(InterruptedException exception) {
            throw new MojoExecutionException("The process was interupted: " + exception.getMessage());
        } catch(MatlabException exception) {
            throw new MojoFailureException("MATLAB did not run to completion: " + exception.getMessage());
        }
    }

    private void parseTestResults() throws MojoExecutionException, MojoFailureException, IOException  {
        boolean failedTest = false;
        boolean incompleteTest = false;
        if( runSandboxTests ) {
            int[] sandboxResults = parseTestResultFile(SandboxResultsFilePath, "Sandbox");
            failedTest = failedTest && sandboxResults[1] > 0;
            incompleteTest = incompleteTest && sandboxResults[2] > 0;
        }
        if( runAddOnTests ) {
            int[] addOnResults = parseTestResultFile(AddOnResultsFilePath, "AddOn");
            failedTest = failedTest && addOnResults[1] > 0;
            incompleteTest = incompleteTest && addOnResults[2] > 0;
        }
        if( failOnFailure && failedTest ) {
            getLog().error("Some test have failed. Test results are not deleted:");
            getLog().error((new File(SandboxResultsFilePath)).getAbsolutePath());
            getLog().error((new File(AddOnResultsFilePath)).getAbsolutePath());
            getLog().error((new File(MatlabResultsFilePath)).getAbsolutePath());
            throw new MojoFailureException("Some test failed.");
        }
        if( failOnIncomplete && incompleteTest ) {
            getLog().error("Some test have failed. Test results are not deleted:");
            getLog().error((new File(SandboxResultsFilePath)).getAbsolutePath());
            getLog().error((new File(AddOnResultsFilePath)).getAbsolutePath());
            getLog().error((new File(MatlabResultsFilePath)).getAbsolutePath());
            throw new MojoFailureException("Some test did not complete.");
        }
    }

    private int[] parseTestResultFile(String filePath, String testType) throws MojoExecutionException, MojoFailureException, IOException  {
        File resultsFile = new File(filePath);
        FileReader reader = new FileReader(resultsFile);
        List<TestResult> results = new CsvToBeanBuilder<TestResult>(reader)
                                   .withSeparator(Delimiter)
                                   .withType(TestResult.class)
                                   .build().parse();
        int numberOfPassed = 0;
        int numberOfFailed = 0;
        int numberOfIncomplete = 0;
        getLog().debug(testType + " Test Details:");
        for(TestResult result : results) {
            getLog().debug(result.Name);
            if( result.Passed) {
                getLog().debug("  Passed");
                numberOfPassed += 1;
            }
            if( result.Failed) {
                getLog().debug("  Failed");
                numberOfFailed += 1;
            }
            if( result.Incomplete) {
                getLog().debug("  Incomplete");
                numberOfIncomplete += 1;
            }
        }
        reader.close();
        getLog().info(testType + " Test Results:");
        getLog().info("Passed:     " + Integer.toString(numberOfPassed));
        getLog().info("Failed:     " + Integer.toString(numberOfFailed));
        getLog().info("Incomplete: " + Integer.toString(numberOfIncomplete));
        return new int[] {numberOfPassed, numberOfFailed, numberOfIncomplete};
    }

}
