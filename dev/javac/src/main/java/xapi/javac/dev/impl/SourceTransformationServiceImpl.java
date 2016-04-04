package xapi.javac.dev.impl;

import com.sun.source.tree.CompilationUnitTree;
import xapi.annotation.inject.InstanceDefault;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.fu.Rethrowable;
import xapi.inject.X_Inject;
import xapi.io.X_IO;
import xapi.javac.dev.api.CompilerService;
import xapi.javac.dev.api.InjectionResolver;
import xapi.javac.dev.api.JavacService;
import xapi.javac.dev.api.SourceTransformationService;
import xapi.javac.dev.model.SourceRange;
import xapi.javac.dev.model.SourceTransformation;
import xapi.javac.dev.model.SourceTransformation.SourceTransformType;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/3/16.
 */
@InstanceDefault(implFor = SourceTransformationService.class)
public class SourceTransformationServiceImpl implements SourceTransformationService, Rethrowable {

  private JavacService service;

  private CompilerService compiler;

  private StringTo.Many<SourceTransformation> transforms = X_Collect.newStringMultiMap(SourceTransformation.class);

  @Override
  public void init(JavacService service) {
    this.service = service;
    compiler = CompilerService.compileServiceFrom(service);
  }

  @Override
  public InjectionResolver createInjectionResolver(CompilationUnitTree cup) {
    InjectionResolver resolver = X_Inject.instance(InjectionResolver.class);
    resolver.init(service, cup, this);
    return resolver;
  }

  @Override
  public void requestOverwrite(CompilationUnitTree cup, int startPos, int endPos, String newSource) {
    final String name = service.getQualifiedName(cup, service.getClassTree(cup));
    final IntTo<SourceTransformation> jobs = getJobs(cup, name);
    jobs.add(new SourceTransformation()
        .setCompilationUnit(cup)
        .setTransformType(SourceTransformType.REPLACE)
        .setRange(new SourceRange(startPos, endPos))
        // TODO add expected text
        .setText(newSource)
    );
  }

  private IntTo<SourceTransformation> getJobs(CompilationUnitTree cup, String name) {
    final IntTo<SourceTransformation> jobs = transforms.get(name);
    if (jobs.isEmpty()) {
      compiler.onCompilationUnitFinished(name, c->{
        final SourceTransformation[] items = jobs.toArray();
        if (items.length > 0) {
          jobs.clear();
          String newSource = applyTransformations(name, cup, items);
          compiler.overwriteCompilationUnit(cup, newSource);
        }
      });
    };
    return jobs;
  }

  private String applyTransformations(String name, CompilationUnitTree cup, SourceTransformation ... jobs) {
    final JavaFileObject sourceFile = cup.getSourceFile();
    String source;
    try {
      try (
          InputStream in = sourceFile.openInputStream()
      ) {
        source = X_IO.toStringUtf8(in);
      }
    } catch (IOException e) {
      throw rethrow(e);
    }
    if (jobs.length > 0) {
      Arrays.sort(jobs, (a, b)->a.getRange().compareTo(b.getRange()));
      // Grab the expected source so we don't accidentally
      for (SourceTransformation job : jobs) {
        job.setExpected(job.getRange().slice(source));
      }
      // To avoid corrupting indexes, we will apply ranges backwards.
      for (int i = jobs.length; i-->0; ) {
        final SourceTransformation job = jobs[i];
        final Optional<String> transformed = applyTransform(source, job);
        if (transformed.isPresent()) {
          source = transformed.get();
        } else {
          // If the transform was not applied, we will put the job back on the queue.
          getJobs(cup, name).add(job);
        }
      }
    }
    return source;
  }

  private Optional<String> applyTransform(String source, SourceTransformation job) {
    final SourceRange range = job.getRange();
    String current = range.slice(source);
    if (!current.equals(job.getExpected())) {
      return Optional.empty();
    }
    StringBuilder b = new StringBuilder();
    switch (job.getTransformType()) {
      case REMOVE:
        if (range.getStart() > 0) {
          b.append(source.substring(0, range.getStart()));
        }
        if (range.getEnd() < source.length()) {
          b.append(source.substring(range.getEnd()));
        }
        break;
      case REPLACE:
        if (range.getStart() > 0) {
          b.append(source.substring(0, range.getStart()));
        }
        b.append(job.getText());
        if (range.getEnd() < source.length()) {
          b.append(source.substring(range.getEnd()));
        }
        break;
      case WRAP:
        if (range.getStart() > 0) {
          b.append(source.substring(0, range.getStart()));
        }
        if (job.getText() != null) {
          b.append(job.getText());
        }
        b.append(current);
        if (job.getExtraText() != null) {
          b.append(job.getExtraText());
        }

        if (range.getEnd() < source.length()) {
          b.append(source.substring(range.getEnd()));
        }
        break;
    }
    return Optional.of(b.toString());
  }
}
