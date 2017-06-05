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
package org.apache.calcite.sql.fun;

import org.apache.calcite.sql.SqlFunction;
import org.apache.calcite.sql.SqlFunctionCategory;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.type.OperandTypes;
import org.apache.calcite.sql.type.ReturnTypes;
import org.apache.calcite.sql.type.SqlTypeFamily;
import org.apache.calcite.sql.util.ReflectiveSqlOperatorTable;

/**
 * Operator table that contains only Hive-specific functions and operators.
 */
public class HiveSqlOperatorTable extends ReflectiveSqlOperatorTable {
  //~ Static fields/initializers ---------------------------------------------

  /**
   * The table of contains Hive-specific operators.
   */
  private static HiveSqlOperatorTable instance;

  /** Hive built-in method to convert timestamp string
   * into date strings */
  public static final SqlFunction HIVE_TO_DATE =
      new SqlFunction("TO_DATE", SqlKind.HIVE_OP,
          ReturnTypes.VARCHAR_2000, null,
          OperandTypes.STRING, SqlFunctionCategory.TIMEDATE);

  public static final SqlFunction HIVE_UNIX_TIMESTAMP =
      new SqlFunction("UNIX_TIMESTAMP", SqlKind.HIVE_OP,
          ReturnTypes.BIGINT, null, OperandTypes.NILADIC,
          SqlFunctionCategory.TIMEDATE);

  public static final SqlFunction HIVE_FROM_UNIXTIME =
      new SqlFunction("FROM_UNIXTIME", SqlKind.HIVE_OP,
          ReturnTypes.VARCHAR_2000, null, OperandTypes.NUMERIC,
          SqlFunctionCategory.TIMEDATE);

  public static final SqlFunction HIVE_DATE_SUB =
      new SqlFunction("DATE_SUB", SqlKind.HIVE_OP,
          ReturnTypes.VARCHAR_2000, null,
          OperandTypes.family(SqlTypeFamily.STRING,
              SqlTypeFamily.INTEGER),
          SqlFunctionCategory.TIMEDATE);

  /**
   * Returns the Hive operator table, creating it if necessary.
   */
  public static synchronized HiveSqlOperatorTable instance() {
    if (instance == null) {
      // Creates and initializes the standard operator table.
      // Uses two-phase construction, because we can't initialize the
      // table until the constructor of the sub-class has completed.
      instance = new HiveSqlOperatorTable();
      instance.init();
    }
    return instance;
  }
}

// End HiveSqlOperatorTable.java
