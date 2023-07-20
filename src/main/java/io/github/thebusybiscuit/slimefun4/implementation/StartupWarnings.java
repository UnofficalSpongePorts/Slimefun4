package io.github.thebusybiscuit.slimefun4.implementation;


import io.github.thebusybiscuit.slimefun4.utils.NumberUtils;
import org.apache.logging.log4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * This class stores some startup warnings we occasionally need to print.
 * If you setup your server the recommended way, you are never going to see
 * any of these messages.
 *
 * @author TheBusyBiscuit
 */
final class StartupWarnings {

    private static final String BORDER = "****************************************************";
    private static final String PREFIX = "* ";

    private StartupWarnings() {
    }

    @ParametersAreNonnullByDefault
    static void oldJavaVersion(Logger logger, int recommendedJavaVersion) {
        int javaVersion = NumberUtils.getJavaVersion();

        logger.warn(BORDER);
        logger.warn(PREFIX + "Your Java version (Java {0}) is out of date.", javaVersion);
        logger.warn(PREFIX);
        logger.warn(PREFIX + "We recommend you to update to Java {0}.", recommendedJavaVersion);
        logger.warn(PREFIX + "Java {0} is required for newer versions of Minecraft", recommendedJavaVersion);
        logger.warn(PREFIX + "and we would like to utilise all the new features");
        logger.warn(PREFIX + "Slimefun will also require Java {0} in", recommendedJavaVersion);
        logger.warn(PREFIX + "the foreseeable future, so please update!");
        logger.warn(BORDER);
    }

}
