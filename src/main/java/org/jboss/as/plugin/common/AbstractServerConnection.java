/*
 * JBoss, Home of Professional Open Source. Copyright 2010, Red Hat, Inc., and
 * individual contributors as indicated by the @author tags. See the
 * copyright.txt file in the distribution for a full listing of individual
 * contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package org.jboss.as.plugin.common;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import javax.security.auth.callback.CallbackHandler;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * The default implementation for connecting to a running AS7 instance
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Stuart Douglas
 */
public abstract class AbstractServerConnection extends AbstractMojo implements ConnectionInfo, Closeable {

    public static final String DEBUG_MESSAGE_NO_CREDS = "No username and password in settings.xml file - falling back to CLI entry";
    public static final String DEBUG_MESSAGE_NO_ID = "No <id> element was found in the POM - Getting credentials from CLI entry";
    public static final String DEBUG_MESSAGE_NO_SERVER_SECTION = "No <server> section was found for the specified id";
    public static final String DEBUG_MESSAGE_NO_SETTINGS_FILE = "No settings.xml file was found in this Mojo's execution context";
    public static final String DEBUG_MESSAGE_POM_HAS_CREDS = "Getting credentials from the POM";
    public static final String DEBUG_MESSAGE_SETTINGS_HAS_CREDS = "Found username and password in the settings.xml file";
    public static final String DEBUG_MESSAGE_SETTINGS_HAS_ID = "Found the server's id in the settings.xml file";

    protected static final Object CLIENT_LOCK = new Object();

    private volatile InetAddress address = null;

    private volatile CallbackHandler handler;

    /**
     * Specifies the host name of the server where the deployment plan should be executed.
     */
    @Parameter(defaultValue = "localhost", property = "jboss-as.hostname")
    private String hostname;

    /**
     * Execute this Mojo if the given address exist.
     */
    @Parameter(alias = "if-exists", property = "jboss-as.ifExists")
    private String ifExists;

    /**
     * Execute this Mojo if the given addres does not exist.
     */
    @Parameter(alias = "if-not-exists", property = "jboss-as.ifNotExists")
    private String ifNotExists;

    /**
     * Specifies the port number the server is listening on.
     */
    @Parameter(defaultValue = "9999", property = "jboss-as.port")
    private int port;

   /**
     * Specifies the id of the server if the username and password is to be
     * retrieved from the settings.xml file
     */
    @Parameter(property = "jboss-as.id")
    private String id;

    /**
     * Provides a reference to the settings file.
     */
    @Parameter(property = "settings", readonly = true, required = true, defaultValue = "${settings}")
    private Settings settings;

    /**
     * Specifies the username to use if prompted to authenticate by the server.
     * <p/>
     * If no username is specified and the server requests authentication the user will be prompted to supply the
     * username,
     */
    @Parameter(property = "jboss-as.username")
    private String username;

    /**
     * Specifies the password to use if prompted to authenticate by the server.
     * <p/>
     * If no password is specified and the server requests authentication the user will be prompted to supply the
     * password,
     */
    @Parameter(property = "jboss-as.password")
    private String password;

    private ModelControllerClient client;

    /**
     * The hostname to deploy the archive to. The default is localhost.
     *
     * @return the hostname of the server.
     */
    public final String hostname() {

        return hostname;
    }

    /**
     * The port number of the server to deploy to. The default is 9999.
     *
     * @return the port number to deploy to.
     */
    @Override
    public final int getPort() {

        return port;
    }

    /**
     * Returns {@code true} if the connection is for a domain server, otherwise {@code false}.
     *
     * @return {@code true} if the connection is for a domain server, otherwise {@code false}
     */
    public final boolean isDomainServer() {

        synchronized (CLIENT_LOCK) {
            return isDomainServer(getClient());
        }
    }

    /**
     * The goal of the deployment.
     *
     * @return the goal of the deployment.
     */
    public abstract String goal();

    /**
     * Gets or creates a new connection to the server and returns the client.
     * <p/>
     * For a domain server a {@link DomainClient} will be returned.
     *
     * @return the client
     */
    public final ModelControllerClient getClient() {

        synchronized (CLIENT_LOCK) {
            ModelControllerClient result = client;
            if (result == null) {
                result = client = ModelControllerClient.Factory.create(getHostAddress(), getPort(),
                                                                       getCallbackHandler());
                if (isDomainServer(result)) {
                    result = client = DomainClient.Factory.create(result);
                }
            }
            return result;
        }
    }

    @Override
    public final void close() {

        synchronized (CLIENT_LOCK) {
            Streams.safeClose(client);
            client = null;
        }
    }

    /**
     * Creates gets the address to the host name.
     *
     * @return the address.
     */
    @Override
    public synchronized final InetAddress getHostAddress() {

        InetAddress result = address;
        // Lazy load the address
        if (result == null) {
            try {
                result = address = InetAddress.getByName(hostname());
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException(String.format("Host name '%s' is invalid.", hostname), e);
            }
        }
        return result;
    }

    @Override
    public synchronized final CallbackHandler getCallbackHandler() {

        CallbackHandler result = handler;
        if (result == null) {
            if(username == null && password == null) {
                if(id != null) {
                    getCredentialsFromSettings();
                } else {
                    getLog().debug(DEBUG_MESSAGE_NO_ID);
                }
            } else {
                getLog().debug(DEBUG_MESSAGE_POM_HAS_CREDS);
            }
            result = handler = new ClientCallbackHandler(username, password);
        }
        return result;
    }

    private void getCredentialsFromSettings() {
        if(settings != null) {
            Server server = settings.getServer(id);
            if(server != null) {
                getLog().debug(DEBUG_MESSAGE_SETTINGS_HAS_ID);
                password = server.getPassword();
                username = server.getUsername();
                if(username != null && password != null) {
                    getLog().debug(DEBUG_MESSAGE_SETTINGS_HAS_CREDS);
                } else {
                    getLog().debug(DEBUG_MESSAGE_NO_CREDS);
                }
            } else {
                getLog().debug(DEBUG_MESSAGE_NO_SERVER_SECTION);
            }
        } else {
            getLog().debug(DEBUG_MESSAGE_NO_SETTINGS_FILE);
        }
    }

    private boolean isDomainServer(final ModelControllerClient client) {

        boolean result = false;
        // Check this is really a domain server
        final ModelNode op = Operations.createReadAttributeOperation(Operations.LAUNCH_TYPE);
        try {
            final ModelNode opResult = client.execute(op);
            if (Operations.successful(opResult)) {
                result = ("DOMAIN".equals(Operations.readResultAsString(opResult)));
            }
        } catch (IOException e) {
            if ( getLog().isDebugEnabled() )
                getLog().debug(e);
            throw new IllegalStateException(String.format("I/O Error could not execute operation '%s'", op), e);
        }
        return result;
    }

    protected boolean checkPreconditions() throws IOException {

        if (ifExists != null || ifNotExists != null) {

            if (ifExists != null && ifNotExists != null) {
                throw new IllegalStateException("Cannot use ifExists together with ifNotExists. Usage is exclusive " +
                                                        "or do not use any of the fields.");
            }

            if (isDomainServer()) {
                getLog().warn("ifExists/ifNotExists currently not supported in domain mode.");
                return true;
            }
            if (ifExists != null) {
                getLog().debug(String.format("Check if exists: '%s'", ifExists));
                ModelNode address = Operations.parseAddress(null, ifExists);
                if (resourceExists(address, client)) {
                    return true;
                }
                return false;
            }

            if (ifNotExists != null) {
                getLog().debug(String.format("Check if not exists: '%s'", ifNotExists));
                ModelNode address = Operations.parseAddress(null, ifNotExists);
                if (!resourceExists(address, client)) {
                    return true;
                }
                return false;
            }
        }

        return true;
    }

    /**
     * Checks the existence of a resource. If the resource exists, {@code true} is returned, otherwise {@code false}.
     *
     * @param address the address of the resource to check.
     * @param client the client used to execute the operation.
     * @return {@code true} if the resources exists, otherwise {@code false}.
     * @throws IOException if an error occurs executing the operation.
     * @throws RuntimeException if the operation fails.
     */
    protected boolean resourceExists(final ModelNode address, final ModelControllerClient client) throws
            IOException {

        final Property childAddress = Operations.getChildAddress(address);
        final ModelNode parentAddress = Operations.getParentAddress(address);
        final ModelNode r = client.execute(Operations.createOperation(Operations.READ_RESOURCE, parentAddress, false));
        reportFailure(r);
        boolean found = false;
        final String name = childAddress.getName();
        int childCount = 0;
        if (r.get(Operations.RESULT).get(name).isDefined()) {
            List<ModelNode> nodes = r.get(Operations.RESULT).get(name).asList();
            childCount = nodes.size();
            for (ModelNode dataSource : nodes) {
                if (dataSource.asProperty().getName().equals(childAddress.getValue().asString())) {
                    found = true;
                }
            }
        }

        getLog().debug(String.format("Check for Resource '%s' result '%b', node count %d", address.toString(), found,
                                     childCount));

        return found;
    }

    protected void reportFailure(final ModelNode result) {

        if (!Operations.successful(result)) {
            throw new RuntimeException(Operations.getFailureDescription(result));
        }
    }
}
