package xapi.mvn.impl;

import com.google.common.collect.ImmutableList;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import xapi.log.X_Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 24/11/15.
 */
public class ProjectIterable implements Iterable<MavenProject> {
  private final MavenProject root;
  private final List<MavenProject> projects;

  public ProjectIterable(
      MavenProject project,
      ProjectBuilder builder,
      ProjectBuildingRequest request,
      boolean includeSelf
  ) {
    root = project;
    final ArrayList<MavenProject> transientList = new ArrayList<>();
    if (includeSelf) {
      transientList.add(project);
    }
    addAllProjects(transientList, project, builder, request);
    projects = ImmutableList.copyOf(transientList);
  }

  private void addAllProjects(
      final List<MavenProject> projects,
      final MavenProject project,
      final ProjectBuilder builder,
      final ProjectBuildingRequest request
  ) {

    final List<String> modules = root.getModules();
    File file = project.getFile().getParentFile();
    for (String module : modules) {
      final File moduleFile = new File(
          new File(file, module),
          "pom.xml"
      );
      try {
        final ProjectBuildingResult result = builder.build(
            moduleFile,
            request
        );
        if (!result.getProblems().isEmpty()) {
          X_Log.warn(getClass(), "Problems encountered looking up module ", moduleFile, result.getProblems());
        }
        projects.add(result.getProject());
        if (!result.getProject().getModules().isEmpty()) {
          addAllProjects(projects, result.getProject(), builder, request);
        }
      } catch (ProjectBuildingException e) {
        X_Log.warn(getClass(), "Unable to build module for ", moduleFile);
      }
    }
  }

  @Override
  public Iterator<MavenProject> iterator() {
    return projects.iterator(); // This list is immutable, so Iterator.remove() will not work.
  }
}
