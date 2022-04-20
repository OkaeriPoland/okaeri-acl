package eu.okaeri.acl.testreflectguardian;

import eu.okaeri.acl.guard.Guard;
import eu.okaeri.acl.guardian.GuardianAction;
import eu.okaeri.acl.guardian.GuardianContext;
import eu.okaeri.acl.guardian.GuardianViolation;
import eu.okaeri.acl.reflect.ReflectGuardian;
import eu.okaeri.placeholders.reflect.ReflectPlaceholders;
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

public class TestReflectGuardian {

    @Test
    @SneakyThrows
    public void should_allow_no_guards() {

        ReflectGuardian guardian = ReflectGuardian.create(ReflectPlaceholders.create());
        GuardianContext context = GuardianContext.of().with("key", "value");
        Method method = EmptyType.class.getMethod("doNothing");

        assertTrue(guardian.allows(method, context));
        assertTrue(guardian.inspect(method, context).isEmpty());
    }

    @Test
    @SneakyThrows
    public void should_allow_in_mode_boolean() {

        ReflectGuardian guardian = ReflectGuardian.create(ReflectPlaceholders.create());
        GuardianContext context = GuardianContext.of().with("player", new FakePlayer().perm("test:admin"));
        Method method = GuardedType.class.getMethod("simpleWithBoolean");

        assertTrue(guardian.allows(method, context));
        assertEquals(0, guardian.inspect(method, context).size());
    }

    @Test
    @SneakyThrows
    public void should_deny_in_mode_boolean() {

        ReflectGuardian guardian = ReflectGuardian.create(ReflectPlaceholders.create());
        GuardianContext context = GuardianContext.of().with("player", new FakePlayer().perm("test:user"));
        Method method = GuardedType.class.getMethod("simpleWithBoolean");

        GuardianViolation violation = new GuardianViolation(
            "false",
            "failed test 'player.hasPermission('test:admin')'",
            Collections.emptySet(),
            Collections.emptySet(),
            GuardianAction.DENY
        );

        assertFalse(guardian.allows(method, context));
        assertIterableEquals(Collections.singletonList(violation), guardian.inspect(method, context));
    }

    @Test
    @SneakyThrows
    public void should_allow_in_mode_allow() {
        ReflectGuardian guardian = ReflectGuardian.create(ReflectPlaceholders.create());
        GuardianContext context = GuardianContext.of().with("player", new FakePlayer().role("ADMIN"));
        assertTrue(guardian.allows(GuardedType.class.getMethod("simpleWithAllow"), context));
    }

    @Test
    @SneakyThrows
    public void should_deny_in_mode_allow() {
        ReflectGuardian guardian = ReflectGuardian.create(ReflectPlaceholders.create());
        GuardianContext context = GuardianContext.of().with("player", new FakePlayer().role("USER"));
        assertFalse(guardian.allows(GuardedType.class.getMethod("simpleWithAllow"), context));
    }

    @Test
    @SneakyThrows
    public void should_allow_in_mode_deny() {
        ReflectGuardian guardian = ReflectGuardian.create(ReflectPlaceholders.create());
        GuardianContext context = GuardianContext.of().with("player", new FakePlayer().role("ADMIN"));
        assertTrue(guardian.allows(GuardedType.class.getMethod("simpleWithDeny"), context));
    }

    @Test
    @SneakyThrows
    public void should_deny_in_mode_deny() {
        ReflectGuardian guardian = ReflectGuardian.create(ReflectPlaceholders.create());
        GuardianContext context = GuardianContext.of().with("player", new FakePlayer().role("BANNED"));
        assertFalse(guardian.allows(GuardedType.class.getMethod("simpleWithDeny"), context));
    }

    @Test
    @SneakyThrows
    public void should_allow_in_mode_boolean_multi() {
        ReflectGuardian guardian = ReflectGuardian.create(ReflectPlaceholders.create());
        GuardianContext context = GuardianContext.of().with("player", new FakePlayer().perm("test:admin").role("ADMIN"));
        assertTrue(guardian.allows(GuardedType.class.getMethod("multiWithBoolean"), context));
    }

    @Test
    @SneakyThrows
    public void should_deny_in_mode_boolean_multi() {

        ReflectGuardian guardian = ReflectGuardian.create(ReflectPlaceholders.create());

        // no perm and role
        assertFalse(guardian.allows(GuardedType.class.getMethod("multiWithBoolean"),
            GuardianContext.of().with("player", new FakePlayer())));

        // perm mismatch
        assertFalse(guardian.allows(GuardedType.class.getMethod("multiWithBoolean"),
            GuardianContext.of().with("player", new FakePlayer().perm("test:user"))));

        // role mismatch
        assertFalse(guardian.allows(GuardedType.class.getMethod("multiWithBoolean"),
            GuardianContext.of().with("player", new FakePlayer().role("BANNED"))));
    }

    @Test
    @SneakyThrows
    public void should_deny_in_mode_boolean_multi_inspect() {

        ReflectGuardian guardian = ReflectGuardian.create(ReflectPlaceholders.create());

        List<GuardianViolation> violations = guardian.inspect(
            GuardedType.class.getMethod("multiWithBoolean"),
            GuardianContext.of().with("player", new FakePlayer().perm("test:user").role("BANNED"))
        );

        assertIterableEquals(Arrays.asList(
                new GuardianViolation(
                    "false",
                    "failed test 'player.hasPermission('test:admin')'",
                    Collections.emptySet(),
                    Collections.emptySet(),
                    GuardianAction.DENY
                ),
                new GuardianViolation(
                    "BANNED",
                    "failed test 'player.role()'",
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
