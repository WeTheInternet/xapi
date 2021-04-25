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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import xapi.log.api.ArgHandlerLogLevel;
import xapi.args.ArgHandlerString;
import xapi.args.ArgProcessorBase;
import xapi.log.api.OptionLogLevel;
import xapi.dev.source.SourceBuilder;
import xapi.log.api.LogLevel;
import xapi.log.api.LogService;

public class TemplateGeneratorOptions extends ArgProcessorBase implements OptionLogLevel{

	private final List<String> templates = new ArrayList<String>();
	private final List<String> extraData = new ArrayList<String>();
	private final PayloadTypeArg payloadArg;
	private final Map<String, SourceBuilder<?>> context;
	private LogLevel logLevel;
	private String outputLocation = "src/main/java";

	public TemplateGeneratorOptions() {
	  logLevel = LogLevel.INFO;
		payloadArg = new PayloadTypeArg();
		context = new HashMap<String, SourceBuilder<?>>();
		registerHandler(new TemplateLocationArg(templates));
		registerHandler(payloadArg);
		registerHandler(new ExtraOptionsArg(extraData));
		registerHandler(new TemplateOutputLocationArg(this));
		registerHandler(new ArgHandlerLogLevel(this));
	}

	public static class TemplateOutputLocationArg extends ArgHandlerString{
		private final TemplateGeneratorOptions opts;

		public TemplateOutputLocationArg(TemplateGeneratorOptions opts) {
			this.opts = opts;
		}

		@Override
		public boolean setString(String str) {
			opts.outputLocation = str.endsWith(File.separator)
			  ? str : str + File.separator;
			return true;
		}

		@Override
		public String getPurpose() {
			return "Location to output generated files.  Default location is ${launch_directory}/src/main/java.";
		}

		@Override
		public String getTag() {
			return "-output";
		}

		@Override
		public String[] getTagArgs() {
			return new String[]{"${workspace_loc:some-project}/src/main/java"};
		}
	}

	public static class TemplateLocationArg extends ArgHandlerString{
		private final List<String> templates;

		public TemplateLocationArg(List<String> templates) {
			this.templates = templates;
		}

		@Override
		public boolean setString(String str) {
		    StringTokenizer st = new StringTokenizer(str, ",");
		    while (st.hasMoreTokens()) {
		      templates.add(st.nextToken().trim());
		    }
		    return true;
		}

		@Override
		public String getPurpose() {
			return "Locations of template to generate from classpath resources, using / as a delimiter.";
		}

		@Override
		public String getTag() {
			return "-template";
		}

		@Override
		public String[] getTagArgs() {
			return new String[]{"package/location/TemplateFile.x"};
		}

		@Override
		public boolean isRequired() {
			return true;
		}
	}

	public static class ExtraOptionsArg extends ArgHandlerString{
		private final List<String> extras;

		public ExtraOptionsArg(List<String> extras) {
			this.extras = extras;
		}

		@Override
		public boolean setString(String str) {
		    StringTokenizer st = new StringTokenizer(str, ",");
		    while (st.hasMoreTokens()) {
		      extras.add(st.nextToken().trim());
		    }
		    return true;
		}

		@Override
		public String getPurpose() {
			return "Extra data you wish to make available to your context's payload object.";
		}

		@Override
		public String getTag() {
			return "-extra";
		}

		@Override
		public String[] getTagArgs() {
			return new String[]{"\"String Data Of Your Choosing\""};
		}
	}

	public static class PayloadTypeArg extends ArgHandlerString{
		private String payloadClass = "java.lang.Object";
		private Object payload;

		@Override
		public boolean setString(String str) {
			payloadClass = str;
			return true;
		}

		@Override
		public String getPurpose() {
			return "Fully qualified classname of payload used in the SourceBuilder object.\n\t  This class must be " +
					"static with a zero-arg public constructor.\n\t  For complex projects using multiple generators, " +
					"you are recommended to use interfaces that will be injected via X_Inject.singleton()";
		}

		@Override
		public String getTag() {
			return "-payload";
		}

		@Override
		public String[] getTagArgs() {
			return new String[]{"com.template.payload.ClassName"};
		}

		public Object getPayload(LogService logger, TemplateGeneratorOptions options) {
			if (payload == null){
				try{
					payload = Class.forName(payloadClass).newInstance();
				}catch(Exception e){
					throw new CompilationFailed("Unable to instantiate payload type.", e);
				}
				if (payload instanceof TemplateClassGenerator)
					((TemplateClassGenerator) payload).initialize(logger, options);
			}
			return payload;
		}
	}

	@Override
	protected String getName() {
		return "TemplateToJava";
	}

	public List<String> getTemplates() {
		return templates;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public SourceBuilder<?> getContext(LogService logger, String forTemplate){
    SourceBuilder ctx = context.get(forTemplate);
		if (ctx == null){
			ctx = new SourceBuilder();
			Object payload = payloadArg.getPayload(logger, this);
			ctx.setPayload(payload);
		}
		return ctx;
	}

	public List<String> getExtraData() {
		return extraData;
	}

	public String getOutputLocation() {
		return outputLocation;
	}

  @Override
  public LogLevel getLogLevel() {
    return logLevel;
  }

  @Override
  public void setLogLevel(LogLevel logLevel) {
    this.logLevel = logLevel;
  }

}
