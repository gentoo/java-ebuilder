package org.gentoo.java.ebuilder.maven;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.gentoo.java.ebuilder.Config;
import org.gentoo.java.ebuilder.portage.CacheItem;
import org.gentoo.java.ebuilder.portage.MavenVersion;
import org.gentoo.java.ebuilder.portage.PortageParser;

/**
 * Cache for resolving maven artifacts into portage ebuilds.
 *
 * @author fordfrog
 */
public class MavenCache {

    /**
     * Cache containing map of group ids, artifact ids and corresponding cache
     * items.
     */
    private final Map<String, Map<String, List<CacheItem>>> cache
            = new HashMap<>();

    /**
     * Searches for system dependency using maven group id, artifact id and
     * version. First version that is the same or greater than specified version
     * is returned. In case there is no such version, the highest version from
     * the available ebuilds is returned.
     *
     * @param groupId    maven group id
     * @param artifactId maven artifact id
     * @param version    maven version
     *
     * @return dependency string or null
     */
    public String getDependency(final String groupId, final String artifactId,
            final String version) {
        final Map<String, List<CacheItem>> artifactIds = cache.get(groupId);

        if (artifactIds == null) {
            return "!!!groupId-not-found!!!";
        }

        final List<CacheItem> versions = artifactIds.get(artifactId);

        if (versions == null) {
            return "!!!artifactId-not-found!!!";
        }

        final MavenVersion mavenVersion = new MavenVersion(version);
        CacheItem cacheItem = null;

        for (final CacheItem curCacheItem : versions) {
            if (curCacheItem.getParsedMavenVersion().compareTo(mavenVersion)
                    >= 0) {
                cacheItem = curCacheItem;

                break;
            }
        }

        if (cacheItem == null) {
            return "!!!suitable-mavenVersion-not-found!!!";
        }

        final StringBuilder sbDependency = new StringBuilder(50);
        if (cacheItem.getCategory().compareTo("java-virtuals") != 0) {
            sbDependency.append(">=");
        }
        sbDependency.append(cacheItem.getCategory());
        sbDependency.append('/');
        sbDependency.append(cacheItem.getPkg());
        if (cacheItem.getCategory().compareTo("java-virtuals") != 0) {
            sbDependency.append('-');
            sbDependency.append(stripExtraFromVersion(cacheItem.getVersion()));
        }

        if (cacheItem.getUseFlag() != null) {
            sbDependency.append('[');
            sbDependency.append(cacheItem.getUseFlag());
            sbDependency.append(']');
        }

        sbDependency.append(':');
        sbDependency.append(cacheItem.getSlot());

        return sbDependency.toString();
    }

    /**
     * Loads cache from specified path.
     *
     * @param config application configuration
     */
    public void loadCache(final Config config) {
        config.getStdoutWriter().print("Reading in maven cache...");

        cache.clear();

        try (final BufferedReader reader = new BufferedReader(
                new FileReader(config.getCacheFile().toFile()))) {
            String line = reader.readLine();

            if ("1.0".equals(line)) {
                config.getStdoutWriter().print("(warning: format is not "
                        + "up-to-date, consider refreshing the cache)...");
            } else if (!PortageParser.CACHE_VERSION.equals(line)) {
                config.getErrorWriter().println("ERROR: Unsupported version of "
                        + "cache. Please refresh the cache using command line "
                        + "switch --refresh-cache.");
                Runtime.getRuntime().exit(1);
            }

            line = reader.readLine();

            while (line != null) {
                if (!line.isEmpty() && line.charAt(0) != '#') {
                    addCacheItem(new CacheItem(line));
                }

                line = reader.readLine();
            }
        } catch (final IOException ex) {
            throw new RuntimeException("Failed to load cache", ex);
        }

        for (final Map<String, List<CacheItem>> artifactIds : cache.values()) {
            for (final List<CacheItem> versions : artifactIds.values()) {
                versions.sort((final CacheItem o1, final CacheItem o2) -> {
                    return o1.getParsedMavenVersion().compareTo(
                            o2.getParsedMavenVersion());
                });
            }
        }

        config.getStdoutWriter().println("done");
    }

    /**
     * Adds cache item to the cache if it contains maven id.
     *
     * @param cacheItem cache item
     */
    private void addCacheItem(final CacheItem cacheItem) {
        if (cacheItem.getGroupId() == null) {
            return;
        }

        Map<String, List<CacheItem>> artifactIds
                = cache.get(cacheItem.getGroupId());

        if (artifactIds == null) {
            artifactIds = new HashMap<>();
            cache.put(cacheItem.getGroupId(), artifactIds);
        }

        List<CacheItem> versions = artifactIds.get(cacheItem.getArtifactId());

        if (versions == null) {
            versions = new ArrayList<>(10);
            artifactIds.put(cacheItem.getArtifactId(), versions);
        }

        versions.add(cacheItem);
    }

    /**
     * Strips all -r* from the version string.
     *
     * @param version version string
     *
     * @return stripped version string
     */
    private String stripExtraFromVersion(final String version) {
        return version.replaceAll("-r\\d+", "");
    }
}
