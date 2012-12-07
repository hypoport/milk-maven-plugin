package org.hypoport.milk.maven.plugin;

import org.apache.maven.artifact.Artifact;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class ModuleMatcherTest_match_mutiple_Files {

  private ModuleMatcher matcher;

  @BeforeMethod
  protected void setUp() throws Exception {
    matcher = new ModuleMatcher(new File("/project/"));
  }

  @Test
  public void zwei_Files_in_zwei_Modulen() throws Exception {
    Artifact artifact = artifactForPath("/project/a/pom.xml");
    Artifact sibling = artifactForPath("/project/b/pom.xml");

    Set<Artifact> result = matcher.match(asList(artifact, sibling), asList("a/world", "b/world"));

    assertThat(result).containsOnly(artifact, sibling);
  }

  @Test
  public void kein_passendes_Artifakt() throws Exception {

    Set<Artifact> result = matcher.match(asList(artifactForPath("/project/nein/pom.xml"), artifactForPath("/project/auch nicht/pom.xml")), asList("hello/world"));

    assertThat(result).isEmpty();
  }

  @Test
  public void viele_Files_in_einem_Modul() throws Exception {
    Artifact parent = artifactForPath("/project/parent/pom.xml");
    Artifact child = artifactForPath("/project/parent/child/pom.xml");

    Set<Artifact> result = matcher.match(asList(parent, child), asList("parent/child/path/to/file", "parent/child/path/to/other/file"));

    assertThat(result).containsOnly(child);
  }

  @Test
  public void viele_Files__viele_Module() throws Exception {
    Artifact parent = artifactForPath("/project/parent/pom.xml");
    Artifact child = artifactForPath("/project/parent/child/pom.xml");
    Artifact grandChild = artifactForPath("/project/parent/child/child/pom.xml");
    Artifact sibling = artifactForPath("/project/parent/sibling/pom.xml");

    Set<Artifact> result = matcher.match(asList(child, grandChild, parent, sibling), asList("parent/child/child/path/to/file", "parent/child/child/path/to/other/file", "parent/sibling/path/to/file"));

    assertThat(result).containsOnly(grandChild, sibling);
  }

  private Artifact artifactForPath(String path) {
    Artifact artifact = mock(Artifact.class);
    given(artifact.getFile()).willReturn(new File(path));
    return artifact;
  }
}
