package wetheinter.net.dev.il8n;

import xapi.annotation.inject.SingletonDefault;

@SingletonDefault(implFor=CodeServerDebugMessages.class)
public class Messages_EN implements CodeServerDebugMessages{

  @Override
  public String unableToStartServer() {
    return "Unable to start codeserver: ";
  }
  
  @Override
  public String unableToParseArguments() {
    return "Unable to parse codeserver arguments: ";
  }
  
}
