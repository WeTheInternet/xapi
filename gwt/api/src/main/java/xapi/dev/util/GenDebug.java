/*
 * Copyright 2012, We The Internet Ltd.
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
 * Redistributions in binary form must reproduce the above copyright notice,
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
package xapi.dev.util;

import java.lang.reflect.Field;

import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ConfigurationProperty;
import com.google.gwt.dev.cfg.DynamicPropertyOracle;
import com.google.gwt.dev.cfg.Properties;
import com.google.gwt.dev.shell.ModuleSpacePropertyOracle;

/**
 * A place to put general-purpose debugging tools for use during code
 * generation.
 *
 *
 * @author James X. Nelson (james@wetheinter.net)
 *
 */
public class GenDebug {

  /**
   * In case you're too lazy to step through the debugger to find all set
   * properties, just make a call here to print out all known properties at gwt
   * compile time.
   *
   * @param oracle
   *          - The property oracle to inspect and print out
   */
  public static void dumpProperties(final PropertyOracle oracle) {
    if (oracle instanceof ModuleSpacePropertyOracle) {
      ModuleSpacePropertyOracle mod = (ModuleSpacePropertyOracle) oracle;
      Properties props;
      try {
        Field field = mod.getClass().getDeclaredField("props");
        field.setAccessible(true);
        props = (Properties) field.get(mod);
        for (BindingProperty binding : props.getBindingProperties()) {
          System.out.println(binding.getName() + " : " + binding.getConstrainedValue());
        }
        System.out.println();
        for (ConfigurationProperty prop : props.getConfigurationProperties()) {
          System.out.print(prop.getName() + " : ");
          System.out.println(
            (prop.isMultiValued() || prop.allowsMultipleValues() ? prop.getValues() : prop.getValue())
            );
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (oracle instanceof DynamicPropertyOracle) {
      DynamicPropertyOracle stat = (DynamicPropertyOracle) oracle;
      for (BindingProperty binding : stat.getAccessedProperties()) {
        System.out.println("Accessed: " + binding.getName() + " : " + binding.getConstrainedValue());
      }
      try {
        ConfigurationProperty[] configProps;
        Field field = stat.getClass().getDeclaredField("configProps");
        field.setAccessible(true);
        configProps = (ConfigurationProperty[]) field.get(stat);
        System.out.println("Config props: ");
        for (ConfigurationProperty prop : configProps) {
          System.out.print(prop.getName() + " : ");
          System.out.println(
            (prop.isMultiValued() || prop.allowsMultipleValues() ? prop.getValues() : prop.getValue())
            );
        }
      } catch (Exception e) {
        e.printStackTrace(System.out);
      }
    }
  }

}
