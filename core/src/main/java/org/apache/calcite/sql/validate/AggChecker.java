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
package org.apache.calcite.sql.validate;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlUtil;
import org.apache.calcite.sql.util.SqlBasicVisitor;
import org.apache.calcite.util.Litmus;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.apache.calcite.util.Static.RESOURCE;

/**
 * Visitor which throws an exception if any component of the expression is not a
 * group expression.
 */
class AggChecker extends SqlBasicVisitor<Void> {
  //~ Instance fields --------------------------------------------------------

  private final Deque<SqlValidatorScope> scopes = new ArrayDeque<>();
  private final List<SqlNode> groupExprs;
  private boolean distinct;
  private SqlValidatorImpl validator;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates an AggChecker.
   *
   * @param validator  Validator
   * @param scope      Scope
   * @param groupExprs Expressions in GROUP BY (or SELECT DISTINCT) clause,
   *                   that are therefore available
   * @param distinct   Whether aggregation checking is because of a SELECT
   *                   DISTINCT clause
   */
  AggChecker(
      SqlValidatorImpl validator,
      AggregatingScope scope,
      List<SqlNode> groupExprs,
      boolean distinct) {
    this.validator = validator;
    this.groupExprs = groupExprs;
    this.distinct = distinct;
    this.scopes.push(scope);
  }

  //~ Methods ----------------------------------------------------------------

  boolean isGroupExpr(SqlNode expr) {
    for (SqlNode groupExpr : groupExprs) {
      if (groupExpr.equalsDeep(expr, Litmus.IGNORE)) {
        return true;
      }
    }
    return false;
  }

  public Void visit(SqlIdentifier id) {
    if (isGroupExpr(id)) {
      return null;
    }

    // If it '*' or 'foo.*'?
    if (id.isStar()) {
      assert false : "star should have been expanded";
    }

    // Is it a call to a parentheses-free function?
    SqlCall call =
        SqlUtil.makeCall(
            validator.getOperatorTable(),
            id);
    if (call != null) {
      return call.accept(this);
    }

    // Didn't find the identifier in the group-by list as is, now find
    // it fully-qualified.
    // TODO: It would be better if we always compared fully-qualified
    // to fully-qualified.
    final SqlQualified fqId = scopes.peek().fullyQualify(id);
    if (isGroupExpr(fqId.identifier)) {
      return null;
    }
    SqlNode originalExpr = validator.getOriginal(id);
    final String exprString = originalExpr.toString();
    throw validator.newValidationError(originalExpr,
        distinct
            ? RESOURCE.notSelectDistinctExpr(exprString)
            : RESOURCE.notGroupExpr(exprString));
  }

  public Void visit(SqlCall call) {
    final SqlValidatorScope scope = scopes.peek();
    if (call.getOperator().isAggregator()) {
      if (distinct) {
        if (scope instanceof AggregatingSelectScope) {
          SqlNodeList selectList =
              ((SqlSelect) scope.getNode()).getSelectList();

          // Check if this aggregation function is just an element in the select
          for (SqlNode sqlNode : selectList) {
            if (sqlNode.getKind() == SqlKind.AS) {
              sqlNode = ((SqlCall) sqlNode).operand(0);
            }

            if (validator.expand(sqlNode, scope)
                .equalsDeep(call, Litmus.IGNORE)) {
              return null;
            }
          }
        }

        // Cannot use agg fun in ORDER BY clause if have SELECT DISTINCT.
        SqlNode originalExpr = validator.getOriginal(call);
        final String exprString = originalExpr.toString();
        throw validator.newValidationError(call,
            RESOURCE.notSelectDistinctExpr(exprString));
      }

      // For example, 'sum(sal)' in 'SELECT sum(sal) FROM emp GROUP
      // BY deptno'
      return null;
    }
    if (call.getKind() == SqlKind.FILTER) {
      call.operand(0).accept(this);
      return null;
    }
    // Visit the operand in window function
    if (call.getOperator().getKind() == SqlKind.OVER) {
      SqlCall windowFunction = call.operand(0);
      if (windowFunction.getOperandList().size() != 0) {
        windowFunction.operand(0).accept(this);
      }
    }
    if (isGroupExpr(call)) {
      // This call matches an expression in the GROUP BY clause.
      return null;
    }
    if (call.isA(SqlKind.QUERY)) {
      // Allow queries for now, even though they may contain
      // references to forbidden columns.
      return null;
    }

    // Switch to new scope.
    SqlValidatorScope newScope = scope.getOperandScope(call);
    scopes.push(newScope);

    // Visit the operands (only expressions).
    call.getOperator()
        .acceptCall(this, call, true, ArgHandlerImpl.<Void>instance());

    // Restore scope.
    scopes.pop();
    return null;
  }
}

// End AggChecker.java
