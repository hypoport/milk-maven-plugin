package org.hypoport.milk.maven.plugin;

import org.apache.maven.project.MavenProject;
import org.hypoport.milk.maven.plugin.utils.ProjectMatcher;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class ProjectMatcherTest_match_single_File {

  private ProjectMatcher matcher;
  private File basedir;

  @BeforeMethod
  protected void setUp() throws Exception {
    basedir = new File("/project/");
    matcher = new ProjectMatcher(basedir);
  }

  @Test
  public void ein_passendes_Artifakt() throws Exception {
    MavenProject artifact = projectForPath("/project/hello/pom.xml");

    MavenProject result = matcher.match(asList(artifact), new File(basedir, "hello/world").getAbsoluteFile());

    assertThat(result).isSameAs(artifact);
  }

  @Test
  public void kein_passendes_Artifakt() throws Exception {

    MavenProject result = matcher.match(asList(projectForPath("/project/nein/pom.xml"), projectForPath("/project/auch nicht/pom.xml")), new File(basedir, "hello/world").getAbsoluteFile());

    assertThat(result).isNull();
  }

  @Test
  public void Parent_and_child_Module() throws Exception {
    MavenProject parent = projectForPath("/project/parent/pom.xml");
    MavenProject child = projectForPath("/project/parent/child/pom.xml");

    MavenProject result = matcher.match(asList(parent, child), new File(basedir, "parent/child/path/to/file").getAbsoluteFile());

    assertThat(result).isSameAs(child);
  }

  @Test
  public void siblings_Module() throws Exception {
    MavenProject artifact = projectForPath("/project/artifact/pom.xml");
    MavenProject sibling = projectForPath("/project/artifactSibling/pom.xml");

    MavenProject result = matcher.match(asList(artifact, sibling), new File(basedir, "artifact/path/to/file").getAbsoluteFile());

    assertThat(result).isSameAs(artifact);
  }

  @Test
  public void viele_Module() throws Exception {
    MavenProject parent = projectForPath("/project/parent/pom.xml");
    MavenProject child = projectForPath("/project/parent/child/pom.xml");
    MavenProject grandChild = projectForPath("/project/parent/child/child/pom.xml");
    MavenProject sibling = projectForPath("/project/parent/sibling/pom.xml");

    MavenProject result = matcher.match(asList(child, grandChild, parent, sibling), new File(basedir, "parent/child/child/path/to/file").getAbsoluteFile());

    assertThat(result).isSameAs(grandChild);
  }

  private MavenProject projectForPath(String path) {
    MavenProject project = mock(MavenProject.class);
    File pom = new File(path);
    given(project.getFile()).willReturn(pom);
    given(project.getBasedir()).willReturn(pom.getParentFile());
    return project;
  }
}
