/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright (c) 2013, MPL CodeInside http://codeinside.ru
 */

package ru.codeinside.gses.liquibase.impl;

import liquibase.servicelocator.ServiceLocator;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import ru.codeinside.gses.liquibase.api.MigrationService;

final public class Activator implements BundleActivator {

    ServiceRegistration registration;

    @Override
    public void start(final BundleContext context) throws Exception {
        ServiceLocator.initialize(context.getBundle());
        registration = context.registerService(MigrationService.class.getName(), new MigrationServiceImpl(), null);
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        if (registration != null) {
            registration.unregister();
        }
    }
}
