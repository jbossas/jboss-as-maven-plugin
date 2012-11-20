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
 * @author stevemoyer
 *
 */
public abstract class AbstractJbossMavenPluginMojoTestCase extends AbstractMojoTestCase {
	
	@Before
	public void setUp() throws Exception {

	}
	
	@After
	public void tearDown() throws Exception {

	}
	
	private Settings getSettings(File userSettingsFile) throws IOException {
        Map<String, ?> options = Collections.singletonMap( SettingsReader.IS_STRICT, Boolean.TRUE );
		SettingsReader reader = new DefaultSettingsReader();
		
		Settings settings = null;
		try {
			settings = reader.read(userSettingsFile, options);
		} catch(SettingsParseException e) {
			
		}
		
		return settings;
	}
	
	public File getTestFileAndVerify(String fileName) {
		File pom = getTestFile(fileName);
		assertNotNull(pom);
		assertTrue(pom.exists());
		return pom;
	}
	
	public Mojo lookupMojoAndVerify(String mojoName, File pomFile) throws Exception {
		Mojo mojo = lookupMojo(mojoName, pomFile);
		assertNotNull(mojo);
		return mojo;
	}
	
	public Mojo lookupMojoVerifyAndApplySettings(String mojoName, File pomFile, File settingsFile) throws Exception {
		Mojo mojo = lookupMojo(mojoName, pomFile);
		assertNotNull(mojo);
		setVariableValueToObject(mojo, "settings", getSettings(settingsFile));
		return mojo;
	}
	
}
