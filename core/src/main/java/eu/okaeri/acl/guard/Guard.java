package eu.okaeri.acl.guard;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Guard.List.class)
public @interface Guard {

    String value();
    String message() default "";
    String[] allow() default {};
    String[] deny() default {};

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        Guard[] value();
    }
}
