package ambovombe.merana.utils.mapping;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)

public @interface Session{
	String session() default "";
}