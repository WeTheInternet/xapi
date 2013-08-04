/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.lang;

import java.security.*;
import java.io.File;

/**
 * Stripped down emulation-layer only no-op SecurityManager.
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public 
class SecurityManager {

    public SecurityManager() {
    }
    public Object getSecurityContext() {
      return AccessController.getContext();
    }
    public void checkPermission(Permission perm) {
    }
    public void checkPermission(Permission perm, Object context) {
    }
    public void checkCreateClassLoader() {
    }
    public void checkAccess(Thread t) {
    }
//    public void checkAccess(ThreadGroup g) {
//    }
    public void checkExit(int status) {
    }
    public void checkExec(String cmd) {
    }
    public void checkLink(String lib) {
    }
//    public void checkRead(FileDescriptor fd) {
//    }
    public void checkRead(String file) {
    }
    public void checkRead(String file, Object context) {
    }
    public void checkWrite(String file) {
    }
    public void checkDelete(String file) {
    }
    public void checkConnect(String host, int port) {
    }
    public void checkConnect(String host, int port, Object context) {
    }
    public void checkListen(int port) {
    }
    public void checkAccept(String host, int port) {
    }
//    public void checkMulticast(InetAddress maddr) {
//    }
//    public void checkMulticast(InetAddress maddr, byte ttl) {
//    }
    public void checkPropertiesAccess() {
    }
    public void checkPropertyAccess(String key) {
    }
    public boolean checkTopLevelWindow(Object window) {
      return true;
    }
    public void checkPrintJobAccess() {
    }
    public void checkSystemClipboardAccess() {
    }
    public void checkAwtEventQueueAccess() {
    }
    public void checkPackageAccess(String pkg) {
    }
    public void checkPackageDefinition(String pkg) {
    }
    public void checkSetFactory() {
    }
    public void checkMemberAccess(Class<?> clazz, int which) {
    }
    public void checkSecurityAccess(String target) {
    }
//    public ThreadGroup getThreadGroup() {
//      return null;
//    }

}
