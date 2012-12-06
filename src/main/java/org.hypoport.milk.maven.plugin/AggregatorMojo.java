package org.hypoport.milk.maven.plugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.List;

/**
 * Goal which touches a timestamp file.
 *
 * @goal aggro
 * @aggregator
 * @requiresDependencyResolution
 */
public class AggregatorMojo extends AbstractMojo {

  /** @component */
  ArtifactMetadataSource metadataSource;
  /**
   * The projects in the reactor.
   *
   * @parameter expression="${reactorProjects}"
   * @readonly
   */
  List<MavenProject> reactorProjects;

  /** @parameter default-value="${localRepository}" */
  private ArtifactRepository localRepository;

  /** @parameter default-value="${project.remoteArtifactRepositories}" */
  private List<ArtifactRepository> remoteRepositories;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    List<Artifact> artifacts = new ArrayList<Artifact>(reactorProjects.size());
    for (MavenProject project : reactorProjects) {
      Artifact artifact = retrievePom(project.getArtifact());
      artifacts.add(artifact);
    }
    getLog().info(artifacts.toString());
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
}
