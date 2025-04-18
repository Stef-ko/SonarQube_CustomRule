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
package org.sonar.java.reporting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class JavaQuickFix {
  private final String description;
  private final List<JavaTextEdit> textEdits;

  private JavaQuickFix(String description, List<JavaTextEdit> textEdits) {
    this.description = description;
    this.textEdits = textEdits;
  }

  public String getDescription() {
    return description;
  }

  public List<JavaTextEdit> getTextEdits() {
    return textEdits;
  }

  /**
   * See {@link org.sonar.api.batch.sensor.issue.fix.NewQuickFix#message(String) } for guidelines on format of the description.
   *
   * @param description a description for this quick fix
   * @return the builder for this quick fix
   */
  public static Builder newQuickFix(String description) {
    return new Builder(description);
  }

  /**
   * See {@link org.sonar.api.batch.sensor.issue.fix.NewQuickFix#message(String) } for guidelines on format of the description.
   *
   * @param description a description for this quick fix, following the {@link String#format(String, Object...)} formatting
   * @param args the arguments for the description
   * @return the builder for this quick fix
   */
  public static Builder newQuickFix(String description, Object... args) {
    return new Builder(String.format(description, args));
  }

  public static class Builder {
    private final String description;
    private final List<JavaTextEdit> textEdits = new ArrayList<>();

    private Builder(String description) {
      this.description = description;
    }

    public Builder addTextEdit(JavaTextEdit... textEdit) {
      textEdits.addAll(Arrays.asList(textEdit));
      return this;
    }

    public Builder addTextEdits(List<JavaTextEdit> textEdits) {
      this.textEdits.addAll(textEdits);
      return this;
    }

    public Builder reverseSortEdits() {
      textEdits.sort(new TreeSorter().reversed());
      return this;
    }

    public JavaQuickFix build() {
      return new JavaQuickFix(description, textEdits);
    }

    private static class TreeSorter implements Comparator<JavaTextEdit> {

      @Override
      public int compare(JavaTextEdit a, JavaTextEdit b) {
        AnalyzerMessage.TextSpan first = a.getTextSpan();
        AnalyzerMessage.TextSpan second = b.getTextSpan();

        int result = first.startLine - second.startLine;
        if (result != 0) {
          return result;
        }
        result = first.startCharacter - second.startCharacter;
        if (result != 0) {
          return result;
        }
        result = first.endLine - second.endLine;
        if (result != 0) {
          return result;
        }
        return first.endCharacter - second.endCharacter;
      }
    }
  }
}
