package org.hypoport.milk.maven.plugin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * Update the POM versions for a project. This performs the normal version updates of the <tt>release:prepare</tt> goal without
 * making other modifications to the SCM such as tagging. For more info see <a href="http://maven.apache.org/plugins/maven-release-plugin/examples/update-versions.html">http://maven.apache.org/plugins/maven-release-plugin/examples/update-versions.html</a>.
 *
 * @author Paul Gier
 * @version $Id: UpdateVersionsMojo.java 1333180 2012-05-02 20:04:26Z rfscholte $
 * @aggregator
 * @goal update-versions
 * @since 2.0
 */
public class UpdateVersionsMojo
    extends AbstractMojo {

  /**
   * @parameter default-value="${newVersion}"
   * @required
   * @readonly
   */
  private String newVersion;

  /**
   * @parameter default-value="${basedir}"
   * @required
   * @readonly
   */
  private File basedir;

  /**
   * @parameter default-value="${settings}"
   * @required
   * @readonly
   */
  private Settings settings;

  /**
   * @parameter default-value="${project}"
   * @required
   * @readonly
   */
  protected MavenProject project;

  /**
   * Additional arguments to pass to the Maven executions, separated by spaces.
   *
   * @parameter expression="${arguments}" alias="prepareVerifyArgs"
   */
  private String arguments;
  /**
   * The file name of the POM to execute any goals against.
   *
   * @parameter expression="${pomFileName}"
   */
  private String pomFileName;
  /**
   * @parameter default-value="${reactorProjects}"
   * @required
   * @readonly
   */
  private List<MavenProject> reactorProjects;

  /**
   * @parameter default-value="${session}"
   * @readonly
   * @required
   * @since 2.0
   */
  protected MavenSession session;

  /** {@inheritDoc} */
  public void execute()
      throws MojoExecutionException, MojoFailureException {

    ReleaseDescriptor config = createReleaseDescriptor();

    // Create a config containing values from the session properties (ie command line properties with cli).
    ReleaseDescriptor sysPropertiesConfig
        = ReleaseUtils.copyPropertiesToReleaseDescriptor(session.getExecutionProperties());
    mergeCommandLineConfig(config, sysPropertiesConfig);

    try {
      for (MavenProject project : reactorProjects) {
        String projectId = ArtifactUtils.versionlessKey(project.getGroupId(), project.getArtifactId());
        config.mapReleaseVersion(projectId, newVersion);
      }
      new PomVersionRewriter().transform(config, reactorProjects);
    }
    catch (ReleaseExecutionException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
    catch (ReleaseFailureException e) {
      throw new MojoFailureException(e.getMessage(), e);
    }
  }

  /**
   * Creates the release descriptor from the various goal parameters.
   *
   * @return The release descriptor, never <code>null</code>.
   */
  protected ReleaseDescriptor createReleaseDescriptor() {
    ReleaseDescriptor descriptor = new ReleaseDescriptor();

    descriptor.setInteractive(settings.isInteractiveMode());

    descriptor.setWorkingDirectory(basedir.getAbsolutePath());

    descriptor.setPomFileName(pomFileName);

    descriptor.setLocalCheckout(false);

    descriptor.setPushChanges(false);

    @SuppressWarnings("unchecked")
    List<Profile> profiles = project.getActiveProfiles();

    String arguments = this.arguments;
    if (profiles != null && !profiles.isEmpty()) {
      if (!StringUtils.isEmpty(arguments)) {
        arguments += " -P ";
      }
      else {
        arguments = "-P ";
      }

      for (Iterator<Profile> it = profiles.iterator(); it.hasNext(); ) {
        Profile profile = it.next();

        arguments += profile.getId();
        if (it.hasNext()) {
          arguments += ",";
        }
      }
    }
    descriptor.setAdditionalArguments(arguments);

    return descriptor;
  }

  /**
   * This method takes some of the release configuration picked up from the command line system properties and copies it into the
   * release config object.
   *
   * @param config The release configuration to merge the system properties into, must not be <code>null</code>.
   * @param sysPropertiesConfig The configuration from the system properties to merge in, must not be <code>null</code>.
   */
  protected void mergeCommandLineConfig(ReleaseDescriptor config, ReleaseDescriptor sysPropertiesConfig) {
    // If the user specifies versions, these should override the existing versions
    if (sysPropertiesConfig.getReleaseVersions() != null) {
      config.getReleaseVersions().putAll(sysPropertiesConfig.getReleaseVersions());
    }
    if (sysPropertiesConfig.getDevelopmentVersions() != null) {
      config.getDevelopmentVersions().putAll(sysPropertiesConfig.getDevelopmentVersions());
    }
  }
}
