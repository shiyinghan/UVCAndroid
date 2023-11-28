package com.herohan.uvcapp.utils;

import android.util.Log;

import java.io.PrintWriter;
import java.util.List;

/**
 * Class to give diagnostic messages for Watchdogs.
 */
class WatchdogDiagnostics {
    private static String getBlockedOnString(Object blockedOn) {
        return String.format("- waiting to lock <0x%08x> (a %s)",
                System.identityHashCode(blockedOn), blockedOn.getClass().getName());
    }

    private static String getLockedString(Object heldLock) {
        return String.format("- locked <0x%08x> (a %s)", System.identityHashCode(heldLock),
                heldLock.getClass().getName());
    }

    public static boolean diagnoseCheckers(final List<Watchdog.HandlerChecker> blockedCheckers) {
        boolean hasStackTrace = false;
        for (int i = 0; i < blockedCheckers.size(); i++) {
            Thread blockedThread = blockedCheckers.get(i).getThread();

            // Fall back to "regular" stack trace, if necessary.
            Log.w(Watchdog.TAG, blockedThread.getName() + " stack trace:");
            StackTraceElement[] stackTrace = blockedThread.getStackTrace();
            for (StackTraceElement element : stackTrace) {
                Log.w(Watchdog.TAG, "    at " + element);
            }
            if (stackTrace.length > 0) {
                hasStackTrace = true;
            }
        }
        return hasStackTrace;
    }
}
