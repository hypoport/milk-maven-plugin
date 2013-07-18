package org.hypoport.milk.maven.plugin;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.hypoport.milk.maven.plugin.utils.DependencyComparator;
import org.hypoport.milk.maven.plugin.utils.DependencyFormatter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
    Set<Dependency> dependencies = new TreeSet<Dependency>(new DependencyComparator());
    DependencyFormatter dependencyFormatter = new DependencyFormatter(pattern);
    for (MavenProject project : reactorProjects) {
      dependencies.addAll(project.getDependencies());
    }
    for (Dependency dependency : dependencies) {
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
