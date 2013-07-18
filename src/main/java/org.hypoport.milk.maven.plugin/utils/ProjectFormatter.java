package org.hypoport.milk.maven.plugin.utils;

import org.apache.maven.project.MavenProject;

public class ProjectFormatter {

  private String pattern;

  public ProjectFormatter(String pattern) {
    this.pattern = pattern;
  }

  public String formatProject(MavenProject project) {
    String result = pattern.replace("%g", project.getGroupId());
    result = result.replace("%a", project.getArtifactId());
    result = result.replace("%v", project.getVersion());
    result = result.replace("%n", project.getName());
    result = result.replace("%p", project.getPackaging());
    result = result.replace("%f", project.getFile().getPath());
    result = result.replace("%d", project.getBasedir().getPath());
    return result;
  }
}