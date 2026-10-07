package com.kerware.e2e.framework.extension;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation composée à poser sur toute classe de test end-to-end.
 *
 * <p>Active le framework ({@link PlaywrightExtension}) et étiquette la classe {@code e2e}
 * (filtrable avec {@code mvn test -Dgroups=e2e}).</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith(PlaywrightExtension.class)
@Tag("e2e")
public @interface TestE2E {
}
