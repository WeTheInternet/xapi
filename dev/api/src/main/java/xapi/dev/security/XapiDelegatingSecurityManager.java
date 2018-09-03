package xapi.dev.security;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/17/17.
 */
public final class XapiDelegatingSecurityManager extends XapiSecurityManager {

    private final SecurityManager original;

    public XapiDelegatingSecurityManager(SecurityManager original) {
        super(original);
        this.original = original;
    }

    @Override
    public void checkAccept(String host, int port) {
        original.checkAccept(host, port);
    }

    @Override
    public void checkAccess(Thread t) {
        original.checkAccess(t);
    }

    @Override
    public void checkAccess(ThreadGroup g) {
        original.checkAccess(g);
    }

    @Override
    public void checkConnect(String host, int port) {
        original.checkConnect(host, port);
    }

    @Override
    public void checkConnect(String host, int port, Object context) {
        original.checkConnect(host, port, context);
    }

    @Override
    public void checkCreateClassLoader() {
        original.checkCreateClassLoader();
    }

    @Override
    public void checkDelete(String file) {
        original.checkDelete(file);
    }

    @Override
    public void checkExec(String cmd) {
        original.checkExec(cmd);
    }

    @Override
    public void checkExit(int status) {
        original.checkExit(status);
    }

    @Override
    public void checkLink(String lib) {
        original.checkLink(lib);
    }

    @Override
    public void checkListen(int port) {
        original.checkListen(port);
    }

    @Override
    public void checkMulticast(InetAddress maddr) {
        original.checkMulticast(maddr);
    }

    @Override
    public void checkPackageAccess(String pkg) {
        original.checkPackageAccess(pkg);
    }

    @Override
    public void checkPackageDefinition(String pkg) {
        original.checkPackageDefinition(pkg);
    }

    @Override
    public void checkPermission(Permission perm) {
        original.checkPermission(perm);
    }

    @Override
    public void checkPermission(Permission perm, Object context) {
        original.checkPermission(perm, context);
    }

    @Override
    public void checkPrintJobAccess() {
        original.checkPrintJobAccess();
    }

    @Override
    public void checkPropertiesAccess() {
        original.checkPropertiesAccess();
    }

    @Override
    public void checkPropertyAccess(String key) {
        original.checkPropertyAccess(key);
    }

    @Override
    public void checkRead(String file) {
        original.checkRead(file);
    }

    @Override
    public void checkRead(FileDescriptor fd) {
        original.checkRead(fd);
    }

    @Override
    public void checkRead(String file, Object context) {
        original.checkRead(file, context);
    }

    @Override
    public void checkSecurityAccess(String target) {
        original.checkSecurityAccess(target);
    }

    @Override
    public void checkSetFactory() {
        original.checkSetFactory();
    }

    @Override
    public void checkWrite(String file) {
        original.checkWrite(file);
    }

    @Override
    public void checkWrite(FileDescriptor fd) {
        original.checkWrite(fd);
    }

    @Override
    @Deprecated
    public boolean checkTopLevelWindow(Object window) {
        return original.checkTopLevelWindow(window);
    }

    @Override
    @Deprecated
    public void checkAwtEventQueueAccess() {
        original.checkAwtEventQueueAccess();
    }

    @Override
    @Deprecated
    public void checkMemberAccess(Class<?> clazz, int which) {
        original.checkMemberAccess(clazz, which);
    }

    @Override
    @Deprecated
    public void checkMulticast(InetAddress maddr, byte ttl) {
        original.checkMulticast(maddr, ttl);
    }

    @Override
    @Deprecated
    public void checkSystemClipboardAccess() {
        original.checkSystemClipboardAccess();
    }

    @Override
    public boolean getInCheck() {
        return original.getInCheck();
    }

    @Override
    public Object getSecurityContext() {
        return original.getSecurityContext();
    }

    @Override
    public ThreadGroup getThreadGroup() {
        return original.getThreadGroup();
    }

}
