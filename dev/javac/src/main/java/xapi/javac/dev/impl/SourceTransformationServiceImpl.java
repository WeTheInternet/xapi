package xapi.javac.dev.impl;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.sun.source.tree.CompilationUnitTree;
import xapi.annotation.inject.InstanceDefault;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.fu.Rethrowable;
import xapi.inject.X_Inject;
import xapi.javac.dev.api.CompilerService;
import xapi.javac.dev.api.InjectionResolver;
import xapi.javac.dev.api.JavacService;
import xapi.javac.dev.api.SourceTransformationService;
import xapi.javac.dev.model.JavaDocument;
import xapi.javac.dev.model.SourceRange;
import xapi.javac.dev.model.SourceTransformation;
import xapi.javac.dev.model.SourceTransformation.SourceTransformType;

import javax.tools.JavaFileObject;
import java.util.Arrays;
import java.util.Comparator;
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
    final JavaDocument doc = compiler.getDocument(cup);
    final String name = doc.getTypeName();
    final IntTo<SourceTransformation> jobs = getJobs(doc, name);
    jobs.add(new SourceTransformation()
        .setCompilationUnit(cup)
        .setTransformType(SourceTransformType.REPLACE)
        .setRange(new SourceRange(startPos, endPos))
        // TODO add expected text
        .setText(newSource)
    );
  }

  private IntTo<SourceTransformation> getJobs(JavaDocument doc, String name) {
    return getJobs(doc, name, false);
  }

  private IntTo<SourceTransformation> getJobs(JavaDocument doc, String name, boolean forceAdd) {
    final IntTo<SourceTransformation> jobs = transforms.get(name);
    if (forceAdd || jobs.isEmpty()) {
      compiler.onCompilationUnitFinished(name, c->{
        final SourceTransformation[] items = jobs.toArray();
        if (items.length == 0) {
          // no transforms?  Lets finalize the document if it is in the working directory.
          finalizeDocument(doc);
        } else {
          jobs.clear();
          String newSource = applyTransformations(name, doc, items);
          compiler.overwriteCompilationUnit(doc, newSource);
        }
      });
    }
    return jobs;
  }

  private void finalizeDocument(JavaDocument doc) {
    if (doc.getPackageName().startsWith(compiler.workingPackage())) {
      String newPackage = doc.getPackageName().replace(compiler.workingPackage() + "." , compiler.outputPackage() + ".");
      if (!newPackage.equals(doc.getPackageName())) {
        String newSource = applyTransformations(doc.getTypeName(), doc, repackage(doc, doc.getPackageName(),
            newPackage));
        compiler.overwriteCompilationUnit(doc, newSource);
      }
    }
  }

  private String applyTransformations(String name, JavaDocument doc, SourceTransformation ... jobs) {
    final JavaFileObject sourceFile = doc.getSourceFile();
    String source = doc.getSource();
    if (jobs.length > 0) {
      Arrays.sort(jobs, Comparator.comparing(SourceTransformation::getRange));
      // Grab the expected source so we don't accidentally overwrite an index that has moved.
      for (SourceTransformation job : jobs) {
        job.setExpected(job.getRange().slice(source));
      }
      // To avoid corrupting indexes, we will apply ranges backwards.
      for (int i = jobs.length; i-->0; ) {
        final SourceTransformation job = jobs[i];
        final Optional<String> transformed = applyTransform(doc, job);
        if (transformed.isPresent()) {
          source = transformed.get();
        } else {
          // If the transform was not applied, we will put the job back on the queue.
          // Also, let the job know that it failed so that it can re-perform its modification on the expected source.
          // We let it see the sources we have when it failed so that it can, if it is able, just refind a particular range.
          // even if that position is moved again, this job will eventually stabilize, if it is able to recover.
          job.setFailedTransform(source, name, sourceFile);
          getJobs(doc, name).add(job);
        }
      }
    }
    return source;
  }

  private Optional<String> applyTransform(JavaDocument doc, SourceTransformation job) {
    final SourceRange range = job.getRange();
    final String source = doc.getSource();
    String current = range.slice(source);
    if (!current.equals(job.getExpected())) {
      return Optional.empty();
    }
    StringBuilder b = new StringBuilder();
    switch (job.getTransformType()) {
      case REMOVE:
        if (range.getStart() > 0) {
          b.append(source, 0, range.getStart());
        }
        if (range.getEnd() < source.length()) {
          b.append(source.substring(range.getEnd()));
        }
        break;
      case REPLACE:
        if (range.getStart() > 0) {
          b.append(source, 0, range.getStart());
        }
        b.append(job.getText());
        if (range.getEnd() < source.length()) {
          b.append(source.substring(range.getEnd()));
        }
        break;
      case REPACKAGE:
        String newPackage = job.getText();
        String oldPackage = job.getExtraText();
        if ("".equals(newPackage) || doc.getAst().getPackage() == null) {
          doc.getAst().setPackage(new PackageDeclaration(new NameExpr(newPackage)));
        } else {
          doc.getAst().getPackage().setName(new NameExpr(oldPackage));
        }
        // fallthrough; we also want to do a change import during a repackage.
      case CHANGE_IMPORT:

        String newPkg = job.getText();
        String oldPkg = job.getExtraText();
        final CompilationUnit ast = doc.getAst();
        ast.getImports()
            .forEach(importDecl -> {
              String name = importDecl.getName().getName();
              String repackaged = name.replace(oldPkg, newPkg);

              if ((!importDecl.isAsterisk() && name.equals(oldPkg)) ||
                  name.startsWith(doc.getTypeName())) {
                if (ast.getImports().stream().noneMatch(
                    imported -> imported.getName().getName().equals(repackaged))) {
                  importDecl.getName().setName(repackaged);
                }
              } else if (importDecl.isAsterisk()) {
                if (name.equals(oldPkg)) {
                  assert !importDecl.isStatic();
                  // If there was an asterisk import on the old package, add a new import for the repackaged type,
                  // but check first that this import does not exist.
                  if (ast.getImports().stream().noneMatch(
                      imported -> imported.isAsterisk() && imported.getName().getName().equals(newPkg))) {
                    ast.getImports().add(new ImportDeclaration(new NameExpr(newPkg), false, true));
                  }
                }
              }
            });
        return Optional.of(doc.getAst().toSource(service.getTransformer()));
      case WRAP:
        if (range.getStart() > 0) {
          b.append(source, 0, range.getStart());
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

  @Override
  public void recordRepackage(JavaDocument doc, String oldPackage, String newPackage) {
    getJobs(doc, doc.getTypeName(), true)
      .add(repackage(doc, oldPackage, newPackage)
      );
    // TODO make sure compiler service cleans these listeners up (or throw away compiler service on each compile)...
    // or maintain a set of remappings we've done to be able to reuse.
    compiler.peekOnCompiledUnits(otherDoc->{
      if (otherDoc.hasImport(oldPackage)) {
        getJobs(otherDoc, otherDoc.getTypeName(), true)
            .add(changeImport(doc, oldPackage, newPackage));
      }
    });
  }

  private SourceTransformation repackage(JavaDocument doc, String oldPackage, String newPackage) {
    return new SourceTransformation()
        .setCompilationUnit(doc.getCompilationUnit())
        .setText(newPackage)
        .setExtraText(oldPackage)
        .setTransformType(SourceTransformType.REPACKAGE);
  }

  private SourceTransformation changeImport(JavaDocument doc, String oldPackage, String newPackage) {
    return new SourceTransformation()
        .setCompilationUnit(doc.getCompilationUnit())
        .setText(newPackage)
        .setExtraText(oldPackage)
        .setTransformType(SourceTransformType.CHANGE_IMPORT);
  }
}
