package org.hypoport.milk.maven.plugin;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/** @goal dump-offline-dependencies */
public class DumpOfflineDependenciesMojo extends AbstractMojo {

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
      pattern = "%g:%a:%v";
    }
    if (delimiter == null) {
      delimiter = ",";
    }
    DependencyFormatter dependencyFormatter = new DependencyFormatter(pattern);
    for (Dependency dependency : project.getDependencies()) {
      String dependencyString = dependencyFormatter.formatDependency(dependency);
      output += delimiter + dependencyString;
      write(dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getType() + ":" + dependency.getVersion());
    }
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
