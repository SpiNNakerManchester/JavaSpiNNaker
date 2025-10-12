# JavaSpiNNaker
This is an implementation of the SpiNNaker host software in Java. It requires at least Java 11. We test regularly with OpenJDK 11, 14 and 17. It also includes the new implementation of the Spalloc (SpiNNaker Allocator) service and associated support tools.

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

# Spalloc Server
[Spalloc Server](https://github.com/SpiNNakerManchester/JavaSpiNNaker/tree/master/SpiNNaker-allocserv) is its own sub-project.

# Documentation
[API documentation](https://javaspinnaker.readthedocs.io/en/7.4.0a2)
<br>
[Maven metadata](https://javaspinnaker.readthedocs.io/en/7.4.0a2)
