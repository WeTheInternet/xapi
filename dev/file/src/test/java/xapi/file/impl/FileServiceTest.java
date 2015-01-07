package xapi.file.impl;

import static xapi.test.Assert.assertFalse;
import static xapi.test.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.junit.Test;

import xapi.file.api.FileService;
import xapi.inject.X_Inject;

public class FileServiceTest {

  static {
    FileServiceTest.class.getClassLoader().setPackageAssertionStatus("xapi.file", true);
  }

  final FileService service = X_Inject.singleton(FileService.class);

  @Test
  public void testChmodValidation() throws Throwable {
    File f = File.createTempFile("testFile", "");
    service.chmod(0x111, f);
    assertChmod(0x111, f);
    service.chmod(0x777, f);
    assertChmod(0x777, f);
    service.chmod(0x222, f);
    assertChmod(0x222, f);
    service.chmod(0x555, f);
    assertChmod(0x555, f);
    service.chmod(0x555, f);
    assertChmod(0x555, f);
  }

  private void assertChmod(int chmod, File f) throws Throwable {
    Set<PosixFilePermission> permissions;
    try {
      permissions = Files.getPosixFilePermissions(Paths.get(f.getCanonicalPath()));
    } catch (UnsupportedOperationException e) {
      return;// running on windows... ew.
    }
    if ((chmod & 0x100) == 0) {
      assertFalse(permissions.contains(PosixFilePermission.OWNER_EXECUTE));
    } else {
      assertTrue(permissions.contains(PosixFilePermission.OWNER_EXECUTE));
    }
    if ((chmod & 0x200) == 0) {
      assertFalse(permissions.contains(PosixFilePermission.OWNER_WRITE));
    } else {
      assertTrue(permissions.contains(PosixFilePermission.OWNER_WRITE));
    }
    if ((chmod & 0x400) == 0) {
      assertFalse(permissions.contains(PosixFilePermission.OWNER_READ));
    } else {
      assertTrue(permissions.contains(PosixFilePermission.OWNER_READ));
    }

    if ((chmod & 0x10) == 0) {
      assertFalse(permissions.contains(PosixFilePermission.GROUP_EXECUTE));
    } else {
      assertTrue(permissions.contains(PosixFilePermission.GROUP_EXECUTE));
    }
    if ((chmod & 0x20) == 0) {
      assertFalse(permissions.contains(PosixFilePermission.GROUP_WRITE));
    } else {
      assertTrue(permissions.contains(PosixFilePermission.GROUP_WRITE));
    }
    if ((chmod & 0x40) == 0) {
      assertFalse(permissions.contains(PosixFilePermission.GROUP_READ));
    } else {
      assertTrue(permissions.contains(PosixFilePermission.GROUP_READ));
    }

    if ((chmod & 0x1) == 0) {
      assertFalse(permissions.contains(PosixFilePermission.OTHERS_EXECUTE));
    } else {
      assertTrue(permissions.contains(PosixFilePermission.OTHERS_EXECUTE));
    }
    if ((chmod & 0x2) == 0) {
      assertFalse(permissions.contains(PosixFilePermission.OTHERS_WRITE));
    } else {
      assertTrue(permissions.contains(PosixFilePermission.OTHERS_WRITE));
    }
    if ((chmod & 0x4) == 0) {
      assertFalse(permissions.contains(PosixFilePermission.OTHERS_READ));
    } else {
      assertTrue(permissions.contains(PosixFilePermission.OTHERS_READ));
    }

  }
}
