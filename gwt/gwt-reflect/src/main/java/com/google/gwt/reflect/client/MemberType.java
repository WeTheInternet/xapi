package com.google.gwt.reflect.client;

public enum MemberType {
  Method, Constructor, Field;
  static final int
  KeepAnnotations = 1,
  KeepParameterAnnotations = 2,
  KeepExceptions = 4,
  KeepDefaults = 8,
  KeepAll = 0xf;
}
