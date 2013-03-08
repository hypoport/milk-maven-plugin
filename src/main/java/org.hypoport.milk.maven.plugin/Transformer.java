package org.hypoport.milk.maven.plugin;

public interface Transformer<To, From> {

  To transform(From source);
}
