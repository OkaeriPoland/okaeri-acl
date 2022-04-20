package eu.okaeri.acl.guard;

public enum GuardMode {

    BOOLEAN,
    ALLOW,
    DENY,
    ;

    public static GuardMode of(Guard guard) {

        if (guard.allow().length == 0 && guard.deny().length == 0) {
            return GuardMode.BOOLEAN;
        }

        if (guard.allow().length > 0 && guard.deny().length == 0) {
            return GuardMode.ALLOW;
        }

        if (guard.allow().length == 0) {
            return GuardMode.DENY;
        }

        throw new IllegalArgumentException("Unknown guard mode: " + guard);
    }
}
