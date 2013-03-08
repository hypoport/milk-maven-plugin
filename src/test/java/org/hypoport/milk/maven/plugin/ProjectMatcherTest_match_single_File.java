package org.hypoport.milk.maven.plugin;

import org.apache.maven.project.MavenProject;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class ProjectMatcherTest_match_single_File {

  private ProjectMatcher matcher;

  @BeforeMethod
  protected void setUp() throws Exception {
    matcher = new ProjectMatcher(new File("/project/"));
  }

  @Test
  public void ein_passendes_Artifakt() throws Exception {
    MavenProject artifact = artifactForPath("/project/hello/pom.xml");

    MavenProject result = matcher.match(asList(artifact), "hello/world");

    assertThat(result).isSameAs(artifact);
  }

  @Test
  public void kein_passendes_Artifakt() throws Exception {

    MavenProject result = matcher.match(asList(artifactForPath("/project/nein/pom.xml"), artifactForPath("/project/auch nicht/pom.xml")), "hello/world");

    assertThat(result).isNull();
  }

  @Test
  public void Parent_and_child_Module() throws Exception {
    MavenProject parent = artifactForPath("/project/parent/pom.xml");
    MavenProject child = artifactForPath("/project/parent/child/pom.xml");

    MavenProject result = matcher.match(asList(parent, child), "parent/child/path/to/file");

    assertThat(result).isSameAs(child);
  }

  @Test
  public void siblings_Module() throws Exception {
    MavenProject artifact = artifactForPath("/project/artifact/pom.xml");
    MavenProject sibling = artifactForPath("/project/artifactSibling/pom.xml");

    MavenProject result = matcher.match(asList(artifact, sibling), "artifact/path/to/file");

    assertThat(result).isSameAs(artifact);
  }

  @Test
  public void viele_Module() throws Exception {
    MavenProject parent = artifactForPath("/project/parent/pom.xml");
    MavenProject child = artifactForPath("/project/parent/child/pom.xml");
    MavenProject grandChild = artifactForPath("/project/parent/child/child/pom.xml");
    MavenProject sibling = artifactForPath("/project/parent/sibling/pom.xml");

    MavenProject result = matcher.match(asList(child, grandChild, parent, sibling), "parent/child/child/path/to/file");

    assertThat(result).isSameAs(grandChild);
  }

  private MavenProject artifactForPath(String path) {
    MavenProject artifact = mock(MavenProject.class);
    given(artifact.getFile()).willReturn(new File(path));
    return artifact;
  }
}
