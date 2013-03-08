package org.hypoport.milk.maven.plugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @goal dump-module-paths
 */
@SuppressWarnings("unchecked")
public class ProjectMatcher {

  private File basedir;

  public ProjectMatcher(File basedir) {
    this.basedir = basedir;
  }

  public Set<MavenProject> match(Iterable<MavenProject> projects, Iterable<String> files) {
    Set<MavenProject> result = new HashSet<MavenProject>();
    for (String path : files) {
      MavenProject project = match(projects, path);
      if (project != null) {
        result.add(project);
      }
    }
    return result;
  }

  public MavenProject match(Iterable<MavenProject> projects, String path) {
    MavenProject owner = null;
    for (MavenProject project : projects) {
      if (!path.startsWith("/")) {
        path = basedir.getPath().toString() + "/" + path;
      }
      if (path.startsWith(project.getFile().getParent() + '/')) {
        if (owner == null || project.getFile().getParent().startsWith(owner.getFile().getParent())) {
          owner = project;
        }
      }
    }
    return owner;
  }
}