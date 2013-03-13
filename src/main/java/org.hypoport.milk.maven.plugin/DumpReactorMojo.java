package org.hypoport.milk.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * @goal dump-reactor
 * @aggregator
 * @requiresDependencyResolution
 */
public class DumpReactorMojo extends AbstractMojo {

  /**
   * @parameter expression="${pattern}"
   */
  String pattern;

  /**
   * @parameter expression="${delimiter}"
   */
  String delimiter;

  /**
   * @parameter expression="${outputFile}"
   */
  File outputFile;

  /**
   * @parameter expression="${reactorProjects}"
   * @readonly
   */
  List<MavenProject> reactorProjects;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    String output = "";
    if (pattern == null) {
      pattern = "%g:%a:%v";
    }
    if (delimiter == null) {
      delimiter = ",";
    }
    for (MavenProject project : reactorProjects) {
      String projectString = formatProject(project);
      output += delimiter;
      output += projectString;
    }
    write(output.substring(delimiter.length()));
  }

  private String formatProject(MavenProject project) {
    String result = pattern.replaceAll("%g", project.getGroupId());
    result = result.replaceAll("%a", project.getArtifactId());
    result = result.replaceAll("%v", project.getVersion());
    result = result.replaceAll("%n", project.getName());
    result = result.replaceAll("%p", project.getPackaging());
    result = result.replaceAll("%f", project.getFile().getPath());
    result = result.replaceAll("%d", project.getBasedir().getPath());
    return result;
  }

  private void write(String output) throws MojoExecutionException {
    if (outputFile == null) {
      getLog().info(output);
    }
    else {
      FileWriter writer;
      try {
        writer = new FileWriter(outputFile);
        writer.write(output);
        writer.close();
      }
      catch (IOException e) {
        throw new MojoExecutionException(e.getMessage(), e);
      }
    }
  }
}
