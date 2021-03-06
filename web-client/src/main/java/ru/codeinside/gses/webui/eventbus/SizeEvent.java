/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright (c) 2013, MPL CodeInside http://codeinside.ru
 */

package ru.codeinside.gses.webui.eventbus;

import java.util.EventObject;

final public class SizeEvent extends EventObject {
  private static final long serialVersionUID = 1L;

  final public String error;
  final public int size;

  public SizeEvent(Object source, String error, int size) {
    super(source);
    this.error = error;
    this.size = size;
  }

}
