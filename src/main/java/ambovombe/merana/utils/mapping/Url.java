package ambovombe.merana.utils.mapping;

import ambovombe.merana.utils.mapping.method.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Url {
    String value() default "";
    HttpMethod method() default HttpMethod.GET;
}
