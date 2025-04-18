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
package org.sonar.java.checks.helpers;

import javax.annotation.Nullable;

/** Represents the three possible values in three-valued logic. */
public enum TernaryValue {
  TRUE, FALSE, UNKNOWN;

  public static TernaryValue of(boolean b) {
    return b ? TRUE : FALSE;
  }

  public static TernaryValue of(@Nullable Boolean b) {
    return b == null ? UNKNOWN : of(b.booleanValue());
  }

  public boolean maybeTrue() {
    return this != FALSE;
  }
}
