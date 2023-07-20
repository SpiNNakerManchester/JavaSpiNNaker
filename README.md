
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
[API documentation](https://spinnakermanchester.github.io/JavaSpiNNaker/apidocs/) (Javadoc)
<br>
[Maven metadata](https://spinnakermanchester.github.io/JavaSpiNNaker/)


Pip Freeze
==========
This code was tested with all (SpiNNakerManchester)[https://github.com/SpiNNakerManchester] on tag 7.0.0

Pip Freeze showed the dependencies as:

appdirs==1.4.4

astroid==2.15.6

attrs==23.1.0

certifi==2023.5.7

charset-normalizer==3.2.0

contourpy==1.1.0

coverage==7.2.7

csa==0.1.12

cycler==0.11.0

dill==0.3.6

ebrains-drive==0.5.1

exceptiongroup==1.1.2

execnet==2.0.2

fonttools==4.41.0

graphviz==0.20.1

httpretty==1.1.4

idna==3.4

importlib-resources==6.0.0

iniconfig==2.0.0

isort==5.12.0

jsonschema==4.18.4

jsonschema-specifications==2023.7.1

kiwisolver==1.4.4

lazy-object-proxy==1.9.0

lazyarray==0.5.2

matplotlib==3.7.2

mccabe==0.7.0

mock==5.1.0

multiprocess==0.70.14

neo==0.12.0

numpy==1.24.4

opencv-python==4.8.0.74

packaging==23.1

pathos==0.3.0

Pillow==10.0.0

pkgutil_resolve_name==1.3.10

platformdirs==3.9.1

pluggy==1.2.0

pox==0.3.2

ppft==1.7.6.6

py==1.11.0

pylint==2.17.4

PyNN==0.11.0

pyparsing==2.4.7

pytest==7.4.0

pytest-cov==4.1.0

pytest-forked==1.6.0

pytest-instafail==0.5.0

pytest-progress==1.2.5

pytest-timeout==2.1.0

pytest-xdist==3.3.1

python-coveralls==2.9.3

python-dateutil==2.8.2

PyYAML==6.0.1

quantities==0.14.1

referencing==0.30.0

requests==2.31.0

rpds-py==0.9.2

scipy==1.10.1

six==1.16.0

tomli==2.0.1

tomlkit==0.11.8

typing_extensions==4.7.1

urllib3==2.0.4

websocket-client==1.6.1

wrapt==1.15.0

zipp==3.16.2

