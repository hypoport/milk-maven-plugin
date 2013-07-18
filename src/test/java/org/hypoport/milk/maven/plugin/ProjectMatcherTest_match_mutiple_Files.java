package org.hypoport.milk.maven.plugin;

import org.apache.maven.project.MavenProject;
import org.hypoport.milk.maven.plugin.utils.ProjectMatcher;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class ProjectMatcherTest_match_mutiple_Files {

  private ProjectMatcher matcher;

  @BeforeMethod
  protected void setUp() throws Exception {
    matcher = new ProjectMatcher(new File("/project/"));
  }

  @Test
  public void zwei_Files_in_zwei_Modulen() throws Exception {
    MavenProject project = projectForPath("/project/a/pom.xml");
    MavenProject sibling = projectForPath("/project/b/pom.xml");

    Set<MavenProject> result = matcher.match(asList(project, sibling), asList("a/world", "b/world"));

    assertThat(result).containsOnly(project, sibling);
  }

  @Test
  public void kein_passendes_Artifakt() throws Exception {

    Set<MavenProject> result = matcher.match(asList(projectForPath("/project/nein/pom.xml"), projectForPath("/project/auch nicht/pom.xml")), asList("hello/world"));

    assertThat(result).isEmpty();
  }

  @Test
  public void viele_Files_in_einem_Modul() throws Exception {
    MavenProject parent = projectForPath("/project/parent/pom.xml");
    MavenProject child = projectForPath("/project/parent/child/pom.xml");

    Set<MavenProject> result = matcher.match(asList(parent, child), asList("parent/child/path/to/file", "parent/child/path/to/other/file"));

    assertThat(result).containsOnly(child);
  }

  @Test
  public void viele_Files__viele_Module() throws Exception {
    MavenProject parent = projectForPath("/project/parent/pom.xml");
    MavenProject child = projectForPath("/project/parent/child/pom.xml");
    MavenProject grandChild = projectForPath("/project/parent/child/child/pom.xml");
    MavenProject sibling = projectForPath("/project/parent/sibling/pom.xml");

    Set<MavenProject> result = matcher.match(asList(child, grandChild, parent, sibling), asList("parent/child/child/path/to/file", "parent/child/child/path/to/other/file", "parent/sibling/path/to/file"));

    assertThat(result).containsOnly(grandChild, sibling);
  }

  private MavenProject projectForPath(String path) {
    MavenProject artifact = mock(MavenProject.class);
    File pom = new File(path);
    given(artifact.getFile()).willReturn(pom);
    given(artifact.getBasedir()).willReturn(pom.getParentFile());
    return artifact;
  }
}
