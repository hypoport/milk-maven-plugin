package org.hypoport.milk.maven.plugin;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.hypoport.milk.maven.plugin.utils.DependencyFormatter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/** @goal dump-direct-dependencies */
public class DumpDirectDependenciesMojo extends AbstractMojo {

  /** @parameter expression="${pattern}" */
  String pattern;

  /** @parameter expression="${delimiter}" */
  String delimiter;

  /** @parameter expression="${outputFile}" */
  File outputFile;

  /**
   * @parameter expression="${project}"
   * @readonly
   */
  MavenProject project;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    String output = "";
    if (pattern == null) {
      pattern = "%g:%a:%t:%v";
    }
    if (delimiter == null) {
      delimiter = ",";
    }
    DependencyFormatter dependencyFormatter = new DependencyFormatter(pattern);
    for (Dependency dependency : project.getDependencies()) {
      output += delimiter + dependencyFormatter.formatDependency(dependency);
    }
    if (!output.isEmpty()) {
      write(output.substring(delimiter.length()));
    }
  }

  private void write(String output) throws MojoExecutionException {
    if (outputFile == null) {
      getLog().info("dependency-dump: " + output);
    }
    else {
      FileWriter writer = null;
      try {
        writer = new FileWriter(outputFile);
        writer.write(output);
      }
      catch (IOException e) {
        throw new MojoExecutionException(e.getMessage(), e);
      }
      finally {
        if (writer != null) {
          try {
            writer.close();
          }
          catch (IOException e) {
            getLog().error("could not close " + outputFile, e);
          }
        }
      }
    }
  }
}
