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
package org.apache.calcite.plan;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexExecutorImpl;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexShuttle;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Extension to {@link SubstitutionVisitor}.
 */
public class MaterializedViewSubstitutionVisitor extends SubstitutionVisitor {
  private static final ImmutableList<UnifyRule> EXTENDED_RULES =
      ImmutableList.<UnifyRule>builder()
          .addAll(DEFAULT_RULES)
          .add(ProjectToProjectUnifyRule1.INSTANCE)
          .add(FilterToProjectOnFilterUnifyRule.INSTANCE)
          .build();

  public MaterializedViewSubstitutionVisitor(RelNode target_, RelNode query_) {
    super(target_, query_, EXTENDED_RULES);
  }

  public List<RelNode> go(RelNode replacement_) {
    return super.go(replacement_);
  }

  /**
   * Project to Project Unify rule.
   */
  private static class ProjectToProjectUnifyRule1 extends AbstractUnifyRule {
    public static final ProjectToProjectUnifyRule1 INSTANCE =
        new ProjectToProjectUnifyRule1();

    private ProjectToProjectUnifyRule1() {
      super(operand(MutableProject.class, query(0)),
          operand(MutableProject.class, target(0)), 1);
    }

    @Override protected UnifyResult apply(UnifyRuleCall call) {
      final MutableProject query = (MutableProject) call.query;

      final List<RelDataTypeField> oldFieldList =
          query.getInput().getRowType().getFieldList();
      final List<RelDataTypeField> newFieldList =
          call.target.getRowType().getFieldList();
      List<RexNode> newProjects;
      try {
        newProjects = transformRex(query.getProjects(), oldFieldList, newFieldList);
      } catch (MatchFailed e) {
        return null;
      }

      final MutableProject newProject =
          MutableProject.of(
              query.getRowType(), call.target, newProjects);

      final MutableRel newProject2 = MutableRels.strip(newProject);
      return call.result(newProject2);
    }

    @Override protected UnifyRuleCall match(SubstitutionVisitor visitor,
        MutableRel query, MutableRel target) {
      assert query instanceof MutableProject && target instanceof MutableProject;

      if (queryOperand.matches(visitor, query)) {
        if (targetOperand.matches(visitor, target)) {
          return null;
        } else if (targetOperand.isWeaker(visitor, target)) {

          final MutableProject queryProject = (MutableProject) query;
          if (queryProject.getInput() instanceof MutableFilter) {
            final MutableFilter innerFilter =
                (MutableFilter) queryProject.getInput();
            RexNode newCondition;
            try {
              newCondition = transformRex(innerFilter.getCondition(),
                  innerFilter.getInput().getRowType().getFieldList(),
                  target.getRowType().getFieldList());
            } catch (MatchFailed e) {
              return null;
            }
            final MutableFilter newFilter = MutableFilter.of(target,
                newCondition);

            return visitor.new UnifyRuleCall(this, query, newFilter,
                copy(visitor.slots, slotCount));
          }
        }
      }
      return null;
    }
  }

  /** Implementation of {@link UnifyRule} that matches a {@link MutableFilter}
   * on a {@link org.apache.calcite.rel.core.TableScan}
   * to a {@link MutableProject} on a {@link MutableFilter}
   * on a {@link org.apache.calcite.rel.core.TableScan}. */
  private static class FilterToProjectOnFilterUnifyRule extends AbstractUnifyRule {
    public static final FilterToProjectOnFilterUnifyRule INSTANCE =
        new FilterToProjectOnFilterUnifyRule();

    private FilterToProjectOnFilterUnifyRule() {
      super(operand(MutableFilter.class, query(0)),
          operand(MutableProject.class,
              operand(MutableFilter.class, target(0))), 1);
    }

    public UnifyResult apply(UnifyRuleCall call) {
      final MutableRel rel0 = call.query;
      final MutableRel rel = ((MutableProject) call.target).getInput();
      RexExecutorImpl rexImpl =
          (RexExecutorImpl) (rel.cluster.getPlanner().getExecutor());
      RexImplicationChecker rexImplicationChecker = new RexImplicationChecker(
          rel.cluster.getRexBuilder(),
          rexImpl, rel.getRowType());

      if (rexImplicationChecker.implies(((MutableFilter) rel0).getCondition(),
          ((MutableFilter) rel).getCondition())) {
        RexNode newCondition;
        try {
          newCondition = transformRex(((MutableFilter) call.query).getCondition(),
              call.query.getRowType().getFieldList(),
              call.target.getRowType().getFieldList());
        } catch (MatchFailed e) {
          return null;
        }
        final MutableFilter newFilter = MutableFilter.of(call.target, newCondition);
        return call.result(newFilter);
      } else {
        return null;
      }
    }
  }

  private static RexNode transformRex(RexNode node,
                                      final List<RelDataTypeField> oldFields,
                                      final List<RelDataTypeField> newFields) {
    List<RexNode> nodes =
        transformRex(ImmutableList.of(node), oldFields, newFields);
    return nodes.get(0);
  }

  private static List<RexNode> transformRex(
      List<RexNode> nodes,
      final List<RelDataTypeField> oldFields,
      final List<RelDataTypeField> newFields) {
    RexShuttle shuttle = new RexShuttle() {
      @Override public RexNode visitInputRef(RexInputRef ref) {
        RelDataTypeField f = oldFields.get(ref.getIndex());
        for (int index = 0; index < newFields.size(); index++) {
          RelDataTypeField newf = newFields.get(index);
          if (f.getKey().equals(newf.getKey())
              && f.getValue() == newf.getValue()) {
            return new RexInputRef(index, f.getValue());
          }
        }
        throw MatchFailed.INSTANCE;
      }
    };
    return shuttle.apply(nodes);
  }
}

// End MaterializedViewSubstitutionVisitor.java
