package dev.ynki.protect;

import dev.ynki.manager.Manager;

public class NativeHelper {
    public static void setProfile() {
        Manager.USER_PROFILE = new UserProfile(
                "ynkixd1337",
                "Deleoper",
                "09.11.2077"
        );
    }
}