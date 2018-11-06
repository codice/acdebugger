package org.codice.acdebugger.cli;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import picocli.CommandLine.IVersionProvider;

/**
 * Implements a {@link IVersionProvider} that reads version information from a version.properties
 * file in a jar Adapted from the picocli example:
 * https://github.com/remkop/picocli/blob/master/examples/src/main/java/picocli/examples/VersionProviderDemo1.java
 *
 * <p>Supports the following version properties:
 *
 * <ul>
 *   <li>Application-Title
 *   <li>Application-Version
 * </ul>
 */
public class PropertiesVersionProvider implements IVersionProvider {

  private static final String TITLE_PROPERTY_NAME = "application.title";
  private static final String VERSION_PROPERTY_NAME = "application.version";

  @Override
  public String[] getVersion() throws Exception {
    final URL url = getClass().getResource("/version.properties");

    if (url == null) {
      return new String[] {"No version.properties file found in the classpath."};
    }
    final Properties properties = new Properties();

    try (final InputStream is = url.openStream()) {
      properties.load(is);
    }
    return new String[] {
      properties.getProperty(TITLE_PROPERTY_NAME)
          + " version: \""
          + properties.getProperty(VERSION_PROPERTY_NAME)
          + "\""
    };
  }
}
