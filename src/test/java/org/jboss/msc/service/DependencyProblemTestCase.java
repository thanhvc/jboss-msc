/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.msc.service;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

/**
 * @author Stuart Douglas
 */
public class DependencyProblemTestCase extends AbstractServiceTest {

    private volatile boolean serviceStarted = false;
    private volatile boolean fail = false;
    private final CountDownLatch latch = new CountDownLatch(2);
    private final ServiceName DEPENDEE = ServiceName.of("dependee");
    private final ServiceName DEPENDANT = ServiceName.of("dependant");


    private class TestListener extends AbstractServiceListener<Object> {

        @Override
        public void dependencyProblemCleared(ServiceController<? extends Object> serviceController) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {

            }
            if(serviceStarted) {
                fail = true;
            }
            latch.countDown();
        }
    }

    @Test
    public void testListenerOrdering() throws InterruptedException {
        this.serviceContainer.addListener(new TestListener());
        this.serviceContainer.addService(DEPENDANT, Service.NULL)
            .addDependency(DEPENDEE)
            .install();
        this.serviceContainer.addService(DEPENDEE, new DependeeService())
        .install();
        latch.await();
        if(fail) {
            throw new RuntimeException("dependee service started before dependent service had dependencyProblemCleared called");
        }
    }


    private class DependeeService implements Service<Object> {

        public DependeeService() {
        }

        public void start(StartContext context) throws StartException {
            serviceStarted = true;
            latch.countDown();
        }

        public void stop(StopContext context) {

        }

        public Object getValue() throws IllegalStateException, IllegalArgumentException {
            return this;
        }
    }

}
