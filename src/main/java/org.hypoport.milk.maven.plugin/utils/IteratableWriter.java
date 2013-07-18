package org.hypoport.milk.maven.plugin.utils;

import java.io.BufferedWriter;
import java.io.IOException;

public class IteratableWriter<T> {

  private final Transformer<String, T> transformer;
  private final BufferedWriter writer;

  public IteratableWriter(BufferedWriter writer, Transformer<String, T> transformer) {
    this.transformer = transformer;
    this.writer = writer;
  }

  public void write(Iterable<T> iterable) throws IOException {
    try {
      boolean first = true;
      for (T element : iterable) {
        first = writeCommaIfNotFirstElement(writer, first);
        writer.write(transformer.transform(element));
      }
      writer.newLine();
    }
    finally {
      writer.close();
    }
  }

  private boolean writeCommaIfNotFirstElement(BufferedWriter writer, boolean first) throws IOException {
    if (!first) {
      writer.write(",");
    }
    return false;
  }
}
