package xapi.time.impl;

import java.util.Calendar;

import xapi.annotation.inject.SingletonDefault;
import xapi.time.service.TimeService;

@SingletonDefault(implFor = TimeService.class)
public class TimeServiceDefault extends AbstractTimeService{

  private static final long serialVersionUID = -8070323612852368910L;

  @Override
  public void runLater(Runnable runnable) {
    new Thread(runnable).start();
  }

  @Override
  public String timestamp() {
    // Ya...  It's more lines of code than using a library,
    // but it's also the minimum overhead possible.
    Calendar c = Calendar.getInstance();
    char[] result = "yyyy-MM-ddTHH:mm.sss+00:00".toCharArray();
    // yyyy
    int val = c.get(Calendar.YEAR);
    result[3] = Character.forDigit(val%10, 10);
    result[2] = Character.forDigit((val/=10)%10, 10);
    result[1] = Character.forDigit((val/=10)%10, 10);
    result[0] = Character.forDigit((val/10)%10, 10);
    
    val = c.get(Calendar.MONTH);
    result[5] = Character.forDigit(val/10, 10);
    result[6] = Character.forDigit(val%10, 10);
    val = c.get(Calendar.DATE);
    result[8] = Character.forDigit(val/10, 10);
    result[9] = Character.forDigit(val%10, 10);
    val = c.get(Calendar.HOUR);
    result[11] = Character.forDigit(val/10, 10);
    result[12] = Character.forDigit(val%10, 10);
    val = c.get(Calendar.MINUTE);
    result[14] = Character.forDigit(val/10, 10);
    result[15] = Character.forDigit(val%10, 10);
    val = c.get(Calendar.MILLISECOND);
    result[19] = Character.forDigit(val%10, 10);
    result[18] = Character.forDigit((val/=10)%10, 10);
    result[17] = Character.forDigit((val/10)%10, 10);
    
    val = c.getTimeZone().getOffset(c.getTimeInMillis()) / 60000;
    if (val < 0) {
      result[20] = '-';
      val = -val;
    }
    int hours = val / 60;
    result[21] = Character.forDigit(hours/10, 10);
    result[22] = Character.forDigit(hours%10, 10);
    val = val%60;
    result[24] = Character.forDigit((val/10)%10, 10);
    result[25] = Character.forDigit(val%10, 10);
    
    return new String(result);
  }
}
