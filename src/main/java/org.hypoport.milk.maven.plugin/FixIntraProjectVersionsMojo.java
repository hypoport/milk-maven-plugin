package org.hypoport.milk.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
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
   * {@inheritDoc}
   */
  public void execute()
      throws MojoExecutionException, MojoFailureException {

    for (MavenProject project : reactorProjects) {
      versionChanges.add(new VersionChange(project.getGroupId(), project.getArtifactId(), "", project.getVersion()));
    }
    process();
  }

  @Override
  protected VersionChanger createVersionChanger(ModifiedPomXMLEventReader pom) throws IOException {
    final VersionChangerFactory changerFactory = new VersionChangerFactory();
    changerFactory.setPom(pom);
    changerFactory.setLog(getLog());
    changerFactory.setModel(PomHelper.getRawModel(pom));

    return changerFactory.newVersionChanger(true, false, true, true);
  }
}
