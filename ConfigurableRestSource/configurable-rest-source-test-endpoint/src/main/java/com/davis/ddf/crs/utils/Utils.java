package com.davis.ddf.crs.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * This software was created for rights to this software belong to appropriate licenses and
 * restrictions apply.
 *
 * @author Samuel Davis created on 9/5/17.
 */
public class Utils {
  private Utils() {}

  /**
   * Transform date string.
   *
   * @param date the date
   * @param dateFormat the date format
   * @return the string
   */
  public static String transformDate(Date date, SimpleDateFormat dateFormat) {
    Calendar c = Calendar.getInstance();
    TimeZone localTimeZone = c.getTimeZone();
    TimeZone afgTimeZone = TimeZone.getTimeZone("Asia/Kabul");
    int localOffsetFromUTC = localTimeZone.getRawOffset();
    int afghanOffsetFromUTC = afgTimeZone.getRawOffset();
    Calendar afghanCal = Calendar.getInstance(afgTimeZone);
    afghanCal.setTimeInMillis(date.getTime());
    afghanCal.add(Calendar.MILLISECOND, (-1 * localOffsetFromUTC));
    afghanCal.add(Calendar.MILLISECOND, afghanOffsetFromUTC);
    return dateFormat.format(afghanCal.getTime());
  }
}
