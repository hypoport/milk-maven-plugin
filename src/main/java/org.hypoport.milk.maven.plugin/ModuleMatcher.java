package org.hypoport.milk.maven.plugin;

import org.apache.maven.artifact.Artifact;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * @goal dump-module-paths
 */
@SuppressWarnings("unchecked")
public class ModuleMatcher {

  private File basedir;

  public ModuleMatcher(File basedir) {
    this.basedir = basedir;
  }

  public Set<Artifact> match(Iterable<Artifact> artifacts, Iterable<String> files) {
    Set<Artifact> result = new HashSet<Artifact>();
    for (String path : files) {
      Artifact artifact = match(artifacts, path);
      if (artifact != null) {
        result.add(artifact);
      }
    }
    return result;
  }

  public Artifact match(Iterable<Artifact> artifacts, String path) {
    Artifact owner = null;
    for (Artifact artifact : artifacts) {
      if (!path.startsWith("/")) {
        path = basedir.getPath().toString() + "/" + path;
      }
      if (path.startsWith(artifact.getFile().getParent() + '/')) {
        if (owner == null || artifact.getFile().getParent().startsWith(owner.getFile().getParent())) {
          owner = artifact;
        }
      }
    }
    return owner;
  }
}