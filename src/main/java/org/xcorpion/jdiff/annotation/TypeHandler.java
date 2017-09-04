package org.xcorpion.jdiff.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.xcorpion.jdiff.api.DiffingHandler;
import org.xcorpion.jdiff.api.EqualityChecker;
import org.xcorpion.jdiff.api.MergingHandler;

@Target(value = {ElementType.FIELD, ElementType.TYPE})
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface TypeHandler {

    interface None extends EqualityChecker<Object>, DiffingHandler<Object>, MergingHandler<Object> {}

    Class<? extends EqualityChecker<?>> checkEqualityUsing() default None.class;

    Class<? extends DiffingHandler<?>> diffUsing() default None.class;

    Class<? extends MergingHandler<?>> mergeUsing() default None.class;

}
