package org.gentoo.java.ebuilder.maven;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * translate licenses from pom.xml to portage
 *
 * @author Zhang Zongyu
 */
public class MavenLicenses {

        /**
         * Location of the resource file mapping licenses.
         */
        private static final String licenseMapFile
                = "/licenseMap.properties";

        /**
         * the Map that will convert license from maven
         * to portage.
         */
        private Map<String, String> licenseMap;

        /**
         * Load cache from resource
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        MavenLicenses() {
            Properties mapProperty = new Properties();
            try {
                mapProperty.load(
                        this.getClass().getResourceAsStream(
                                licenseMapFile));
            } catch (final IOException ex) {
                throw new RuntimeException(
                        "Failed to read license map from resource", ex);
            }

            licenseMap = (Map)mapProperty;
        }

        /**
         * query the LicenseMap
         *
         * @param licenseName the licenses/license/name in pom.xml
         *
         * @return license identifier that works with Portage
         */
        public String getEquivalentLicense(String licenseName) {
            final String portageLicense =
                    licenseMap.get(licenseName.trim().
                                    replaceAll("[\n ]+", " ").
                                    toLowerCase());

            if (portageLicense == null) {
                return "!!!equivalentPortageLicenseName-not-found!!!";
            } else {
                return portageLicense;
            }
        }
}
