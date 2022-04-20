package eu.okaeri.acl.reflect;

import eu.okaeri.acl.guard.Guard;
import eu.okaeri.acl.guardian.Guardian;
import eu.okaeri.acl.guardian.GuardianContext;
import eu.okaeri.placeholders.Placeholders;
import eu.okaeri.placeholders.message.CompiledMessage;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ReflectGuardian extends Guardian {

    protected Placeholders placeholders;

    public static ReflectGuardian create(@NonNull Placeholders placeholders) {
        return new ReflectGuardian(placeholders);
    }

    @Override
    public String evaluate(@NonNull Guard guard, @NonNull GuardianContext context) {
        return this.placeholders.contextOf(CompiledMessage.of("{" + guard.value() + "}"))
            .with(context.getData())
            .apply();
    }
}
