Feature:
  Use XApi lang to create a complete server application.

  Scenario:
  Compile a simple hello world server

    Given Generate web app named HelloWorld:
      | <web-app /> |
    Then Expect web app named HelloWorld to have source:
      | package xapi.generated.web;                                                                                                  |
      |                                                                                                                              |
      | import xapi.fu.In1;                                                                                                          |
      | import xapi.scope.request.RequestScope;                                                                                            |
      | import xapi.server.api.WebApp;                                                                                               |
      | import xapi.server.api.XapiServer;                                                                                           |
      | import xapi.server.api.XapiServerPlugin;                                                                                     |
      |                                                                                                                              |
      | public class BaseHelloWorldComponent <Request extends RequestScope> implements HelloWorldComponent, XapiServerPlugin<Request> { |
      |                                                                                                                              |
      | public In1<XapiServer<Request>> installToServer (WebApp app) {                                                     |
      |   return In1.ignored();                                                                                                        |
      | }                                                                                                                            |
      |                                                                                                                              |
      | }                                                                                                          |

  Scenario:
  Compile a simple <route />

    Given Generate web app named HelloWorld:
      | <web-app                                |
      |   routes=[                              |
      |     <route                              |
      |       path = "/"                        |
      |       method = "GET"                    |
      |       response = <page                  |
      |         dom = <div>"HelloWorld"</div>   |
      |       /page>                            |
      |     /route>                             |
      |   ]                                     |
      | /web-app>                               |
    Then Expect web app named HelloWorld to have source:
      | package xapi.generated.web;                                                                                                                       |
      |                                                                                                                                                   |
      | import static xapi.model.X_Model.create;                                                                                                         |
      | import static xapi.server.api.Route.RouteType.File;                                                                                               |
      | import xapi.fu.In1;                                                                                                                               |
      | import xapi.scope.request.RequestScope;                                                                                                           |
      | import xapi.server.api.Route;                                                                                                                     |
      | import xapi.server.api.WebApp;                                                                                                                    |
      | import xapi.server.api.XapiServer;                                                                                                                |
      | import xapi.server.api.XapiServerPlugin;                                                                                                          |
      | public class BaseHelloWorldComponent <Request extends RequestScope> implements HelloWorldComponent, XapiServerPlugin<Request> { |
      |                                                                                                                                                   |
      |   public In1<XapiServer<Request>> installToServer (WebApp app) {                                                                          |
      |     installRoute(app);                                                                                                                                |
      |     return In1.ignored();                                                                                                                             |
      |   }                                                                                                                                                 |
      |                                                                                                                                                   |
      |   public void installRoute (WebApp app) {                                                                                                                  |
      |     Route route = create(Route.class);                                                                                                                |
      |     route.setPayload("/GetPage_");                                                                                                                    |
      |     route.setRouteType(File);                                                                                                                         |
      |     route.setPath("/");                                                                                                                               |
      |     app.getRoute().add(route);                                                                                                                       |
      |   }                                                                                                                                                 |
      |                                                                                                                                                   |
      | }                                                                                                          |

  Scenario:
  Compile a <route /> using a <template />

    Given Generate web app named TemplateWorld:
      | <web-app                                |
      |   templates=[                           |
      |     <template name="hi">                |
      |       <page                             |
      |         dom = <div>Templatey</div>      |
      |       /page>                            |
      |     </template>                         |
      |   ]                                     |
      |   routes=[                              |
      |     <route                              |
      |       path = "/"                        |
      |       method = "GET"                    |
      |       response = <page                  |
      |         template = "hi"                 |
      |       /page>                            |
      |     /route>                             |
      |   ]                                     |
      | /web-app>                               |
    Then Expect web app named TemplateWorld to have source:
      | package xapi.generated.web;                                                                                                                          |
      |                                                                                                                                                      |
      | import static xapi.model.X_Model.create;                                                                                                             |
      | import static xapi.server.api.Route.RouteType.File;                                                                                                  |
      | import xapi.fu.In1;                                                                                                                                  |
      | import xapi.scope.request.RequestScope;                                                                                                              |
      | import xapi.server.api.Route;                                                                                                                        |
      | import xapi.server.api.WebApp;                                                                                                                       |
      | import xapi.server.api.XapiServer;                                                                                                                   |
      | import xapi.server.api.XapiServerPlugin;                                                                                                             |
      |                                                                                                                                                      |
      | public class BaseTemplateWorldComponent <Request extends RequestScope> implements TemplateWorldComponent, XapiServerPlugin<Request> { |
      |                                                                                                                                                      |
      |   public In1<XapiServer<Request>> installToServer (WebApp app) {                                                                           |
      |     installRoute(app);                                                                                                                               |
      |     return In1.ignored();                                                                                                                            |
      |   }                                                                                                                                                  |
      |                                                                                                                                                      |
      |   public void installRoute (WebApp app) {                                                                                                            |
      |     Route route = create(Route.class);                                                                                                               |
      |     route.setPayload("/GetPage_");                                                                                                                   |
      |     route.setRouteType(File);                                                                                                                        |
      |     route.setPath("/");                                                                                                                              |
      |     app.getRoute().add(route);                                                                                                                       |
      |   }                                                                                                                                                  |
      |                                                                                                                                                      |
      | }                                                                                                                                                    |
