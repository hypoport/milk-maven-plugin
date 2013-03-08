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
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.versions.api.PomHelper;
import org.codehaus.mojo.versions.change.VersionChange;
import org.codehaus.mojo.versions.change.VersionChanger;
import org.codehaus.mojo.versions.rewriting.ModifiedPomXMLEventReader;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.stax2.XMLInputFactory2;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for Versions Mojos.
 * This mainly a striped down copy of AbstractVersionsUpdaterMojo from versions-maven-plugin:2.0
 *
 * @author Eric Karge
 */
public abstract class AbstractVersionsUpdaterMojo
    extends AbstractMojo {
  protected final List<VersionChange> versionChanges = new ArrayList<VersionChange>();
  /**
   * @parameter default-value="${reactorProjects}"
   * @required
   * @readonly
   */
  protected List<MavenProject> reactorProjects;

  protected void process() throws MojoFailureException, MojoExecutionException {
    for (MavenProject project : reactorProjects) {
      process(project.getFile());
    }
  }

  /**
   * Processes the specified file. This is an extension point to allow updating a file external to the reactor.
   *
   * @param outFile The file to process.
   * @throws org.apache.maven.plugin.MojoExecutionException
   *          If things go wrong.
   * @throws org.apache.maven.plugin.MojoFailureException
   *          If things go wrong.
   * @since 1.0-alpha-1
   */
  protected void process(File outFile)
      throws MojoExecutionException, MojoFailureException {
    try {
      StringBuilder input = PomHelper.readXmlFile(outFile);
      ModifiedPomXMLEventReader newPom = newModifiedPomXER(input);

      update(newPom);

      if (newPom.isModified()) {
        writeFile(outFile, input);
      }
    }
    catch (IOException e) {
      getLog().error(e);
    }
    catch (XMLStreamException e) {
      getLog().error(e);
    }
  }

  /**
   * Creates a {@link org.codehaus.mojo.versions.rewriting.ModifiedPomXMLEventReader} from a StringBuilder.
   *
   * @param input The XML to read and modify.
   * @return The {@link org.codehaus.mojo.versions.rewriting.ModifiedPomXMLEventReader}.
   */
  protected final ModifiedPomXMLEventReader newModifiedPomXER(StringBuilder input) {
    ModifiedPomXMLEventReader newPom = null;
    try {
      XMLInputFactory inputFactory = XMLInputFactory2.newInstance();
      inputFactory.setProperty(XMLInputFactory2.P_PRESERVE_LOCATION, Boolean.TRUE);
      newPom = new ModifiedPomXMLEventReader(input, inputFactory);
    }
    catch (XMLStreamException e) {
      getLog().error(e);
    }
    return newPom;
  }

  /**
   * Writes a StringBuilder into a file.
   *
   * @param outFile The file to read.
   * @param input   The contents of the file.
   * @throws java.io.IOException when things go wrong.
   */
  protected final void writeFile(File outFile, StringBuilder input)
      throws IOException {
    Writer writer = WriterFactory.newXmlWriter(outFile);
    try {
      IOUtil.copy(input.toString(), writer);
    }
    finally {
      IOUtil.close(writer);
    }
  }

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

  protected abstract VersionChanger createVersionChanger(ModifiedPomXMLEventReader pom) throws IOException;
}
