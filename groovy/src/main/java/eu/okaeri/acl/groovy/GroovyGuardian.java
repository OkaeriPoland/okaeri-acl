package eu.okaeri.acl.groovy;

import eu.okaeri.acl.guard.Guard;
import eu.okaeri.acl.guardian.Guardian;
import eu.okaeri.acl.guardian.GuardianContext;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.*;
import org.codehaus.groovy.control.CompilerConfiguration;

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class GroovyGuardian extends Guardian {

    private CompilerConfiguration compilerConfiguration;

    public static GroovyGuardian create(@NonNull CompilerConfiguration compilerConfiguration) {
        return new GroovyGuardian(compilerConfiguration);
    }

    public static GroovyGuardian create() {
        return new GroovyGuardian(new CompilerConfiguration());
    }

    @Override
    public String evaluate(@NonNull Guard guard, @NonNull GuardianContext context) {

        Binding binding = new Binding(context.getData());
        GroovyShell groovyShell = new GroovyShell(binding, this.compilerConfiguration);

        return String.valueOf(groovyShell.evaluate(guard.value()));
    }
}
