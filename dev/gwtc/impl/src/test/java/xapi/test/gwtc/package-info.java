@Gwtc(
  includeGwtXml={
    @Resource("xapi.X_Inherit")
  }
//  ,includeHostHtml={
//    @UiTemplate(
//      value="<div>"
//            + "<h4>"+$package+"</h4>"
//            + $child
//          + "</div>",
//      embedStrategy=WrapEachPackage,
//      keys={$child, $package}
//    ),
//    @UiTemplate(
//      value="<div>"
//          + "<h5>"+$class+"</h5>"
//          + $child
//        + "</div>",
//        embedStrategy=WrapEachClass,
//        keys={$child, $class}
//    ),
//    @UiTemplate(
//      value="<div>"
//          + "<h5>"+$class+"."+$method+"</h5>"
//          + $child
//        + "</div>",
//        embedStrategy=WrapEachMethod,
//        keys={$method, $class}
//    ),
//  }
)
package xapi.test.gwtc;
import static xapi.annotation.ui.UiTemplate.EmbedStrategy.*;
import static xapi.annotation.ui.UiTemplate.*;
import xapi.annotation.ui.UiTemplate;
import xapi.annotation.compile.Resource;
import xapi.gwtc.api.Gwtc;
