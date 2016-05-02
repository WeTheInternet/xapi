Feature: Compile templates into valid java files

Scenario: Perform direct ui element transpilation
Given compile ui with name HelloWorld:
  | <div>Hello World </div> |
Then source code of HelloWorld is:
  | new PotentialElement("div").append("Hello World ") .build() |

Scenario: Parse a template with children
Given compile ui with name HelloWorld:
  | <template id="test">                        |
  | <a href="javascript:void(0)">  |
  | <content />                                 |
  | </a>                                        |
  | </template>                                 |
Then source code of HelloWorld is:
  | new PotentialElement("template")            |
  | .set("id", "test")                          |
  |                                             |
  | .createChild("a")                           |
  | .set("href", "javascript:void(0)")          |
  |                                             |
  | .createChild("content")                     |
  | .finishChild()                              |
  |                                             |
  | .finishChild()                              |
  |                                             |
  | .build()                                    |

Scenario: Parse a template with an html class attribute
Given compile ui with name HelloWorld:
  | <template id="test"  class="link" /> |
Then source code of HelloWorld is:
  | new PotentialElement("template")            |
  | .set("id", "test")                          |
  | .set("class", "link")                       |
  | .build()                                    |

Scenario: Parse a template with an html comment
Given compile ui with name HelloWorld:
  | <template id="test">                        |
  | <!-- This is a comment -->                  |
  | </template>                                 |
Then source code of HelloWorld is:
  | new PotentialElement("template")            |
  | .set("id", "test")                          |
  | .build()                                    |

Scenario: Create a class from a template file
Given compile component with name HelloWorld:
  | import xapi.ui.api.*;                                                                 |
  |                                                                                       |
  | @ShadowDom(`                                                                          |
  |        <h1>$title</h1>                                                                |
  |        <div>$children</div>                                                           |
  |        <if isNotNull=$footer>                                                         |
  |           <div>$footer</div>                                                          |
  |        </if>                                                                          |
  | `)                                                                                    |
  | interface Test extends UiComponent {                                                  |
  |   // Only put the methods you want to be public                                       |
  |   // Anything that is missing will be generated                                       |
  |   // ...I hope ;-)                                                                    |
  |   void setTitle(String title);                                                        |
  |   String getTitle();                                                                  |
  |                                                                                       |
  |   void setFooter(String footer);                                                      |
  |   String getFooter();                                                                 |
  |                                                                                       |
  |   static Test newInstance(String title, String footer,                                |
  |      UiComponent ... children) {                                                      |
  |      return `<Test title=$title                                                       |
  |                    footer=$footer                                                   |
  |                    children=$children />`;                                            |
  |  }                                                                                    |
  |                                                                                       |
  | }                                                                                     |
Then source code of HelloWorld is:
  | import xapi.ui.api.*;                                                                 |
  |                                                                                       |
  | @ShadowDom("<h1>$title</h1>\n" +                                                      |
  | "<div>$children</div>\n" +                                                            |
  | "<if isNotNull=$footer>\n" +                                                          |
  | "<div>$footer</div>\n" +                                                              |
  | "</if>")                                                                              |
  | interface Test extends UiComponent {                                                  |
  |                                                                                       |
  | // Only put the methods you want to be public                                         |
  | // Anything that is missing will be generated                                         |
  | // ...I hope ;-)                                                                      |
  | void setTitle(String title);                                                          |
  |                                                                                       |
  | String getTitle();                                                                    |
  |                                                                                       |
  | void setFooter(String footer);                                                        |
  |                                                                                       |
  | String getFooter();                                                                   |
  |                                                                                       |
  | static Test newInstance(String title, String footer, UiComponent... children) {       |
  | return new PotentialElement("Test")                                                   |
  |      .set("title", xapi.fu.X_Fu.coerce(title))                                        |
  |      .set("footer", xapi.fu.X_Fu.coerce(footer))                                      |
  |      .set("children", xapi.fu.X_Fu.coerce(children))                                  |
  |      .build();                                                                        |
  | }                                                                                     |
  | }                                                                                     |
