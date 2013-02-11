package org.hypoport.milk.maven.plugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @goal aggregator
 * @aggregator
 * @requiresDependencyResolution
 */
public class AggregatorMojo extends AbstractMojo {

  /**
   * @component
   */
  ArtifactMetadataSource metadataSource;

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

  /**
   * @parameter default-value="${localRepository}"
   */
  private ArtifactRepository localRepository;

  /**
   * @parameter default-value="${project.remoteArtifactRepositories}"
   */
  private List<ArtifactRepository> remoteRepositories;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    List<String> changes = readChangeSet();
    List<Artifact> artifacts = getResolvedArtifacts();
    Set<Artifact> changedArtifacts = findChangedArtifacts(changes, artifacts);
    writeChangedArtifacts(changedArtifacts);
  }

  private List<Artifact> getResolvedArtifacts() throws MojoExecutionException {
    List<Artifact> artifacts = new ArrayList<Artifact>(reactorProjects.size());
    for (MavenProject project : reactorProjects) {
      Artifact artifact = retrievePom(project.getArtifact());
      artifacts.add(artifact);
    }
    return artifacts;
  }

  private Artifact retrievePom(Artifact artifact) throws MojoExecutionException {
    try {
      return metadataSource.retrieve(artifact, localRepository, remoteRepositories).getPomArtifact();
    }
    catch (ArtifactMetadataRetrievalException e) {
      getLog().error("Retrieving pom.xml for: " + artifact);
      throw new MojoExecutionException("Retrieving pom.xml for: " + artifact, e);
    }
  }

  private Set<Artifact> findChangedArtifacts(List<String> changes, List<Artifact> artifacts) {
    Set<Artifact> changedArtifacts = new ModuleMatcher(basedir).match(artifacts, changes);
    getLog().info("Changed artifacts: " + changedArtifacts.toString());
    return changedArtifacts;
  }

  private List<String> readChangeSet() throws MojoFailureException {
    try {
      return new ChangeSetReader().readChangeSet(changeSet);
    }
    catch (IOException e) {
      throw new MojoFailureException("Failed reading changeset: " + changeSet, e);
    }
  }

  private void writeChangedArtifacts(Iterable<Artifact> artifacts) throws MojoFailureException {
    try {
      new ArtifactSetWriter(basedir).writeArtifacts(artifacts, outputFile);
    }
    catch (IOException e) {
      throw new MojoFailureException("Failed writing artifact-ids to file: " + outputFile, e);
    }
  }
}
