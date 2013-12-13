@Gwtc(
  includeGwtXml={
    @Resource("xapi.X_Inherit")
  },
  includeHostHtml={
    @UiTemplate(
      value="<div>"
            + "<h3>$package</h3>"
          + "</div>",
      embedStrategy=WrapEachPackage,
      keys={"$package","$childId"}
    )
  }
)
package xapi.test;
import static xapi.annotation.ui.UiTemplate.EmbedStrategy.*;
import xapi.annotation.ui.UiTemplate;
import xapi.annotation.compile.Resource;
import xapi.gwtc.api.Gwtc;
