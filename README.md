# jDiff

Java object diffing and merging made easy.

# Build Status
[![Build Status](https://travis-ci.org/X-corpion/jDiff.svg?branch=master)](https://travis-ci.org/X-corpion/jDiff)
[![Coverage Status](https://coveralls.io/repos/github/X-corpion/jDiff/badge.svg?branch=master)](https://coveralls.io/github/X-corpion/jDiff?branch=master)
[![Maven Version](https://img.shields.io/maven-central/v/org.xcorpion/jDiff.svg)](https://mvnrepository.com/artifact/org.xcorpion/jDiff)

# Project Status
**This project is still under development so the APIs and underlying implementations are subject to change.**
But it should be able to handle non-critical production use cases with the current test coverage.

# Getting Started

1. Add this to maven

    ```xml
    <dependency>
        <groupId>org.xcorpion</groupId>
        <artifactId>jDiff</artifactId>
        <version>(Check the releases tab for latest version)</version>
    </dependency>
    ```

2. Create the object diff mapper

    Currently the reflection mapper is the only implementation.

    ```java
    ObjectDiffMapper mapper = new ReflectionObjectDiffMapper();
    ```

3. Diff/Merge objects

    ```java
    public class MyClass {

        private String str;
        private List<Integer> list;

        public MyClass(String str, List<Integer> list) {
            this.str = str;
            this.list = list;
        }

        public String getStr() {
            return str;
        }

        public List<Integer> getList() {
            return list;
        }
    }


    MyClass obj1 = new MyClass("foo", Arrays.asList(1, 2));
    MyClass obj2 = new MyClass("bar", Arrays.asList(2));

    DiffNode diff = mapper.diff(obj1, obj2);

    // apply diff in place
    MyClass newObj = mapper.applyDiff(obj1, diff);

    assert newObj == obj1;
    assert "bar".equals(newObj.getStr());
    assert Arrays.asList(1, 2).equals(newObj.getList());

    obj1 = new MyClass("foo", Arrays.asList(1, 2));
    // apply diff into a new object
    newObj = mapper.applyDiff(obj1, diff, Collections.singleton(Feature.MergingStrategy.DEEP_CLONE_SOURCE));

    assert newObj != obj1;
    ```

# Advanced Usage

## Ignore transient fields

```java
ObjectDiffMapper mapper = new ReflectionObjectDiffMapper()
        .enable(Feature.IgnoreFields.TRANSIENT);
```

## Custom Merging/Diffing Handler

```java
/*
 * This example shows how to serialize dates into millis for diffing instead of field by field reflection
 */
public class DateDiffingHandler extends AbstractDiffingHandler<Date> {

    @Nonnull
    @Override
    public DiffNode diff(@Nonnull Date src, @Nonnull Date target, @Nonnull DiffingContext diffingContext) {
        if (src.equals(target)) {
            return DiffNode.empty();
        }
        return new DiffNode(new Diff(Diff.Operation.UPDATE_VALUE, getMillis(src), getMillis(target)));
    }

    private static Long getMillis(Date date) {
        if (date == null) {
            return null;
        }
        return date.getTime();
    }

}

public class DateMergingHandler extends AbstractMergingHandler<Date> {

    @Nullable
    @Override
    public Date merge(@Nullable Date src, @Nonnull DiffNode diffNode,
            @Nonnull MergingContext mergingContext) throws MergingException {
        Diff diff = diffNode.getDiff();
        if (diff == null) {
            return src;
        }
        switch (diff.getOperation()) {
            case NO_OP:
                return src;
            case UPDATE_VALUE:
                validateSourceValue(getMillis(src), diffNode, mergingContext);
                Long target = (Long) diff.getTargetValue();
                if (target == null) {
                    return null;
                }
                return new Date(target);
        }
        return src;
    }

    private static Long getMillis(Date date) {
        if (date == null) {
            return null;
        }
        return date.getTime();
    }

}

ObjectDiffMapper mapper = new ReflectionObjectDiffMapper();
diffMapper.registerDiffingHandler(Date.class, new DateDiffingHandler());
diffMapper.registerMergingHandler(Date.class, new DateMergingHandler());

// alternatively you can also just annotate your field like:
class YourClass {
    @TypeHandler(
            diffUsing = DateDiffingHandler.class,
            mergeUsing = DateMergingHandler.class
    )
    Date date;
}
```

### Generic Field Support

```java
public class StringListToStringDiffingHandler extends AbstractDiffingHandler<List<String>> {
    @Nonnull
    @Override
    public DiffNode diff(@Nonnull List<String> src, @Nonnull List<String> target, @Nonnull DiffingContext diffingContext) {
        if (src.equals(target)) {
            return new DiffNode();
        }
        Diff diff = new Diff(Diff.Operation.UPDATE_VALUE, String.join("|", src), String.join("|", target));
        return new DiffNode(diff);
    }
}

ObjectDiffMapper mapper = new ReflectionObjectDiffMapper();
diffMapper.registerMergingHandler(new StringToStringListMergingHandler());
// alternatively you can also just annotate your field like:
class YourClass {
    @TypeHandler(
            diffUsing = StringListToStringDiffingHandler.class
    )
    List<String> stringList;
}
```

# Features
- Support arbitrary object diffing and merging

  - "Native" diffing support for array, collection (`Iterable`, `Set`, `Map`) and general objects
  - "Native" merging support for array, collection (`List`, `Set`, `Map`) and general objects
  - Pluggable interface to allow for extensible diffing/merging support (e.g. unmodifiable collections)
  
- Support abitrary size object diffing/merging as long as the object fits into memory using Iterative approach
 
- Support optional validation during merging

- Allow for ignoring transient fields upon configuration

- Allow for adding custom object equality checker to avoid expensive comparison

- Allow for using either `equals()` or `hashCode()` for default object equality check 
 
- Seralization friendly diffing result (e.g. can be stored and recovered using JSON)

- Well tested

# Use Cases

- Object auditing record

  You can generate reports on how objects are modified.
  
- Distributed object change handling system

  You can build an ETL type of system:
  
  build diffs on one set of boxes, seralize them in db, and selectively merge them on other boxes based on certain criteria
  
# Contribution

## Release Handling

```shell
$ mvn gitflow:release-start
$ git push origin HEAD
```

Travis will pick up the release branch, release artifacts to Sonatype OSSRH, merge and tag the release.

If that failed, a manual local merge can be done (in release branch):
```shell
$ mvn gitflow:release-finish
```
