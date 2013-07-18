package org.hypoport.milk.maven.plugin.utils;

import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class ProjectMatcher {

  private File basedir;

  public ProjectMatcher(File basedir) {
    this.basedir = basedir;
  }

  public Set<MavenProject> match(Iterable<MavenProject> projects, Iterable<String> files) {
    Set<MavenProject> result = new HashSet<MavenProject>();
    for (String path : files) {
      MavenProject project = match(projects, new File(basedir, path).getAbsoluteFile());
      if (project != null) {
        result.add(project);
      }
    }
    return result;
  }

  public MavenProject match(Iterable<MavenProject> projects, File path) {
    MavenProject owner = null;
    for (MavenProject project : projects) {
      File projectPath = project.getBasedir();
      if (belongsTo(project, path)) {
        if (belongsTo(owner, projectPath)) {
          owner = project;
        }
      }
    }
    return owner;
  }

  private boolean belongsTo(MavenProject project, File file) {
    return project == null || isSameOrDescendand(project.getBasedir(), file);
  }

  private boolean isSameOrDescendand(File root, File file) {
    root = root.getAbsoluteFile();
    do {
      if (root.equals(file)) {
        return true;
      }
    } while ((file = file.getParentFile()) != null);
    return false;
  }
}