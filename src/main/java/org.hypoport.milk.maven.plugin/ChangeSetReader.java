package org.hypoport.milk.maven.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChangeSetReader {

  List<String> readChangeSet(File changeSet) throws IOException {
    ArrayList<String> changes = new ArrayList<String>();
    BufferedReader reader = createReader(changeSet);
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

  private BufferedReader createReader(File changeSet) throws FileNotFoundException {
    return new BufferedReader(new FileReader(changeSet));
  }
}
