/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.se.checks;

import java.util.Collections;
import org.sonar.check.Rule;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.Flow;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2689")
public class ObjectOutputStreamCheck extends SECheck {

  private static final MethodMatchers FILES_NEW_OUTPUT_STREAM = MethodMatchers.create()
    .ofTypes("java.nio.file.Files")
    .names("newOutputStream")
    .withAnyParameters()
    .build();

  private static class FileOutputStreamAppendConstraint implements Constraint {
    private final Tree node;

    FileOutputStreamAppendConstraint(Tree node) {
      this.node = node;
    }
  }

  @Override
  public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
    ProgramState programState = context.getState();
    switch (syntaxNode.kind()) {
      case NEW_CLASS:
        NewClassTree newClassTree = (NewClassTree) syntaxNode;
        if (newClassTree.symbolType().is("java.io.FileOutputStream") && newClassTree.arguments().size() == 2) {
          ProgramState psBeforeInvocation = context.getNode().programState;
          BooleanConstraint argConstraint = psBeforeInvocation.getConstraint(psBeforeInvocation.peekValue(), BooleanConstraint.class);
          if (argConstraint == BooleanConstraint.TRUE) {
            return programState.addConstraint(programState.peekValue(), new FileOutputStreamAppendConstraint(newClassTree));
          }
        }
        return programState;
      case METHOD_INVOCATION:
        MethodInvocationTree mit = (MethodInvocationTree) syntaxNode;
        if (FILES_NEW_OUTPUT_STREAM.matches(mit)) {
          ProgramState psBeforeInvocation = context.getNode().programState;
          int optionsNumber = mit.arguments().size() - 1;
          for (int i = 0; i < optionsNumber; i++) {
            FileOutputStreamAppendConstraint argConstraint = psBeforeInvocation.getConstraint(psBeforeInvocation.peekValue(i), FileOutputStreamAppendConstraint.class);
            if (argConstraint != null) {
              return programState.addConstraint(programState.peekValue(), new FileOutputStreamAppendConstraint(mit));
            }
          }
        }
        return programState;
      case MEMBER_SELECT:
        return handleOpenOptionAppend(programState, ((MemberSelectExpressionTree) syntaxNode).identifier());
      case IDENTIFIER:
        return handleOpenOptionAppend(programState, (IdentifierTree) syntaxNode);
      default:
        return programState;
    }
  }

  private static ProgramState handleOpenOptionAppend(ProgramState programState, IdentifierTree identifier) {
    if (identifier.symbolType().is("java.nio.file.StandardOpenOption") && "APPEND".equals(identifier.name())) {
      return programState.addConstraint(programState.peekValue(), new FileOutputStreamAppendConstraint(identifier));
    }
    return programState;
  }

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    if (syntaxNode.is(Tree.Kind.NEW_CLASS)) {
      NewClassTree newClassTree = (NewClassTree) syntaxNode;
      if (newClassTree.symbolType().is("java.io.ObjectOutputStream") && newClassTree.arguments().size() == 1) {
        ProgramState programState = context.getState();
        FileOutputStreamAppendConstraint constraint = programState.getConstraint(programState.peekValue(), FileOutputStreamAppendConstraint.class);
        if (constraint != null) {
          context.reportIssue(
            newClassTree.arguments().get(0),
            this,
            "Do not use a FileOutputStream in append mode.",
            Collections.singleton(Flow.of(new JavaFileScannerContext.Location("FileOutputStream created here.", constraint.node))));
        }
      }
    }
    return super.checkPreStatement(context, syntaxNode);
  }

}
