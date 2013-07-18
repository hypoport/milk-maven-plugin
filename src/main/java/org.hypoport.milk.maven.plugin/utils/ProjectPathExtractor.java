package org.hypoport.milk.maven.plugin.utils;

import org.apache.maven.project.MavenProject;
import org.hypoport.milk.maven.plugin.utils.Transformer;

import java.io.File;

public class ProjectPathExtractor implements Transformer<String, MavenProject> {
  File basedir;

  public ProjectPathExtractor(File basedir) {
    this.basedir = basedir;
  }

  public String transform(MavenProject project) {
    File path = project.getBasedir();
    if (basedir.equals(path)) {
      return ".";
    }
    return path.getPath().substring(this.basedir.getPath().length() + 1);
  }
}