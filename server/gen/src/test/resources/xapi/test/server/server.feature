Feature:
  Use XApi lang to create a complete server application.

  Scenario:
  Compile a simple hello world server

    Given Generate web app named HelloWorld:
      | <web-app /> |
    Then Expect web app named HelloWorld to have source:
      |                                           |
      | public interface HelloWorldComponent { |
      |                                           |
      | }                                         |
