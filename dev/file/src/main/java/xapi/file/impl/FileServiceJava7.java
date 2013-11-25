package xapi.file.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import xapi.annotation.inject.SingletonOverride;
import xapi.file.api.FileService;
import xapi.log.X_Log;
import xapi.platform.JrePlatform;
import xapi.util.X_Debug;

@JrePlatform(version=7)
@SingletonOverride(implFor=FileService.class)
public class FileServiceJava7 extends FileServiceImpl {

  @Override
  public File chmod(int chmod, File file) {
    assertValidChmod(chmod);
    Set<PosixFilePermission> perms = getPermissions(chmod);
    try {
      Files.setPosixFilePermissions(Paths.get(file.getCanonicalPath()), perms);
    } catch (IOException e) {
      X_Log.warn(getClass(), "Could not chmod file",file,"with permissions","0x"+Integer.toHexString(chmod), e);
      X_Debug.maybeRethrow(e);
    }
    return file;
  }

  private Set<PosixFilePermission> getPermissions(int chmod) {
    Set<PosixFilePermission> perms = new HashSet<>();
    if ((chmod & 1) > 0) {
      perms.add(PosixFilePermission.OTHERS_EXECUTE);
    }
    if ((chmod & 2) > 0) {
      perms.add(PosixFilePermission.OTHERS_WRITE);
    }
    if ((chmod & 4) > 0) {
      perms.add(PosixFilePermission.OTHERS_READ);
    }
    if ((chmod & 0x10) > 0) {
      perms.add(PosixFilePermission.GROUP_EXECUTE);
    }
    if ((chmod & 0x20) > 0) {
      perms.add(PosixFilePermission.GROUP_WRITE);
    }
    if ((chmod & 0x40) > 0) {
      perms.add(PosixFilePermission.GROUP_READ);
    }
    if ((chmod & 0x100) > 0) {
      perms.add(PosixFilePermission.OWNER_EXECUTE);
    }
    if ((chmod & 0x200) > 0) {
      perms.add(PosixFilePermission.OWNER_WRITE);
    }
    if ((chmod & 0x400) > 0) {
      perms.add(PosixFilePermission.OWNER_READ);
    }
    return perms;
  }
  
}
