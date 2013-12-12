/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.TimeUnit;

import org.codehaus.plexus.classworlds.realm.ClassRealm;

/**
 * Security actions to perform possibly privileged operations. No methods in this class are to be made public under any
 * circumstances!
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
final class SecurityActions {

    /**
     * Bad hack to respawn the plugin's class loader when Maven is already shut down.
     */
    static void respawnCurrentClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (!(classLoader instanceof ClassRealm)) {
            // It seems there is nothing we can do if the current class loader is not a ClassRealm.
            return;
        }

        // Modify the current class loader so that classes that was not loaded during the Maven build will be loaded by the new class loader.
        final ClassRealm classRealm = (ClassRealm) classLoader;
        ClassLoader newClassLoader = new URLClassLoader(classRealm.getURLs(), classRealm.getParentClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                // We should first check whether the requested class was already loaded by the old class loader in order not to break compatibility with already loaded classes.
                Class<?> clazz = classRealm.loadClassFromSelf(name);
                return clazz != null ? clazz : super.loadClass(name, resolve);
            }
        };
        classRealm.setParentClassLoader(newClassLoader);
    }

    static void registerShutdown(final Server server) {
        final Thread hook = new Thread(new Runnable() {
            @Override
            public void run() {
                respawnCurrentClassLoader();

                server.stop();
                // Bad hack to get maven to complete it's message output
                try {
                    TimeUnit.MILLISECONDS.sleep(500L);
                } catch (InterruptedException ignore) {
                    // no-op
                }
            }
        });
        hook.setDaemon(true);
        addShutdownHook(hook);
    }
    static void addShutdownHook(final Thread hook) {
        if (System.getSecurityManager() == null) {
            Runtime.getRuntime().addShutdownHook(hook);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    Runtime.getRuntime().addShutdownHook(hook);
                    return null;
                }
            });
        }
    }

    static String getEnvironmentVariable(final String key) {
        if (System.getSecurityManager() == null) {
            return System.getenv(key);
        }
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getenv(key);
            }
        });
    }
}
