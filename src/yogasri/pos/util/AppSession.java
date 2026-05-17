package yogasri.pos.util;

import yogasri.pos.model.User;

public class AppSession {
    private static User currentUser;

    private AppSession() {}

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }
}

