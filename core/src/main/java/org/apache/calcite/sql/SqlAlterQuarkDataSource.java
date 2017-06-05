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
package org.apache.calcite.sql;

import org.apache.calcite.sql.parser.SqlParserPos;

/**
 * A <code>SqlAlterQuarkDataSource</code> is a node of a parse tree which represents an ALTER
 * metadata for Quark DataSource
 */
public class SqlAlterQuarkDataSource extends SqlAlterQuark {
  //~ Constructors -----------------------------------------------------------

  public SqlAlterQuarkDataSource(SqlParserPos pos,
                       SqlNodeList targetColumnList,
                       SqlNodeList sourceExpressionList,
                       SqlNode condition) {
    super(pos, targetColumnList, sourceExpressionList, condition);
    operator = new SqlSpecialOperator("ALTER_DATASOURCE", SqlKind.OTHER_DDL);
    operatorString = "ALTER DATASOURCE";
  }
}

// End SqlAlterQuarkDataSource.java
