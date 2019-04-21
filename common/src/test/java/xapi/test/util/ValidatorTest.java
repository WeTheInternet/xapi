package xapi.test.util;

import org.junit.Assert;
import org.junit.Test;

import xapi.util.validators.ChecksValidEmail;

public class ValidatorTest {

  @Test
  public void testValidEmail() {
    String error = ChecksValidEmail.SINGLETON.validate("test.user@domain.com", "");
    Assert.assertNull(error);
  }

  @Test
  public void testInvalidUsername() {
    String error = ChecksValidEmail.SINGLETON.validate("test. user@domain.com", "");
    Assert.assertEquals(error, "[username can only contain letter, numbers and . _ or - ] You sent: test. user@domain.com");
  }

  @Test
  public void testInvalidDomain() {
    String error = ChecksValidEmail.SINGLETON.validate("test.user@doma&in.com", "");
    Assert.assertEquals(error, "[domain name can only contain letter, numbers and . or - ] You sent: test.user@doma&in.com");
  }

  @Test
  public void testDomainMissingTld() {
    String error = ChecksValidEmail.SINGLETON.validate("test.user@domaincom", "");
    Assert.assertEquals(error, "[domain name must contain a . ] You sent: test.user@domaincom");
  }

  @Test
  public void testNoUsername() {
    String error = ChecksValidEmail.SINGLETON.validate("@domain.com", "");
    Assert.assertEquals(error, "[value cannot start with @] You sent: @domain.com");
  }

  @Test
  public void testNoAtSign() {
    String error = ChecksValidEmail.SINGLETON.validate("test.user-domain.com", "");
    Assert.assertEquals(error, "[value must contain @] +You sent: test.user-domain.com");
  }

}
