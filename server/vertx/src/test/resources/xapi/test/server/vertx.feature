Feature:
  Use XApi lang to create a complete server application.

  Background:
    Given Use vert.x generator

  Scenario:
  Compile a simple hello world server

    Given Generate web app named HelloWorld:
      | <web-app  |
      |   routes = <route  |
      |     path = "hello" |
      |     method = "GET" |
      |     response = <html> |
      |       <body>Hello World</body> |
      |     </html> |
      |   /> |
      | /> |
    And Run web app named HelloWorld
    Then Expect url /hello to return:
      | <html><body>Hello World</body></html> |
