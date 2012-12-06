package org.hypoport.milk.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.command.diff.DiffScmResult;
import org.apache.maven.scm.plugin.AbstractScmMojo;
import org.apache.maven.scm.repository.ScmRepository;

import java.io.IOException;

/**
 * Goal which touches a timestamp file.
 *
 * @goal diff
 */
public class DiffMojo extends AbstractScmMojo {

  public void execute() throws MojoExecutionException {
    super.execute();

    try {
      ScmRepository repository = getScmRepository();

      DiffScmResult result = getScmManager().diff(repository, getFileSet(), null, null);

      getLog().info("Changed Files:");
      for (ScmFile scmFile : result.getChangedFiles()) {
        getLog().info(scmFile.getPath());
      }
    }
    catch (IOException e) {
      throw new MojoExecutionException("Cannot run diff command : ", e);
    }
    catch (ScmException e) {
      throw new MojoExecutionException("Cannot run diff command : ", e);
    }
  }
}
