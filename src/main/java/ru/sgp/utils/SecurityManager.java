package ru.sgp.utils;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityManager {

    static final String sgpPrefix = "sgp\\";

    public static boolean isDebugging() {
        return java.lang.management.ManagementFactory.getRuntimeMXBean().
                getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;
    }

    public static String getCurrentUser() {
        String currentUserName = null;
        boolean anonymous = true;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication != null) {
            currentUserName = authentication.getName();
            anonymous = authentication instanceof AnonymousAuthenticationToken;

            if(!currentUserName.toLowerCase().startsWith(sgpPrefix)) {
                currentUserName = sgpPrefix + currentUserName;
            }

            currentUserName = currentUserName.replaceAll("(?i)@SGP.RU", "");
            currentUserName = currentUserName.replaceAll("(?i)@SGP1.RU", "");
        }

        if(currentUserName == null || anonymous) {
            if(isDebugging()) {
                currentUserName = sgpPrefix + "SirenkoNiV";
            } else {
                return null;
            }
        }

        return currentUserName;
    }
}
