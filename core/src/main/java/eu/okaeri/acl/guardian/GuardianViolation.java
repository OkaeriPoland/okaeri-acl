package eu.okaeri.acl.guardian;

import lombok.Data;

import java.util.Set;

@Data
public class GuardianViolation {
    public final String value;
    private final String message;
    private final Set<String> allow;
    private final Set<String> deny;
    public final GuardianAction action;
}
