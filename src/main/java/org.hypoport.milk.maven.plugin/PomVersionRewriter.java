package org.hypoport.milk.maven.plugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.ObjectBasedValueSource;
import org.codehaus.plexus.interpolation.PrefixAwareRecursionInterceptor;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;
import org.jdom.CDATA;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.filter.ContentFilter;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PomVersionRewriter {

  private String ls = ReleaseUtil.LS;

  public void transform(ReleaseDescriptor releaseDescriptor,
                        List<MavenProject> reactorProjects)
      throws ReleaseExecutionException, ReleaseFailureException {
    ReleaseResult result = new ReleaseResult();
    for (MavenProject project : reactorProjects) {
      transformProject(project, releaseDescriptor, reactorProjects, result);
    }
    result.setResultCode(ReleaseResult.SUCCESS);
  }

  private void transformProject(MavenProject project, ReleaseDescriptor releaseDescriptor,
                                List<MavenProject> reactorProjects,
                                ReleaseResult result)
      throws ReleaseExecutionException, ReleaseFailureException {
    Document document;
    String intro = null;
    String outtro = null;
    try {
      String content = ReleaseUtil.readXmlFile(ReleaseUtil.getStandardPom(project), ls);
      // we need to eliminate any extra whitespace inside elements, as JDOM will nuke it
      content = content.replaceAll("<([^!][^>]*?)\\s{2,}([^>]*?)>", "<$1 $2>");
      content = content.replaceAll("(\\s{2,}|[^\\s])/>", "$1 />");

      SAXBuilder builder = new SAXBuilder();
      document = builder.build(new StringReader(content));

      // Normalize line endings to platform's style (XML processors like JDOM normalize line endings to "\n" as
      // per section 2.11 of the XML spec)
      normaliseLineEndings(document);

      // rewrite DOM as a string to find differences, since text outside the root element is not tracked
      StringWriter w = new StringWriter();
      Format format = Format.getRawFormat();
      format.setLineSeparator(ls);
      XMLOutputter out = new XMLOutputter(format);
      out.output(document.getRootElement(), w);

      int index = content.indexOf(w.toString());
      if (index >= 0) {
        intro = content.substring(0, index);
        outtro = content.substring(index + w.toString().length());
      }
      else {
        /*
        * NOTE: Due to whitespace, attribute reordering or entity expansion the above indexOf test can easily
        * fail. So let's try harder. Maybe some day, when JDOM offers a StaxBuilder and this builder employes
        * XMLInputFactory2.P_REPORT_PROLOG_WHITESPACE, this whole mess can be avoided.
        */
        final String SPACE = "\\s++";
        final String XML = "<\\?(?:(?:[^\"'>]++)|(?:\"[^\"]*+\")|(?:'[^\']*+'))*+>";
        final String INTSUB = "\\[(?:(?:[^\"'\\]]++)|(?:\"[^\"]*+\")|(?:'[^\']*+'))*+\\]";
        final String DOCTYPE =
            "<!DOCTYPE(?:(?:[^\"'\\[>]++)|(?:\"[^\"]*+\")|(?:'[^\']*+')|(?:" + INTSUB + "))*+>";
        final String PI = XML;
        final String COMMENT = "<!--(?:[^-]|(?:-[^-]))*+-->";

        final String INTRO =
            "(?:(?:" + SPACE + ")|(?:" + XML + ")|(?:" + DOCTYPE + ")|(?:" + COMMENT + ")|(?:" + PI + "))*";
        final String OUTRO = "(?:(?:" + SPACE + ")|(?:" + COMMENT + ")|(?:" + PI + "))*";
        final String POM = "(?s)(" + INTRO + ")(.*?)(" + OUTRO + ")";

        Matcher matcher = Pattern.compile(POM).matcher(content);
        if (matcher.matches()) {
          intro = matcher.group(1);
          outtro = matcher.group(matcher.groupCount());
        }
      }
    }
    catch (JDOMException e) {
      throw new ReleaseExecutionException("Error reading POM: " + e.getMessage(), e);
    }
    catch (IOException e) {
      throw new ReleaseExecutionException("Error reading POM: " + e.getMessage(), e);
    }

    transformDocument(project, document.getRootElement(), releaseDescriptor, reactorProjects,
                      result);

    File pomFile = ReleaseUtil.getStandardPom(project);

    writePom(pomFile, document, releaseDescriptor, project.getModelVersion(), intro, outtro);
  }

  private void normaliseLineEndings(Document document) {
    for (Iterator<?> i = document.getDescendants(new ContentFilter(ContentFilter.COMMENT)); i.hasNext(); ) {
      Comment c = (Comment) i.next();
      c.setText(ReleaseUtil.normalizeLineEndings(c.getText(), ls));
    }
    for (Iterator<?> i = document.getDescendants(new ContentFilter(ContentFilter.CDATA)); i.hasNext(); ) {
      CDATA c = (CDATA) i.next();
      c.setText(ReleaseUtil.normalizeLineEndings(c.getText(), ls));
    }
  }

  private void transformDocument(MavenProject project, Element rootElement, ReleaseDescriptor releaseDescriptor,
                                 List<MavenProject> reactorProjects, ReleaseResult result)
      throws ReleaseExecutionException, ReleaseFailureException {
    Namespace namespace = rootElement.getNamespace();
    Map<String, String> mappedVersions = getNextVersionMap(releaseDescriptor);
    Map<String, String> originalVersions = getOriginalVersionMap(releaseDescriptor, reactorProjects);
    @SuppressWarnings("unchecked")
    Map<String, Map<String, String>> resolvedSnapshotDependencies = releaseDescriptor.getResolvedSnapshotDependencies();
    Model model = project.getModel();
    Element properties = rootElement.getChild("properties", namespace);

    String parentVersion = rewriteParent(project, rootElement, namespace, mappedVersions,
                                         resolvedSnapshotDependencies, originalVersions);

    String projectId = ArtifactUtils.versionlessKey(project.getGroupId(), project.getArtifactId());

    rewriteVersion(rootElement, namespace, mappedVersions, projectId, project, parentVersion);

    List<Element> roots = new ArrayList<Element>();
    roots.add(rootElement);
    roots.addAll(getChildren(rootElement, "profiles", "profile"));

    for (Element root : roots) {
      rewriteArtifactVersions(getChildren(root, "dependencies", "dependency"), mappedVersions,
                              resolvedSnapshotDependencies, originalVersions, model, properties, result,
                              releaseDescriptor);

      rewriteArtifactVersions(getChildren(root, "dependencyManagement", "dependencies", "dependency"),
                              mappedVersions, resolvedSnapshotDependencies, originalVersions, model, properties,
                              result, releaseDescriptor);

      rewriteArtifactVersions(getChildren(root, "build", "extensions", "extension"), mappedVersions,
                              resolvedSnapshotDependencies, originalVersions, model, properties, result,
                              releaseDescriptor);

      List<Element> pluginElements = new ArrayList<Element>();
      pluginElements.addAll(getChildren(root, "build", "plugins", "plugin"));
      pluginElements.addAll(getChildren(root, "build", "pluginManagement", "plugins", "plugin"));

      rewriteArtifactVersions(pluginElements, mappedVersions, resolvedSnapshotDependencies, originalVersions,
                              model, properties, result, releaseDescriptor);

      for (Element pluginElement : pluginElements) {
        rewriteArtifactVersions(getChildren(pluginElement, "dependencies", "dependency"), mappedVersions,
                                resolvedSnapshotDependencies, originalVersions, model, properties, result,
                                releaseDescriptor);
      }

      rewriteArtifactVersions(getChildren(root, "reporting", "plugins", "plugin"), mappedVersions,
                              resolvedSnapshotDependencies, originalVersions, model, properties, result,
                              releaseDescriptor);
    }
  }

  @SuppressWarnings("unchecked")
  private List<Element> getChildren(Element root, String... names) {
    Element parent = root;
    for (int i = 0; i < names.length - 1 && parent != null; i++) {
      parent = parent.getChild(names[i], parent.getNamespace());
    }
    if (parent == null) {
      return Collections.emptyList();
    }
    return parent.getChildren(names[names.length - 1], parent.getNamespace());
  }

  /**
   * Updates the text value of the given element. The primary purpose of this method is to preserve any whitespace and comments
   * around the original text value.
   *
   * @param element The element to update, must not be <code>null</code>.
   * @param value The text string to set, must not be <code>null</code>.
   */
  private void rewriteValue(Element element, String value) {
    Text text = null;
    if (element.getContent() != null) {
      for (Iterator<?> it = element.getContent().iterator(); it.hasNext(); ) {
        Object content = it.next();
        if ((content instanceof Text) && ((Text) content).getTextTrim().length() > 0) {
          text = (Text) content;
          while (it.hasNext()) {
            content = it.next();
            if (content instanceof Text) {
              text.append((Text) content);
              it.remove();
            }
            else {
              break;
            }
          }
          break;
        }
      }
    }
    if (text == null) {
      element.addContent(value);
    }
    else {
      String chars = text.getText();
      String trimmed = text.getTextTrim();
      int idx = chars.indexOf(trimmed);
      String leadingWhitespace = chars.substring(0, idx);
      String trailingWhitespace = chars.substring(idx + trimmed.length());
      text.setText(leadingWhitespace + value + trailingWhitespace);
    }
  }

  private void rewriteVersion(Element rootElement, Namespace namespace, Map<String, String> mappedVersions, String projectId,
                              MavenProject project, String parentVersion)
      throws ReleaseFailureException {
    Element versionElement = rootElement.getChild("version", namespace);
    String version = mappedVersions.get(projectId);
    if (version == null) {
      throw new ReleaseFailureException("Version for '" + project.getName() + "' was not mapped");
    }

    if (versionElement == null) {
      if (!version.equals(parentVersion)) {
        // we will add this after artifactId, since it was missing but different from the inherited version
        Element artifactIdElement = rootElement.getChild("artifactId", namespace);
        int index = rootElement.indexOf(artifactIdElement);

        versionElement = new Element("version", namespace);
        versionElement.setText(version);
        rootElement.addContent(index + 1, new Text("\n  "));
        rootElement.addContent(index + 2, versionElement);
      }
    }
    else {
      rewriteValue(versionElement, version);
    }
  }

  private String rewriteParent(MavenProject project, Element rootElement, Namespace namespace, Map<String, String> mappedVersions,
                               Map<String, Map<String, String>> resolvedSnapshotDependencies, Map<String, String> originalVersions)
      throws ReleaseFailureException {
    String parentVersion = null;
    if (project.hasParent()) {
      Element parentElement = rootElement.getChild("parent", namespace);
      Element versionElement = parentElement.getChild("version", namespace);
      MavenProject parent = project.getParent();
      String key = ArtifactUtils.versionlessKey(parent.getGroupId(), parent.getArtifactId());
      parentVersion = mappedVersions.get(key);
      if (parentVersion == null) {
        //MRELEASE-317
        parentVersion = getResolvedSnapshotVersion(key, resolvedSnapshotDependencies);
      }
      if (parentVersion == null) {
        if (parent.getVersion().equals(originalVersions.get(key))) {
          throw new ReleaseFailureException("Version for parent '" + parent.getName() + "' was not mapped");
        }
      }
      else {
        rewriteValue(versionElement, parentVersion);
      }
    }
    return parentVersion;
  }

  private void rewriteArtifactVersions(Collection<Element> elements, Map<String, String> mappedVersions,
                                       Map<String, Map<String, String>> resolvedSnapshotDependencies, Map<String, String> originalVersions,
                                       Model projectModel, Element properties, ReleaseResult result,
                                       ReleaseDescriptor releaseDescriptor)
      throws ReleaseExecutionException, ReleaseFailureException {
    if (elements == null) {
      return;
    }
    String projectId = ArtifactUtils.versionlessKey(projectModel.getGroupId(), projectModel.getArtifactId());
    for (Element element : elements) {
      Element versionElement = element.getChild("version", element.getNamespace());
      if (versionElement == null) {
        // managed dependency or unversioned plugin
        continue;
      }
      String rawVersion = versionElement.getTextTrim();

      Element groupIdElement = element.getChild("groupId", element.getNamespace());
      if (groupIdElement == null) {
        if ("plugin".equals(element.getName())) {
          groupIdElement = new Element("groupId", element.getNamespace());
          groupIdElement.setText("org.apache.maven.plugins");
        }
        else {
          // incomplete dependency
          continue;
        }
      }
      String groupId = interpolate(groupIdElement.getTextTrim(), projectModel);

      Element artifactIdElement = element.getChild("artifactId", element.getNamespace());
      if (artifactIdElement == null) {
        // incomplete element
        continue;
      }
      String artifactId = interpolate(artifactIdElement.getTextTrim(), projectModel);

      String key = ArtifactUtils.versionlessKey(groupId, artifactId);
      String resolvedSnapshotVersion = getResolvedSnapshotVersion(key, resolvedSnapshotDependencies);
      String mappedVersion = mappedVersions.get(key);
      String originalVersion = originalVersions.get(key);
      if (originalVersion == null) {
        originalVersion = getOriginalResolvedSnapshotVersion(key, resolvedSnapshotDependencies);
      }

      // MRELEASE-220
      if (mappedVersion != null && mappedVersion.endsWith(Artifact.SNAPSHOT_VERSION)
          && !rawVersion.endsWith(Artifact.SNAPSHOT_VERSION) && !releaseDescriptor.isUpdateDependencies()) {
        continue;
      }

      if (mappedVersion != null) {
        if (rawVersion.equals(originalVersion)) {
          logInfo(result, "  Updating " + artifactId + " to " + mappedVersion);
          rewriteValue(versionElement, mappedVersion);
        }
        else if (rawVersion.matches("\\$\\{.+\\}")) {
          String expression = rawVersion.substring(2, rawVersion.length() - 1);

          if (expression.startsWith("project.") || expression.startsWith("pom.")
              || "version".equals(expression)) {
            if (!mappedVersion.equals(mappedVersions.get(projectId))) {
              logInfo(result, "  Updating " + artifactId + " to " + mappedVersion);
              rewriteValue(versionElement, mappedVersion);
            }
            else {
              logInfo(result, "  Ignoring artifact version update for expression " + rawVersion);
            }
          }
          else if (properties != null) {
            // version is an expression, check for properties to update instead
            Element property = properties.getChild(expression, properties.getNamespace());
            if (property != null) {
              String propertyValue = property.getTextTrim();

              if (propertyValue.equals(originalVersion)) {
                logInfo(result, "  Updating " + rawVersion + " to " + mappedVersion);
                // change the property only if the property is the same as what's in the reactor
                rewriteValue(property, mappedVersion);
              }
              else if (mappedVersion.equals(propertyValue)) {
                // this property may have been updated during processing a sibling.
                logInfo(result, "  Ignoring artifact version update for expression " + rawVersion
                                + " because it is already updated");
              }
              else if (!mappedVersion.equals(rawVersion)) {
                if (mappedVersion.matches("\\$\\{project.+\\}")
                    || mappedVersion.matches("\\$\\{pom.+\\}") || "${version}".equals(mappedVersion)) {
                  logInfo(result, "  Ignoring artifact version update for expression "
                                  + mappedVersion);
                  // ignore... we cannot update this expression
                }
                else {
                  // the value of the expression conflicts with what the user wanted to release
                  throw new ReleaseFailureException("The artifact (" + key + ") requires a "
                                                    + "different version (" + mappedVersion + ") than what is found ("
                                                    + propertyValue + ") for the expression (" + expression + ") in the "
                                                    + "project (" + projectId + ").");
                }
              }
            }
            else {
              // the expression used to define the version of this artifact may be inherited
              // TODO needs a better error message, what pom? what dependency?
              throw new ReleaseFailureException("The version could not be updated: " + rawVersion);
            }
          }
        }
        else {
          // different/previous version not related to current release
        }
      }
      else if (resolvedSnapshotVersion != null) {
        logInfo(result, "  Updating " + artifactId + " to " + resolvedSnapshotVersion);

        rewriteValue(versionElement, resolvedSnapshotVersion);
      }
      else {
        // artifact not related to current release
      }
    }
  }

  private void logInfo(ReleaseResult result, String s) {
  }

  private String interpolate(String value, Model model)
      throws ReleaseExecutionException {
    if (value != null && value.contains("${")) {
      StringSearchInterpolator interpolator = new StringSearchInterpolator();
      List<String> pomPrefixes = Arrays.asList("pom.", "project.");
      interpolator.addValueSource(new PrefixedObjectValueSource(pomPrefixes, model, false));
      interpolator.addValueSource(new MapBasedValueSource(model.getProperties()));
      interpolator.addValueSource(new ObjectBasedValueSource(model));
      try {
        value = interpolator.interpolate(value, new PrefixAwareRecursionInterceptor(pomPrefixes));
      }
      catch (InterpolationException e) {
        throw new ReleaseExecutionException(
            "Failed to interpolate " + value + " for project " + model.getId(),
            e);
      }
    }
    return value;
  }

  private void writePom(File pomFile, Document document, ReleaseDescriptor releaseDescriptor, String modelVersion,
                        String intro, String outtro)
      throws ReleaseExecutionException {
    Element rootElement = document.getRootElement();

    if (releaseDescriptor.isAddSchema()) {
      Namespace pomNamespace = Namespace.getNamespace("", "http://maven.apache.org/POM/" + modelVersion);
      rootElement.setNamespace(pomNamespace);
      Namespace xsiNamespace = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
      rootElement.addNamespaceDeclaration(xsiNamespace);

      if (rootElement.getAttribute("schemaLocation", xsiNamespace) == null) {
        rootElement.setAttribute("schemaLocation", "http://maven.apache.org/POM/" + modelVersion
                                                   + " http://maven.apache.org/maven-v" + modelVersion.replace('.', '_') + ".xsd", xsiNamespace);
      }

      // the empty namespace is considered equal to the POM namespace, so match them up to avoid extra xmlns=""
      ElementFilter elementFilter = new ElementFilter(Namespace.getNamespace(""));
      for (Iterator<?> i = rootElement.getDescendants(elementFilter); i.hasNext(); ) {
        Element e = (Element) i.next();
        e.setNamespace(pomNamespace);
      }
    }

    Writer writer = null;
    try {
      writer = WriterFactory.newXmlWriter(pomFile);

      if (intro != null) {
        writer.write(intro);
      }

      Format format = Format.getRawFormat();
      format.setLineSeparator(ls);
      XMLOutputter out = new XMLOutputter(format);
      out.output(document.getRootElement(), writer);

      if (outtro != null) {
        writer.write(outtro);
      }
    }
    catch (IOException e) {
      throw new ReleaseExecutionException("Error writing POM: " + e.getMessage(), e);
    }
    finally {
      IOUtil.close(writer);
    }
  }

  protected String getResolvedSnapshotVersion(String artifactVersionlessKey,
                                              Map<String, Map<String, String>> resolvedSnapshotsMap) {
    Map<String, String> versionsMap = resolvedSnapshotsMap.get(artifactVersionlessKey);

    if (versionsMap != null) {
      return versionsMap.get(ReleaseDescriptor.RELEASE_KEY);
    }
    else {
      return null;
    }
  }

  protected Map<String, String> getOriginalVersionMap(ReleaseDescriptor releaseDescriptor,
                                                      List<MavenProject> reactorProjects) {
    return releaseDescriptor.getOriginalVersions(reactorProjects);
  }

  @SuppressWarnings("unchecked")
  protected Map<String, String> getNextVersionMap(ReleaseDescriptor releaseDescriptor) {
    return releaseDescriptor.getReleaseVersions();
  }

  protected String getOriginalResolvedSnapshotVersion(String artifactVersionlessKey, Map<String, Map<String, String>> resolvedSnapshots) {
    Map<String, String> versionsMap = resolvedSnapshots.get(artifactVersionlessKey);

    if (versionsMap != null) {
      return versionsMap.get(ReleaseDescriptor.ORIGINAL_VERSION);
    }
    else {
      return null;
    }
  }
}
