package org.hypoport.milk.maven.plugin;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * @goal dump-all-direct-dependencies
 * @aggregator
 */
public class DumpAllDirectDependenciesMojo extends AbstractMojo {

  /** @parameter expression="${pattern}" */
  String pattern;

  /** @parameter expression="${delimiter}" */
  String delimiter;

  /** @parameter expression="${outputFile}" */
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
      pattern = "%g:%a:%t:%v";
    }
    if (delimiter == null) {
      delimiter = ",";
    }
    DependencyFormatter dependencyFormatter = new DependencyFormatter(pattern);
    for (MavenProject project : reactorProjects) {
      output += addProjectDependencies(output, dependencyFormatter, project);
    }
    if (!output.isEmpty()) {
      write(output.substring(delimiter.length()));
    }
  }

  private String addProjectDependencies(String output, DependencyFormatter dependencyFormatter, MavenProject project) throws MojoExecutionException {
    for (Dependency dependency : project.getDependencies()) {
      output += delimiter + dependencyFormatter.formatDependency(dependency);
    }
    return output;
  }

  private void write(String output) throws MojoExecutionException {
    if (outputFile == null) {
      getLog().info("dependency-dump: " + output);
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
