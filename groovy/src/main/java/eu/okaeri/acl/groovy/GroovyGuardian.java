package eu.okaeri.acl.groovy;

import eu.okaeri.acl.guard.Guard;
import eu.okaeri.acl.guardian.Guardian;
import eu.okaeri.acl.guardian.GuardianContext;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.*;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class GroovyGuardian extends Guardian {

    private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("okaeri.platform.debug", "false"));
    private static final Logger LOGGER = Logger.getLogger(GroovyGuardian.class.getSimpleName());

    protected final Map<String, Script> scriptCache = new ConcurrentHashMap<>();

    protected CompilerConfiguration compilerConfiguration;
    protected final GroovyShell groovyShell;

    public static GroovyGuardian create(@NonNull CompilerConfiguration compilerConfiguration) {
        GroovyGuardian guardian = new GroovyGuardian(compilerConfiguration, new GroovyShell(compilerConfiguration));
        guardian.groovyShell.evaluate("return 1"); // prime shell for faster response time later
        return guardian;
    }

    public static GroovyGuardian create() {
        return create(new CompilerConfiguration());
    }

    @Override
    public String evaluate(@NonNull Guard guard, @NonNull GuardianContext context) {

        Script script = this.scriptCache.computeIfAbsent(guard.value(), this::parse);
        script.setBinding(new Binding(context.getData()));

        return String.valueOf(script.run());
    }

    public long prime(@NonNull Class<?> clazz) {

        long primed = this.prime(clazz.getAnnotationsByType(Guard.class));

        for (Method method : clazz.getMethods()) {
            primed += this.prime(method.getAnnotationsByType(Guard.class));
        }

        for (Method declaredMethod : clazz.getDeclaredMethods()) {
            primed += this.prime(declaredMethod.getAnnotationsByType(Guard.class));
        }

        return primed;
    }

    public long prime(@NonNull Guard[] annotations) {
        return Arrays.stream(annotations)
            .filter(annotation -> this.prime(annotation.value()))
            .count();
    }

    public boolean prime(@NonNull String expression) {

        if (this.scriptCache.containsKey(expression)) {
            return false;
        }

        Script script = this.parse(expression);
        this.scriptCache.put(expression, script);

        return true;
    }

    protected Script parse(@NonNull String expression) {

        long start = System.nanoTime();
        Script script = this.groovyShell.parse(expression);

        if (DEBUG) {
            long took = System.nanoTime() - start;
            long tookMs = took / 1000L / 1000L;
            LOGGER.info("Parsed '" + expression + "' in " + tookMs + "ms/" + took + "ns");
        }

        return script;
    }
}
