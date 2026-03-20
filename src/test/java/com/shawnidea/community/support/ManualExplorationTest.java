package com.shawnidea.community.support;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ShawnIdeaSpringBootTest
@Tag("manual")
@Disabled("Legacy exploratory test. Run manually only after converting it into assertion-based coverage.")
public @interface ManualExplorationTest {
}
