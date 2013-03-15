/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.as.plugin.server;

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.jboss.jdf.stacks.model.Runtime;
import org.junit.Test;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 15.03.13 10:01
 */
public class RunTest extends AbstractMojoTestCase {

    @Test
    public void testGetRuntime() throws Exception {

        File pom = getTestFile("src/test/resources/unit/common/run-pom.xml");
        Run runMojo = (Run) lookupMojo("run", pom);
        Runtime runtime = runMojo.getRuntime();

        assertNotNull(runtime);
        assertEquals("7.1.0.Final", runtime.getVersion());
    }

    @Test
    public void testGetRuntimeInvalidVersion() throws Exception {

        File pom = getTestFile("src/test/resources/unit/common/run-invalid-version-pom.xml");
        Run runMojo = (Run) lookupMojo("run", pom);
        Runtime runtime = runMojo.getRuntime();

        assertNull(runtime);
    }
}
