package org.hypoport.milk.maven.plugin.utils;

public interface Transformer<To, From> {

  To transform(From source);
}
