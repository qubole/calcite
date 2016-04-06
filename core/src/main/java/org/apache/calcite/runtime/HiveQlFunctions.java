/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.runtime;

import org.apache.calcite.avatica.util.DateTimeUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by amoghm on 4/6/16.
 */
public class HiveQlFunctions {
  private HiveQlFunctions() {
  }
  /**
   * Converts timestamp string to unix date string
   * implements to_date builtin function of Hive.
   * @param timestamp String depicting timestamp
   * @return string containing unix date
   */
  public static String toDate(String timestamp) {
    long ts =
        DateTimeUtils.timestampStringToUnixDate(timestamp);
    int date = (int) (ts / 86400000L);
    int time = (int) (ts % 86400000L);
    if (time < 0) {
      --date;
    }
    return DateTimeUtils.unixDateToString(date);
  }

  public static Long unixTimeStamp() {
    return System.currentTimeMillis() / 1000L;
  }

  public static String fromUnixTime(Long secondsEpoch) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Long millis = secondsEpoch * 1000L;
    Date date = new Date(millis);
    return sdf.format(date);
  }
  public static String dateSub(String timeStamp, Integer toBeSubed) {
    Calendar calendar = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    try {
      calendar.setTime(sdf.parse(timeStamp));
    } catch (ParseException e) {
      return null;
    }
    calendar.add(Calendar.DAY_OF_MONTH, -toBeSubed);
    Date newDate = calendar.getTime();
    return sdf.format(newDate);
  }
}
// End HiveQlFunctions.java
