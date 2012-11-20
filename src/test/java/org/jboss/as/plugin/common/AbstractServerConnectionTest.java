/*
 * File: AbstractServerConnectionTest.java
 * Date: Nov 19, 2012
 *
 * Copyright (c) 2011 by Select Systems - All rights reserved.
 *
 * By:	Select Systems
 *		111 Heritage Way, Suite S
 *		Boalsburg, PA  16827
 *
 * Revision history:
 *
 * 
 */

package org.jboss.as.plugin.common;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;

import org.apache.maven.plugin.logging.Log;
import org.jboss.as.plugin.deployment.Deploy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

/**
 * @author stevemoyer
 *
 */
public class AbstractServerConnectionTest extends AbstractJbossMavenPluginMojoTestCase {

	Log log;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		super.setUp();
		log = mock(Log.class);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}
	
	/**
	 * Tests that if there is an <id> provided in the pom.xml file but no
	 * username and password, and there is no settings.xml file in the
	 * plugin's context, then the plugin falls back to prompting for
	 * credentials on the CLI.
	 */
	@Test
	public void testIdProvidedInPomButNoSettingsFile() {
		File pom = getTestFileAndVerify("src/test/resources/unit/common/id-provided-pom.xml");
		try {
			Deploy mojo = (Deploy) lookupMojoAndVerify("deploy", pom);
			mojo.setLog(log);
			mojo.getCallbackHandler();
			verify(log).debug(Deploy.DEBUG_MESSAGE_NO_SETTINGS_FILE);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Couldn't find \"deploy\" mojo");
		}
	}
	
	/**
	 * Tests that if there is an <id> provided in the pom.xml file but no
	 * username and password, and there is a settings.xml file that doesn't
	 * match the id of the server, then the plugin falls back to prompting
	 * for credentials on the CLI.
	 */
	@Test
	public void testIdProvidedInPomButNoServerSection() {
		File pom = getTestFileAndVerify("src/test/resources/unit/common/id-provided-pom.xml");
		File settings = getTestFileAndVerify("src/test/resources/unit/common/missing-id-settings.xml");
		try {
			Deploy mojo = (Deploy) lookupMojoVerifyAndApplySettings("deploy",  pom, settings);
			mojo.setLog(log);
			mojo.getCallbackHandler();
			verify(log).debug(Deploy.DEBUG_MESSAGE_NO_SERVER_SECTION);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Couldn't find \"deploy\" mojo");
		}
	}
	
	/**
	 * Tests that if there is an <id> provided in the pom.xml file but no
	 * username and password, and there is a settings.xml file that has a
	 * <server> section that matches the id, but there is no username and
	 * password specified, then the plugin falls back to prompting for
	 * credentials on the CLI.
	 */
	@Test
	public void testIdProvidedInPomButNoCredentials() {
		File pom = getTestFileAndVerify("src/test/resources/unit/common/id-provided-pom.xml");
		File settings = getTestFileAndVerify("src/test/resources/unit/common/id-provided-settings.xml");
		try {
			Deploy mojo = (Deploy) lookupMojoVerifyAndApplySettings("deploy",  pom, settings);
			mojo.setLog(log);
			mojo.getCallbackHandler();
			InOrder inOrder = inOrder(log);
			inOrder.verify(log).info(Deploy.DEBUG_MESSAGE_SETTINGS_HAS_ID);
			inOrder.verify(log).info(Deploy.DEBUG_MESSAGE_NO_CREDS);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Couldn't find \"deploy\" mojo");
		}
	}
	
	/**
	 * Test that if credentials are provided in the pom.xml file, they are used
	 * regardless of whether an <id> element is also present.
	 */
	@Test
	public void testCredentialsProvidedInPom() {
		File pom = getTestFileAndVerify("src/test/resources/unit/common/credentials-provided-pom.xml");
		try {
			Deploy mojo = (Deploy) lookupMojoAndVerify("deploy", pom);
			mojo.setLog(log);
			mojo.getCallbackHandler();
			verify(log).debug(Deploy.DEBUG_MESSAGE_POM_HAS_CREDS);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Couldn't find \"deploy\" mojo");
		}
	}

	/**
	 * Tests that if there is an <id> provided in the pom.xml file but no
	 * username and password, and there is a settings.xml file that has a
	 * <server> section that matches the id, and the section includes
	 * credentials, then they are used by the plugin.
	 */	
	@Test
	public void testCredentialsProvidedInSettings() {
		File pom = getTestFileAndVerify("src/test/resources/unit/common/id-provided-pom.xml");
		File settings = getTestFileAndVerify("src/test/resources/unit/common/credentials-provided-settings.xml");
		try {
			Deploy mojo = (Deploy) lookupMojoVerifyAndApplySettings("deploy",  pom, settings);
			mojo.setLog(log);
			mojo.getCallbackHandler();
			InOrder inOrder = inOrder(log);
			inOrder.verify(log).info(Deploy.DEBUG_MESSAGE_SETTINGS_HAS_ID);
			inOrder.verify(log).info(Deploy.DEBUG_MESSAGE_SETTINGS_HAS_CREDS);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Couldn't find \"deploy\" mojo");
		}
	}

	/**
	 * Test that if there is no <id> element and no credentials in the pom.xml
	 * file, then it falls back to prompting for them on the CLI. 
	 */
	@Test
	public void testNoCredentialsOrIdInPom() {
		File pom = getTestFileAndVerify("src/test/resources/unit/common/missing-id-pom.xml");
		try {
			Deploy mojo = (Deploy) lookupMojoAndVerify("deploy", pom);
			mojo.setLog(log);
			mojo.getCallbackHandler();
			verify(log).debug(Deploy.DEBUG_MESSAGE_NO_ID);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Couldn't find \"deploy\" mojo");
		}
	}
	
}
