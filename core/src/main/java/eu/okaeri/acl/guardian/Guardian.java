package eu.okaeri.acl.guardian;

import eu.okaeri.acl.guard.Guard;
import eu.okaeri.acl.guard.GuardMode;
import lombok.Data;
import lombok.NonNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Data
public class Guardian {

    private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("okaeri.platform.debug", "false"));
    private static final Logger LOGGER = Logger.getLogger(Guardian.class.getSimpleName());

    protected GuardianAction defaultAction = GuardianAction.ALLOW;
    protected boolean messageVariables = true;

    public Guardian defaultAction(GuardianAction defaultAction) {
        this.defaultAction = defaultAction;
        return this;
    }

    public Guardian messageVariables(boolean messageVariables) {
        this.messageVariables = messageVariables;
        return this;
    }

    public String evaluate(@NonNull Guard guard, @NonNull GuardianContext context) {
        return "true";
    }

    private String _evaluate(@NonNull Guard guard, @NonNull GuardianContext context) {

        long start = System.nanoTime();
        String result = this.evaluate(guard, context);

        if (DEBUG) {
            long took = System.nanoTime() - start;
            long tookMs = took / 1000L / 1000L;
            LOGGER.info(guard + " was evaluated to '" + result + "' in " + tookMs + "ms/" + took + "ns with " + context);
        }

        return result;
    }

    public boolean allows(Guard guard, String result) {
        switch (GuardMode.of(guard)) {
            case BOOLEAN:
                return "true".equals(result);
            case ALLOW:
                return Arrays.asList(guard.allow()).contains(result);
            case DENY:
                return !Arrays.asList(guard.deny()).contains(result);
            default:
                throw new IllegalArgumentException("Unsupported guard mode: " + guard);
        }
    }

    public boolean allows(@NonNull Guard guard, @NonNull GuardianContext context) {
        return this.allows(guard, this._evaluate(guard, context));
    }

    public List<GuardianViolation> inspect(@NonNull Guard guard, @NonNull GuardianContext context) {

        String result = this._evaluate(guard, context);
        boolean allows = this.allows(guard, result);

        if (allows) {
            return Collections.emptyList();
        }

        String error = guard.message();
        if (this.messageVariables) {
            error = error.replace("{mode}", GuardMode.of(guard).name());
            error = error.replace("{value}", guard.value());
            error = error.replace("{allow}", Arrays.toString(guard.allow()));
            error = error.replace("{deny}", Arrays.toString(guard.deny()));
        }

        return Collections.singletonList(new GuardianViolation(
            result,
            error,
            new LinkedHashSet<>(Arrays.asList(guard.allow())),
            new LinkedHashSet<>(Arrays.asList(guard.deny())),
            GuardianAction.DENY
        ));
    }

    public boolean allows(@NonNull Guard[] guards, @NonNull GuardianContext context) {

        if (guards.length == 0) {
            return this.getDefaultAction().isAllow();
        }

        return Arrays.stream(guards).allMatch(guard -> this.allows(guard, context));
    }

    public List<GuardianViolation> inspect(@NonNull Guard[] guards, @NonNull GuardianContext context) {
        return Arrays.stream(guards)
            .flatMap(guard -> this.inspect(guard, context).stream())
            .collect(Collectors.toList());
    }

    public boolean allows(@NonNull Method method, @NonNull GuardianContext context) {
        return this.allows(method.getAnnotationsByType(Guard.class), context);
    }

    public List<GuardianViolation> inspect(@NonNull Method method, @NonNull GuardianContext context) {
        return this.inspect(method.getAnnotationsByType(Guard.class), context);
    }

    public boolean allows(@NonNull Class<?> clazz, @NonNull GuardianContext context) {
        return this.allows(clazz.getAnnotationsByType(Guard.class), context);
    }

    public List<GuardianViolation> inspect(@NonNull Class<?> clazz, @NonNull GuardianContext context) {
        return this.inspect(clazz.getAnnotationsByType(Guard.class), context);
    }
}
