# jDiff

Java object diffing and merging made easy.

# Build Status
[![Build Status](https://travis-ci.org/X-corpion/jDiff.svg?branch=master)](https://travis-ci.org/X-corpion/jDiff) [![Coverage Status](https://coveralls.io/repos/github/X-corpion/jDiff/badge.svg?branch=master)](https://coveralls.io/github/X-corpion/jDiff?branch=master)

# Project Status
TODO items:

- Documentation
- Examples
- Publish to maven

# Features
- Support arbitrary object diffing and merging

  - "Native" diffing support for array, collection (Iterable, Set, Map) and general objects
  - "Native" merging support for array, collection (List, Set, Map) and general objects
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
