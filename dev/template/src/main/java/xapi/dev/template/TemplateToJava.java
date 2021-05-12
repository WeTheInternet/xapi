/*
 * Copyright 2013, We The Internet Ltd.
 *
 * All rights reserved.
 *
 * Distributed under a modified BSD License as follow:
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistribution in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution, unless otherwise
 * agreed to in a written document signed by a director of We The Internet Ltd.
 *
 * Neither the name of We The Internet nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package xapi.dev.template;

import xapi.collect.fifo.SimpleFifo;
import xapi.dev.source.ImportSection;
import xapi.dev.source.SourceBuilder;
import xapi.log.api.LogLevel;
import xapi.log.api.LogService;
import xapi.log.impl.JreLog;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class TemplateToJava {

  private static final String templateSuffix = System.getProperty("template.suffix", ".x");
  private static final Pattern lineMatcher = Pattern.compile("\\s*//.*//\\s*");
  private static final Charset utf8 = Charset.forName("UTF-8");

  public static void main(String[] templates) {

    TemplateToJava generator = new TemplateToJava();
    
    LogService logger = new JreLog();
    TemplateGeneratorOptions options = new TemplateGeneratorOptions();

    if (options.processArgs(templates)) {
      logger.setLogLevel(options.getLogLevel());
      for (String template : options.getTemplates()) {
        generator.generate(logger, template, options);
      }
    } else {
      throw new RuntimeException("Invalid arguments specified;" + " see console logs for help.");
    }
  }

  private Class<?> generatorClass;
  private final Map<Class<?>,Object> generators = new HashMap<Class<?>,Object>();

  public void generate(LogService logger, String template, TemplateGeneratorOptions options) {
    SourceBuilder<?> context = options.getContext(logger, template);
    InputStream input = null;
    try {
      if (new File(template).exists()) {
        input = new FileInputStream(template);
      } else {
        URL url = getClass().getClassLoader().getResource(template);
        if (url == null) {
          url = ClassLoader.getSystemResource(template);
          if (url == null) {
            logger.log(LogLevel.ERROR, "You requested code generation for template " + template +
              ", but the file could not be found.");
            throw new CompilationFailed();
          }
        }
        input = url.openStream();
      }
      BufferedReader reader = new BufferedReader(new InputStreamReader(input));
      String line;
      while ((line = reader.readLine()) != null) {
        if (lineMatcher.matcher(line).matches()) {
          // we have a template command to parse!
          applyTemplate(logger, reader, context, options, line.trim());
        } else {
          context.getBuffer().append(line).append('\n');
        }
      }
      exportClass(logger, template, context, options);
    } catch (Exception e) {
      throw new CompilationFailed("Unable to generate java source file for template " + template, e);
    } finally {
      if (input != null) {
        try {
          input.close();
        } catch (IOException e) {}
      }
    }
  }

  private static final int BUF_SIZE = 16 * 1024;

  
  private void exportClass(LogService logger, String filename, SourceBuilder<?> context,
    TemplateGeneratorOptions opts) {
    File outputFile = new File(opts.getOutputLocation());
    // normalize filename
    if (filename.endsWith(templateSuffix))
      filename = filename.substring(0, filename.length() - templateSuffix.length());
    if (!filename.endsWith(".java")) filename = filename + ".java";

    // repackage if requested; useful for generating non-transient super-source
    String packageName = context.getPackage();
    if (packageName != null) {
      filename = packageName.replace('.', File.separatorChar) + File.separator +
        filename.substring(filename.lastIndexOf('/'));
    }

    // save the source to file
    outputFile = new File(outputFile.getAbsoluteFile(), filename);
    outputFile.getParentFile().mkdirs();
    try {
      logger.log(LogLevel.INFO, "Writing generated output to " + outputFile.getAbsolutePath());
      InputStream in = new ByteArrayInputStream(context.toString().getBytes(utf8));
      OutputStream out = new FileOutputStream(outputFile);
      
      byte[] buf = new byte[BUF_SIZE];
      try {
        int i;
        while ((i = in.read(buf)) != -1) {
          out.write(buf, 0, i);
        }
      } finally {
        in.close();
        out.close();
      }
      
    } catch (IOException e) {
      logger.doLog(LogLevel.ERROR, new SimpleFifo<>()
          .give("Error streaming generated output to file")
          .give(outputFile.getAbsolutePath())
          .give(e));
    }
  }

  private void applyTemplate(LogService logger, BufferedReader input, SourceBuilder<?> context,
    TemplateGeneratorOptions options, String line) throws IOException {
    if (lineMatcher.matcher(line).matches())
      line = line.substring(2, line.length() - 2);
    else {
      context.getBuffer().println(line);
      return;
    }
    if (line.startsWith("@")) {
      // we have a state-modifying command
      String[] parts = stripBrackets(line.substring(1), input);
      String command = parts[1];
      switch (TemplateProcessingInstruction.valueOf(parts[0])) {
      case classDefinition:
        String next = input.readLine();
        if (next.contains("class ") || next.contains("interface ")) {
          // We have an @classDefinition above an actual well-formatted class {
          // so we tell the class-def to not supply a }, as the file is
          // assumed to be well-formatted as well.
          context.setClassDefinition(next, true);
          context.setClassDefinition(command, true);
        } else {
          context.setClassDefinition(command, false);
          applyTemplate(logger, input, context, options, next);
          return;
        }
        break;
      case generateWith:
        logger.log(LogLevel.TRACE, "Setting generator to " + command);
        try {
          generatorClass = Class.forName(command);
          // Using X_Inject allows payloads to be specified as interfaces.
          // This will allow generators and payloads to be reused and combined
          // by creating a master payload which overrides all injected payload
          // types needed.

          // Mixing generators with different payloads cannot succeed without a
          // master payload instance.
          Object generator = inject(generatorClass);
          if (generator instanceof TemplateClassGenerator)
            ((TemplateClassGenerator)generator).initialize(logger, options);
          generators.put(generatorClass, generator);
        } catch (Exception e) {
          throw new CompilationFailed("Could not instantiate requested generator " + command + "; " +
            "please ensure this class is available on your code generation classpath.", e);
        }
        break;
      case emulated:
        // when emulated, we need to pull in the next line, as we're rewriting a
        // package statement.
        String packageStatement = input.readLine().trim();
        if (!packageStatement.startsWith("package ")) {
          throw new CompilationFailed("An //@emulated()// command must appear on the line directly "
            + "above the package statement.");
        }
        String repackage = packageStatement.substring(8);
        logger.log(LogLevel.TRACE, "Repackaging emulated source into " + command + "." + repackage);
        context.getBuffer().append("package " + repackage);
        context.setPackage(command + "." + repackage);
        break;
      case repackaged:
        packageStatement = input.readLine().trim();
        if (!packageStatement.startsWith("package ")) {
          throw new CompilationFailed("An //@repackage()// command must appear on the line directly "
            + "above the package statement.");
        }
        repackage = command + ";";
        logger.log(LogLevel.TRACE, "Repackaging source into " + repackage);
        context.getBuffer().append("package " + repackage);
        context.setPackage(repackage);
        break;
      case imports:
        // declares an ImportSection that we can write to lazily
        ImportSection imports = context.getImports();
        String existing;
        while ((existing = input.readLine()) != null) {
          if (existing.trim().length() == 0) {
            continue;
          }
          if (existing.startsWith("import ")) {
            imports.addImport(existing);
          } else {
            applyTemplate(logger, input, context, options, existing);
            return;
          }
        }
        break;
      case skipline:
        int lines = Integer.parseInt(command);
        while (lines-- > 0) {
          input.readLine();
        }
        break;
      }
    } else {
      String[] parts = stripBrackets(line, input);
      if (generatorClass == null)
        throw new CompilationFailed("TemplateToJava encountered a call to a template method, " + line +
          ", before a generator was called.  " +
          "\nPlease add //@generateWith(package.Class)// to the top of your template");
      Object generator = generators.get(generatorClass);
      if (generator == null)
        throw new CompilationFailed("TemplateToJava encountered a call to a template method, " + line +
          ", before a generator was loaded.  " +
          "\nPlease add //@generateWith(package.Class)// to the top of your template");

      Method method;
      try {
        method = generatorClass.getDeclaredMethod(parts[0], LogService.class, SourceBuilder.class,
          String.class);
        method.setAccessible(true);
      } catch (Exception e) {
        throw new CompilationFailed("Unable to find instance method " + parts[0] + " in generator class " +
          generatorClass.getName(), e);
      }
      try {
        method.invoke(generator, logger, context, parts[1]);
        int skipLines = context.getLinesToSkip();
        while (skipLines-- > 0)
          input.readLine();
      } catch (Exception e) {
        throw new CompilationFailed("Failure invoking instance method " + parts[0] + " in generator class " +
          generatorClass.getName(), e);
      }
    }
  }

  /**
   * This method blindly instantiates the zero-arg constructor of the class. It
   * it protected for subclasses to implement custom instantiation strategies
   *
   * @param cls - The class to inject
   * @return cls.newInstance();
   */
  protected Object inject(Class<?> cls) {
    try {
      return cls.newInstance();
    } catch (Exception e) {
      throw new CompilationFailed("Unable to instantiate class " + cls, e);
    }
  }

  private String[] stripBrackets(String line, BufferedReader input) {
    int start = line.indexOf('(');
    int end = line.lastIndexOf(')');
    if (start < 0 || end < 0)
      throw new CompilationFailed("The command " + line + " was malformed.  Expected () brackets in " + line);
    String body = line.substring(0, start).trim();
    String params = line.substring(start + 1, end).trim();
    if (end < line.length() - 1) {
      try {
        String extra = line.substring(end + 1).trim();
        if (extra.length() > 0) {
          ProcessingInstructionOptions opts = new ProcessingInstructionOptions();
          if (opts.processArgs(extra.split(" "))) {
            if (opts.wordsToSkip > 0) {
              // eat opening space
              while ((input.read()) == ' ')
                ;
            }
            while (opts.wordsToSkip-- > 0) {
              while ((input.read()) != ' ')
                ;
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return new String[] {body, params};
  }

}
