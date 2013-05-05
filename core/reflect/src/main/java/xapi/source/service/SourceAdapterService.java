package xapi.source.service;

import xapi.source.api.IsClass;
import xapi.source.api.IsField;
import xapi.source.api.IsMethod;
import xapi.source.api.IsAnnotation;

public interface SourceAdapterService
<ClassType, MethodType, FieldType, AnnotationType>
{

  IsClass toClass(ClassType type);
  IsMethod toMethod(MethodType type);
  IsField toField(FieldType type);
  IsAnnotation toAnnotation(AnnotationType type);
  
}
