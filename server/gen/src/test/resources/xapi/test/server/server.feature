Feature:
  Use XApi lang to create a complete server application.

  Scenario:
  Compile a simple hello world server

    Given Generate web app named HelloWorld:
      | <web-app /> |
    Then Expect web app named HelloWorld to have source:
      | import xapi.fu.In1;                                                                                                          |
      | import xapi.scope.request.RequestLike;                                                                                            |
      | import xapi.scope.request.ResponseLike;                                                                                            |
      | import xapi.server.api.WebApp;                                                                                               |
      | import xapi.server.api.XapiServer;                                                                                           |
      | import xapi.server.api.XapiServerPlugin;                                                                                     |
      |                                                                                                                              |
      | public class BaseHelloWorldComponent <Request extends RequestLike, Response extends ResponseLike> implements XapiServerPlugin<Request,Response> { |
      |                                                                                                                              |
      | public In1<XapiServer<Request, Response>> installToServer (WebApp app) {                                                     |
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
      | import xapi.fu.In1;                                                                   |
      | import xapi.scope.request.RequestLike;                                                                          |
      | import xapi.scope.request.ResponseLike;                                                                          |
      | import xapi.server.api.WebApp;                                                                   |
      | import xapi.server.api.XapiServer;                                                                         |
      | import xapi.server.api.XapiServerPlugin;                                                                   |
      |                                                                                                            |
      | public class BaseHelloWorldComponent <Request extends RequestLike, Response extends ResponseLike> implements XapiServerPlugin<Request,Response> { |
      |                                                                                                            |
      |   public In1<XapiServer<Request, Response>> installToServer (WebApp app) {                                                     |
      |     installRoute(app);                                                                                     |
      |     return In1.ignored();                                                                                     |
      |   }                                                                                                        |
      |                                                                                                            |
      |   public void installRoute (WebApp app) {                                                                  |
      |   }                                                                                                        |
      |                                                                                                            |
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
      | import xapi.fu.In1;                                                                   |
      | import xapi.scope.request.RequestLike;                                                                          |
      | import xapi.scope.request.ResponseLike;                                                                          |
      | import xapi.server.api.WebApp;                                                                   |
      | import xapi.server.api.XapiServer;                                                                         |
      | import xapi.server.api.XapiServerPlugin;                                                                   |
      |                                                                                                            |
      | public class BaseTemplateWorldComponent <Request extends RequestLike, Response extends ResponseLike> implements XapiServerPlugin<Request,Response> { |
      |                                                                                                            |
      |   public In1<XapiServer<Request, Response>> installToServer (WebApp app) {                                                     |
      |     installRoute(app);                                                                                     |
      |     return In1.ignored();                                                                                     |
      |   }                                                                                                        |
      |                                                                                                            |
      |   public void installRoute (WebApp app) {                                                                  |
      |   }                                                                                                        |
      |                                                                                                            |
      | }                                                                                                          |
