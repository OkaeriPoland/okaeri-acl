package eu.okaeri.acl.guardian;

import lombok.Getter;

@Getter
public enum GuardianAction {

    ALLOW(true),
    DENY(false),
    ;

    private final boolean allow;

    GuardianAction(boolean allow) {
        this.allow = allow;
    }
}
