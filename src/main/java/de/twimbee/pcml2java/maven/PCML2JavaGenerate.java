package de.twimbee.pcml2java.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import de.twimbee.pcml2java.PCML2Java;

/**
 * @goal gensrc
 * @phase generate-sources
 */
public class PCML2JavaGenerate extends AbstractMojo {

    /**
     * The package name for the generated classes
     * 
     * @parameter
     * @required
     */
    private String packageName;

    /**
     * The source folder to scan for PCML-Files
     * 
     * @parameter
     * @required
     */
    private String sourceFolder;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        PCML2Java pcml2Java = new PCML2Java();
        getLog().error("generating for " + packageName + " from " + sourceFolder);
        pcml2Java.createJavaClassesForPCMLFiles(packageName, sourceFolder);
    }

}
