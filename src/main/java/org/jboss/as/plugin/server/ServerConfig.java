/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
import java.util.List;

import org.jboss.as.plugin.common.ConnectionInfo;
import org.jboss.as.plugin.common.Files;
import org.jboss.as.plugin.server.Arguments.Argument;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class ServerConfig {
    private static final String SERVER_BASE_DIR = "jboss.server.base.dir";
    private static final String SERVER_CONFIG_DIR = "jboss.server.config.dir";
    private static final String SERVER_LOG_DIR = "jboss.server.log.dir";

    private final ConnectionInfo connectionInfo;
    private final File jbossHome;
    private String modulesDir;
    private String bundlesDir;
    private final Arguments jvmArgs;
    private String javaHome;
    private String serverConfig;
    private String propertiesFile;
    private final Arguments serverArgs;
    private long startupTimeout;
    private String baseDir;
    private String configDir;
    private String logDir;

    ServerConfig(final ConnectionInfo connectionInfo, final File jbossHome) {
        this.connectionInfo = connectionInfo;
        this.jbossHome = jbossHome;
        jvmArgs = new Arguments();
        serverArgs = new Arguments();
        startupTimeout = 60L;
    }

    public static ServerConfig of(final ConnectionInfo connectionInfo, final File jbossHome) {
        return new ServerConfig(connectionInfo, jbossHome);
    }

    public ConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }

    public File getJbossHome() {
        return jbossHome;
    }

    public String getModulesDir() {
        return modulesDir == null ? Files.createPath(jbossHome, "modules") : modulesDir;
    }

    public ServerConfig setModulesDir(final String modulesDir) {
        this.modulesDir = modulesDir;
        return this;
    }

    public String getBundlesDir() {
        return bundlesDir == null ? Files.createPath(jbossHome, "bundles") : bundlesDir;
    }

    public ServerConfig setBundlesDir(final String bundlesDir) {
        this.bundlesDir = bundlesDir;
        return this;
    }

    public List<String> getJvmArgs() {
        return jvmArgs.asList();
    }

    public ServerConfig addJvmArg(final String arg) {
        final Argument argument = Arguments.parse(arg);
        if (SERVER_BASE_DIR.equals(argument.getKey())) {
            baseDir = argument.getValue();
        } else if (SERVER_CONFIG_DIR.equals(argument.getKey())) {
            configDir = argument.getValue();
        } else if (SERVER_LOG_DIR.equals(argument.getKey())) {
            logDir = argument.getValue();
        }
        jvmArgs.add(argument);
        return this;
    }

    public ServerConfig setJvmArgs(final String... jvmArgs) {
        this.jvmArgs.clear();
        for (String arg : jvmArgs) {
            addJvmArg(arg);
        }
        return this;
    }

    public String getJavaHome() {
        return javaHome;
    }

    public ServerConfig setJavaHome(final String javaHome) {
        this.javaHome = javaHome;
        return this;
    }

    public String getServerConfig() {
        return serverConfig;
    }

    public ServerConfig setServerConfig(final String serverConfig) {
        this.serverConfig = serverConfig;
        return this;
    }

    public String getPropertiesFile() {
        return propertiesFile;
    }

    public ServerConfig setPropertiesFile(final String propertiesFile) {
        this.propertiesFile = propertiesFile;
        return this;
    }

    public List<String> getServerArgs() {
        return serverArgs.asList();
    }

    public ServerConfig addServerArg(final String arg) {
        final Argument argument = Arguments.parse(arg);
        if (SERVER_BASE_DIR.equals(argument.getKey())) {
            baseDir = argument.getValue();
        } else if (SERVER_CONFIG_DIR.equals(argument.getKey())) {
            configDir = argument.getValue();
        } else if (SERVER_LOG_DIR.equals(argument.getKey())) {
            logDir = argument.getValue();
        }
        serverArgs.add(argument);
        return this;
    }

    public ServerConfig setServerArgs(final String... serverArgs) {
        this.serverArgs.clear();
        for (String arg : serverArgs) {
            addServerArg(arg);
        }
        return this;
    }

    public long getStartupTimeout() {
        return startupTimeout;
    }

    public ServerConfig setStartupTimeout(final long startupTimeout) {
        this.startupTimeout = startupTimeout;
        return this;
    }

    public String getBaseDir() {
        return baseDir == null ? Files.createPath(jbossHome, "standalone") : baseDir;
    }

    public String getConfigDir() {
        return configDir == null ? Files.createPath(getBaseDir(), "configuration") : configDir;
    }

    public String getLogDir() {
        return logDir == null ? Files.createPath(getBaseDir(), "log") : logDir;
    }
}
