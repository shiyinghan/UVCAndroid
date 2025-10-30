package com.herohan.uvcdemo.cases;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.rule.ActivityTestRule;

import com.herohan.uvcdemo.EntryActivity;
import com.herohan.uvcdemo.R;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;

public class CommonTest {
    private static final String TAG = CommonTest.class.getSimpleName();

    /**
     * Use {@link ActivityScenario} to create and launch the activity under test. This is a
     * replacement for {@link ActivityTestRule}.
     */
    @Rule
    public ActivityTestRule<EntryActivity> activityTestRule =
            new ActivityTestRule<EntryActivity>(EntryActivity.class);

    @Test
    public void testMemoryLeak() throws InterruptedException {
        Thread.sleep(1000);

        for (int i = 0; i < 200; i++) {
            onView(withId(R.id.btnRecordVideo)).perform(scrollTo(), click());

            // Waiting for USB camera to finish loading (4s)
            Thread.sleep(4000 + Math.round(Math.random() * 500));

            // Click the "Start Recording" button
            onView(withId(R.id.bthCaptureVideo)).perform(click());

            // Record video (4s)
            Thread.sleep(4000);

            // Return to entry activity
            Espresso.pressBack();
        }
    }

    @After
    public void onAfter() {
        getAllThread();
    }

    private void getAllThread() {
        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        Log.d(TAG, "Thread Count: " + allStackTraces.size());
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] stackTraceElements = entry.getValue();

            Log.d(TAG, "Thread Name: " + thread.getName());
            Log.d(TAG, "Thread ID: " + thread.getId());
            Log.d(TAG, "Thread State: " + thread.getState());

            if (stackTraceElements != null) {
                for (StackTraceElement element : stackTraceElements) {
                    Log.d(TAG, "\t" + element.toString());
                }
            }
        }
    }
}
