package eu.okaeri.acl.testgroovyguardian;

import eu.okaeri.acl.groovy.GroovyGuardian;
import eu.okaeri.acl.guard.Guard;
import eu.okaeri.acl.guardian.Guardian;
import eu.okaeri.acl.guardian.GuardianAction;
import eu.okaeri.acl.guardian.GuardianContext;
import eu.okaeri.acl.guardian.GuardianViolation;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestGroovyGuardian {

    private final Guardian guardian = GroovyGuardian.create();

    @Test
    @SneakyThrows
    public void should_allow_no_guards() {

        GuardianContext context = GuardianContext.of().with("key", "value");
        Method method = EmptyType.class.getMethod("doNothing");

        assertTrue(this.guardian.allows(method, context));
        assertTrue(this.guardian.inspect(method, context).isEmpty());
    }

    @Test
    @SneakyThrows
    public void should_allow_in_mode_boolean() {

        GuardianContext context = GuardianContext.of().with("player", new FakePlayer().perm("test:admin"));
        Method method = GuardedType.class.getMethod("simpleWithBoolean");

        assertTrue(this.guardian.allows(method, context));
        assertEquals(0, this.guardian.inspect(method, context).size());
    }

    @Test
    @SneakyThrows
    public void should_deny_in_mode_boolean() {

        GuardianContext context = GuardianContext.of().with("player", new FakePlayer().perm("test:user"));
        Method method = GuardedType.class.getMethod("simpleWithBoolean");

        GuardianViolation violation = new GuardianViolation(
            "false",
            "",
            Collections.emptySet(),
            Collections.emptySet(),
            GuardianAction.DENY
        );

        assertFalse(this.guardian.allows(method, context));
        assertIterableEquals(Collections.singletonList(violation), this.guardian.inspect(method, context));
    }

    @Test
    @SneakyThrows
    public void should_allow_in_mode_allow() {
        GuardianContext context = GuardianContext.of().with("player", new FakePlayer().role("ADMIN"));
        assertTrue(this.guardian.allows(GuardedType.class.getMethod("simpleWithAllow"), context));
    }

    @Test
    @SneakyThrows
    public void should_deny_in_mode_allow() {
        GuardianContext context = GuardianContext.of().with("player", new FakePlayer().role("USER"));
        assertFalse(this.guardian.allows(GuardedType.class.getMethod("simpleWithAllow"), context));
    }

    @Test
    @SneakyThrows
    public void should_allow_in_mode_deny() {
        GuardianContext context = GuardianContext.of().with("player", new FakePlayer().role("ADMIN"));
        assertTrue(this.guardian.allows(GuardedType.class.getMethod("simpleWithDeny"), context));
    }

    @Test
    @SneakyThrows
    public void should_deny_in_mode_deny() {
        GuardianContext context = GuardianContext.of().with("player", new FakePlayer().role("BANNED"));
        assertFalse(this.guardian.allows(GuardedType.class.getMethod("simpleWithDeny"), context));
    }

    @Test
    @SneakyThrows
    public void should_allow_in_mode_boolean_multi() {
        GuardianContext context = GuardianContext.of().with("player", new FakePlayer().perm("test:admin").role("ADMIN"));
        assertTrue(this.guardian.allows(GuardedType.class.getMethod("multiWithBoolean"), context));
    }

    @Test
    @SneakyThrows
    public void should_deny_in_mode_boolean_multi() {

        // no perm and role
        assertFalse(this.guardian.allows(GuardedType.class.getMethod("multiWithBoolean"),
            GuardianContext.of().with("player", new FakePlayer())));

        // perm mismatch
        assertFalse(this.guardian.allows(GuardedType.class.getMethod("multiWithBoolean"),
            GuardianContext.of().with("player", new FakePlayer().perm("test:user"))));

        // role mismatch
        assertFalse(this.guardian.allows(GuardedType.class.getMethod("multiWithBoolean"),
            GuardianContext.of().with("player", new FakePlayer().role("BANNED"))));
    }

    @Test
    @SneakyThrows
    public void should_deny_in_mode_boolean_multi_inspect() {

        List<GuardianViolation> violations = this.guardian.inspect(
            GuardedType.class.getMethod("multiWithBoolean"),
            GuardianContext.of().with("player", new FakePlayer().perm("test:user").role("BANNED"))
        );

        assertIterableEquals(Arrays.asList(
                new GuardianViolation(
                    "false",
                    "",
                    Collections.emptySet(),
                    Collections.emptySet(),
                    GuardianAction.DENY
                ),
                new GuardianViolation(
                    "BANNED",
                    "",
                    Collections.emptySet(),
                    Collections.singleton("BANNED"),
                    GuardianAction.DENY
                )
            ),
            violations);
    }

    static class EmptyType {
        public void doNothing() {
        }
    }

    static class GuardedType {

        @Guard(value = "player.hasPermission('test:admin')")
        public void simpleWithBoolean() {
        }

        @Guard(value = "player.role()", allow = "ADMIN")
        public void simpleWithAllow() {
        }

        @Guard(value = "player.role()", deny = "BANNED")
        public void simpleWithDeny() {
        }

        @Guard(value = "player.hasPermission('test:admin')")
        @Guard(value = "player.role()", deny = "BANNED")
        public void multiWithBoolean() {
        }
    }

    @Data
    @Accessors(fluent = true)
    public static class FakePlayer {

        private String role;
        private String perm;

        @SuppressWarnings("unused")
        public boolean hasPermission(@NonNull String perm) {
            return perm.equals(this.perm);
        }
    }
}
