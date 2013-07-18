package org.hypoport.milk.maven.plugin.utils;

import org.apache.maven.model.Dependency;

import java.util.Comparator;

/**
* Created with IntelliJ IDEA. User: eric Date: 7/18/13 Time: 5:57 PM To change this template use File | Settings | File
* Templates.
*/
public class DependencyComparator implements Comparator<Dependency> {

  @Override
  public int compare(Dependency dependency1, Dependency dependency2) {
    return compare(getProperties(dependency1), getProperties(dependency2));
  }

  private static int compare(String[] properties1, String[] properties2) {
    for (int i = 0; i < properties1.length; ++i) {
      int result = compare(properties1[i], properties2[i]);
      if (result != 0) {
        return result;
      }
    }
    return 0;
  }

  private static int compare(String value1, String value2) {
    return value1 != null
           ? value1.compareTo(value2)
           : value2 == null ? 0 : 1;
  }

  private static String[] getProperties(Dependency o1) {
    return new String[] {o1.getGroupId(), o1.getArtifactId(), o1.getVersion(), o1.getType(), o1.getScope(), o1.getClassifier()};
  }
}
