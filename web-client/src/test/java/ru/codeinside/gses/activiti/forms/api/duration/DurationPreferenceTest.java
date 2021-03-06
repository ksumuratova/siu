/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright (c) 2014, MPL CodeInside http://codeinside.ru
 */

package ru.codeinside.gses.activiti.forms.api.duration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DurationPreferenceTest {

  @Test
  public void testParseTaskPeriods() throws Exception {
    DurationPreference preference = new DurationPreference();
    preference.parseTaskPreference("1/25/30");
    assertEquals(1, preference.inactiveInterval);
    assertEquals(25, preference.notificationInterval);
    assertEquals(30, preference.executionInterval);

    preference = new DurationPreference();
    preference.parseProcessPreference("1/25");
    assertEquals(0, preference.inactiveInterval); // в поле используется примити
    assertEquals(1, preference.notificationInterval);
    assertEquals(25, preference.executionInterval);
  }

  @Test(expected = IllegalDurationExpression.class)
  public void testParseEmptyExpression() throws Exception {
    DurationPreference preference = new DurationPreference();
    preference.parseTaskPreference("");
  }

  @Test(expected = IllegalDurationExpression.class)
  public void testParseWithAlphaExpression() throws Exception {
    DurationPreference preference = new DurationPreference();
    preference.parseTaskPreference("1/qwe/12");
  }

  @Test(expected = IllegalDurationExpression.class)
  public void testParseEmptyExpressionForProcess() throws Exception {
    DurationPreference preference = new DurationPreference();
    preference.parseProcessPreference("");
  }

  @Test(expected = IllegalDurationExpression.class)
  public void testParseWithAlphaExpressionForProcess() throws Exception {
    DurationPreference preference = new DurationPreference();
    preference.parseProcessPreference("1/qwe/12");
  }
}