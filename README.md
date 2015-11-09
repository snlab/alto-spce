# ALTO SPCE (Simple Path Computation Engine)

ALTO SPCE module provides a simple path computation engine for ALTO and other projects. It works in ODL (OpenDaylight) SDN Controller.

## Installation

We assume you have set up your development environment by following [this link](https://wiki.opendaylight.org/view/GettingStarted:Development_Environment_Setup).

Run `mvn clean install` in the top directory of alto-spce project. After that, you can execute `./karaf/target/assembly/bin/karaf` to start alto-spce with ODL.

## Deployment

You can also deploy this module into a running ODL controller without stopping controller. Only run `./deploy.sh <distribution_directory>`. And `<distribution_directory>` is the path of your own running ODL distribution.

For example, if you start your ODL controller from `/root/distribution-karaf-0.4.0-SNAPSHOT/bin/karaf`, you can use the command `./deploy.sh /root/distribution-karaf-0.4.0-SNAP`.

And then, you can check whether the features of alto-spce are loaded in your karaf shell:

```
karaf@root()> feature:list | grep alto-spce
```

If features are loaded, you can install them:

```
karaf@root()> feature:install odl-alto-spce
```

## Usage

TODO
