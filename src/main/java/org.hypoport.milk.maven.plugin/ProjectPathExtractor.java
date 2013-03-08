package org.hypoport.milk.maven.plugin;

import org.apache.maven.project.MavenProject;

import java.io.File;

public class ProjectPathExtractor implements Transformer<String, MavenProject> {
  File basedir;

  public ProjectPathExtractor(File basedir) {
    this.basedir = basedir;
  }

  public String transform(MavenProject project) {
    String path = project.getFile().getParent();
    if (basedir.getPath().equals(path)) {
      return ".";
    }
    return path.substring(this.basedir.getPath().length() + 1);
  }
}