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
 * Update the POM versions for a project.
 *
 * @author Eric Karge
 * @aggregator
 * @goal update-versions
 * @since 2.0
 */
public class UpdateVersionsMojo
    extends AbstractVersionsUpdaterMojo {

  private final List<VersionChange> versionChanges = new ArrayList<VersionChange>();
  /**
   * @parameter default-value="${newVersion}"
   * @required
   * @readonly
   */
  private String newVersion;

  /**
   * {@inheritDoc}
   */
  public void execute()
      throws MojoExecutionException, MojoFailureException {

    for (MavenProject project : reactorProjects) {
      versionChanges.add(new VersionChange(project.getGroupId(), project.getArtifactId(), "", newVersion));
    }
    process();
  }

  @Override
  protected VersionChanger createVersionChanger(ModifiedPomXMLEventReader pom) throws IOException {
    final VersionChangerFactory changerFactory = new VersionChangerFactory();
    changerFactory.setPom(pom);
    changerFactory.setLog(getLog());
    changerFactory.setModel(PomHelper.getRawModel(pom));

    return changerFactory.newVersionChanger();
  }
}
