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
package org.sonar.java.cfg;

import java.io.File;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.LambdaExpressionTreeImpl;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;

public class CFGTestUtils {

  public static CFG buildCFG(String methodCode) {
    return buildCFGFromCUT(JParserTestUtils.parse("class A { " + methodCode + " }"));
  }

  public static CFG buildCFGFromLambda(String functionalInterfaceImpl) {
    var cut = JParserTestUtils.parse("class A { " + functionalInterfaceImpl + " }");
    VariableTreeImpl variable = (VariableTreeImpl) ((ClassTree) cut.types().get(0)).members().get(0);
    return (CFG) ((LambdaExpressionTreeImpl) variable.initializer()).cfg();
  }

  public static CFG buildCFG(File file) {
    return buildCFGFromCUT(JParserTestUtils.parse(file));
  }

  private static CFG buildCFGFromCUT(CompilationUnitTree cut) {
    MethodTree tree = ((MethodTree) ((ClassTree) cut.types().get(0)).members().get(0));
    return (CFG) tree.cfg();
  }

}
