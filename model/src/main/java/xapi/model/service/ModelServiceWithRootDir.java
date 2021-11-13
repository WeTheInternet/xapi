package xapi.model.service;

import xapi.dev.source.CharBuffer;
import xapi.fu.itr.MappedIterable;
import xapi.model.api.*;
import xapi.source.lex.CharIterator;
import xapi.util.api.SuccessHandler;

import java.io.File;
import java.lang.reflect.Method;

public interface ModelServiceWithRootDir extends ModelService {

  void setRootDir(File rootDir);
}
