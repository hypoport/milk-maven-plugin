package org.hypoport.milk.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal which touches a timestamp file.
 *
 * @goal hello
 */
public class MyMojo extends AbstractMojo {

  public void execute() throws MojoExecutionException {
    getLog().info("Hello");
  }
}
