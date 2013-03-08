package org.hypoport.milk.maven.plugin;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChangeSetReader {

  private BufferedReader reader;

  public ChangeSetReader(BufferedReader reader) throws FileNotFoundException {
    this.reader = reader;
  }

  List<String> read() throws IOException {
    ArrayList<String> changes = new ArrayList<String>();
    try {
      String line;
      while ((line = reader.readLine()) != null) {
        changes.add(line);
      }
      return changes;
    }
    finally {
      reader.close();
    }
  }

}
