package org.hypoport.milk.maven.plugin.utils;

import org.apache.maven.model.Dependency;

public class DependencyFormatter {

  private String pattern;

  public DependencyFormatter(String pattern) {
    this.pattern = pattern;
  }

  public String formatDependency(Dependency dependency) {
    String result = pattern;
    result = result.replace("%g", dependency.getGroupId());
    result = result.replace("%a", dependency.getArtifactId());
    result = result.replace("%v", dependency.getVersion());
    result = result.replace("%t", dependency.getType());
    result = result.replace("%c", dependency.getClassifier() != null ? dependency.getClassifier() : "");
    result = result.replace("%s", dependency.getScope());
    return result;
  }
}
