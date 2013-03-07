package org.hypoport.milk.maven.plugin;

import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.versions.AbstractVersionsUpdaterMojo;
import org.codehaus.mojo.versions.api.PomHelper;
import org.codehaus.mojo.versions.change.VersionChange;
import org.codehaus.mojo.versions.change.VersionChanger;
import org.codehaus.mojo.versions.change.VersionChangerFactory;
import org.codehaus.mojo.versions.rewriting.ModifiedPomXMLEventReader;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Ensures consistent intraproject dependencies.
 *
 * @author Eric Karge
 * @aggregator
 * @goal fix-intra-project-dependencies
 * @since 2.0
 */
public class FixIntraProjectVersionsMojo extends AbstractVersionsUpdaterMojo {
  /**
   * @parameter default-value="${reactorProjects}"
   * @required
   * @readonly
   */
  private List<MavenProject> reactorProjects;

  private final List<VersionChange> versionChanges = new ArrayList<VersionChange>();

  /**
   * {@inheritDoc}
   */
  public void execute()
      throws MojoExecutionException, MojoFailureException {

    for (MavenProject project : reactorProjects) {
      versionChanges.add(new VersionChange(project.getGroupId(), project.getArtifactId(), "", project.getVersion()));
    }
    for (MavenProject project : reactorProjects) {
      process(project.getFile());
    }
  }

  @Override
  protected void update(ModifiedPomXMLEventReader pom) throws MojoExecutionException, XMLStreamException {
    try {
      final VersionChanger changer = createVersionChanger(pom);

      for (VersionChange change : versionChanges) {
        changer.apply(change);
      }
    }
    catch (IOException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private VersionChanger createVersionChanger(ModifiedPomXMLEventReader pom) throws IOException {
    final VersionChangerFactory changerFactory = new VersionChangerFactory();
    changerFactory.setPom(pom);
    changerFactory.setLog(getLog());
    changerFactory.setModel(PomHelper.getRawModel(pom));

    return changerFactory.newVersionChanger(true, false, true, true);
  }
}
