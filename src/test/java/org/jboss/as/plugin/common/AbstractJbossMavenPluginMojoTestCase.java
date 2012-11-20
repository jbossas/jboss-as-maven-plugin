/*
 * File: MojoTestUtilities.java
 * Date: Aug 5, 2011
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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.DefaultSettingsReader;
import org.apache.maven.settings.io.SettingsParseException;
import org.apache.maven.settings.io.SettingsReader;
import org.junit.After;
import org.junit.Before;

/**
 * 
 * @author swm16 (swm16@psu.edu)
 */
public abstract class AbstractJbossMavenPluginMojoTestCase extends AbstractMojoTestCase {
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
	}
	
	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}
	
	/**
	 * Gets a settings.xml file from the input File and prepares it to be
	 * attached to a pom.xml
	 * 
	 * @param a file object pointing to the candidate settings file
	 * @return the settings object
	 * @throws IOException - if the settings file can't be read
	 */
	private Settings getSettingsFile(File userSettingsFile) throws IOException {
        Map<String, ?> options = Collections.singletonMap( SettingsReader.IS_STRICT, Boolean.TRUE );
		SettingsReader reader = new DefaultSettingsReader();
		
		Settings settings = null;
		try {
			settings = reader.read(userSettingsFile, options);
		} catch(SettingsParseException e) {
			
		}
		
		return settings;
	}
	
	/**
	 * Creates a File object from the fileName provided and verifies that it
	 * exists.
	 * 
	 * @param fileName the path of the test file
	 * @return a verified File object
	 */
	public File getTestFileAndVerify(String fileName) {
		File file = getTestFile(fileName);
		assertNotNull(file);
		assertTrue(file.exists());
		return file;
	}
	
	/**
	 * Looks up the specified mojo by name, passing it the POM file that
	 * references it, then verifying that the lookup was successful.
	 * 
	 * @param mojoName the name of the mojo being tested
	 * @param pomFile the pom.xml file to be used during testing
	 * @return the Mojo object under test
	 * @throws Exception if the mojo can not be found
	 */
	public Mojo lookupMojoAndVerify(String mojoName, File pomFile) throws Exception {
		Mojo mojo = lookupMojo(mojoName, pomFile);
		assertNotNull(mojo);
		return mojo;
	}
	
	/**
	 * Looks up the specified mojo by name, passing it the POM file that
	 * references it and a settings file that configures it, then verifying
	 * that the lookup was successful.
	 * 
	 * @param mojoName the name of the mojo being tested
	 * @param pomFile the pom.xml file to be used during testing
	 * @param settingsFile the settings.xml file to be used during testing
	 * @return the Mojo object under test
	 * @throws Exception if the mojo can not be found
	 */
	public Mojo lookupMojoVerifyAndApplySettings(String mojoName, File pomFile, File settingsFile) throws Exception {
		Mojo mojo = lookupMojo(mojoName, pomFile);
		assertNotNull(mojo);
		setVariableValueToObject(mojo, "settings", getSettingsFile(settingsFile));
		return mojo;
	}
	
}
