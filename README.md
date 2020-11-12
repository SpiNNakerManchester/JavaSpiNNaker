[![Build and Test](https://github.com/SpiNNakerManchester/JavaSpiNNaker/workflows/Build%20and%20Test/badge.svg?branch=actions)](https://github.com/SpiNNakerManchester/JavaSpiNNaker/actions?query=workflow%3A%22Build+and+Test%22+branch%3Amaster)
[![Coverage Status](https://coveralls.io/repos/github/SpiNNakerManchester/JavaSpiNNaker/badge.svg?branch=master)](https://coveralls.io/github/SpiNNakerManchester/JavaSpiNNaker?branch=master)
# JavaSpiNNaker
This is an implementation of the SpiNNaker host software in Java. It requires at least Java 8. We test regularly with OpenJDK 8 and OpenJDK 11.

This code currently supports these operations:

1. Executing a collection of Data Specifications (in a SQLite database) and uploading the resulting data to SpiNNaker.

2. Downloading the contents of recording regions and storing the downloaded data in a SQLite database.

The implementation of these operations is parallelised (to speed up multi-board SpiNNaker jobs) and uses high-speed protocols to move data, allowing large data to be moved on and off SpiNNaker systems far more rapidly (accelerations of 10&times; or more have been measured, depending on the details of job configuration).

# Usage
Check out this repository _beside_ your checkout of [SpiNNFrontEndCommon](https://github.com/SpiNNakerManchester/SpiNNFrontEndCommon), using the directory name JavaSpiNNaker.

Build this software with Apache Maven:

    mvn package

Configure your SpiNNaker software to include these settings:

    [Machine]
    enable_advanced_monitor_support = True

    [Java]
    use_java = True

The `enable_advanced_monitor_support` option is turned on by default. The `use_java` option is turned off by default. You may also need to set these options in the `[Java]` section:

## `java_call`

This option says how to run the Java language runtime. You only need to specify it when Java is not on your path. (On Windows, do _not_ specify it to use `javaw.exe`; that will make the system fail.)

    java_call = /path/to/bin/java

## `java_spinnaker_path`

This option says where to find the built version of JavaSpiNNaker. If you have followed the build and usage instructions above, you shouldn't need to specify this (as the built-in autodiscovery code in SpiNNFrontEndCommon handles it). Otherwise, the path needs to be the full path to the `spinnaker-exe.jar` file produced by the JavaSpiNNaker build.

    java_spinnaker_path = /path/to/spinnaker-exe.jar

