package org.hypoport.milk.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.*;
import java.util.List;
import java.util.Set;

/**
 * @goal aggregator
 * @aggregator
 * @requiresDependencyResolution
 */
public class AggregatorMojo extends AbstractMojo {

  /**
   * @parameter expression="${changeSet}"
   * @required
   */
  File changeSet;

  /**
   * @parameter expression="${outputFile}"
   * @required
   */
  File outputFile;

  /**
   * @parameter expression="${basedir}"
   * @required
   * @readonly
   */
  protected File basedir;

  /**
   * @parameter expression="${reactorProjects}"
   * @readonly
   */
  List<MavenProject> reactorProjects;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    writeChangedProjects(findChangedArtifacts(readChangeSet()));
  }

  private List<String> readChangeSet() throws MojoFailureException {
    try {
      final BufferedReader reader = new BufferedReader(new FileReader(changeSet));
      return new ChangeSetReader(reader).read();
    }
    catch (IOException e) {
      throw new MojoFailureException("Failed reading changeset: " + changeSet, e);
    }
  }

  private Set<MavenProject> findChangedArtifacts(List<String> changedFiles) {
    Set<MavenProject> changedProjects = new ProjectMatcher(basedir).match(reactorProjects, changedFiles);
    getLog().info("Changed artifacts: " + changedProjects.toString());
    return changedProjects;
  }

  private void writeChangedProjects(Iterable<MavenProject> projects) throws MojoFailureException {
    try {
      final BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
      final ProjectPathExtractor transformer = new ProjectPathExtractor(basedir);
      new IteratableWriter<MavenProject>(writer, transformer).write(projects);
    }
    catch (IOException e) {
      throw new MojoFailureException("Failed writing artifact-ids to file: " + outputFile, e);
    }
  }
}
