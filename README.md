# dsp-usrp-source

USRP driver for use with [dsp-common](https://github.com/rhodey/dsp-common), tested successfully on:
  + USRP B100
  + USRP1

## Install uhd-java
Clone the [uhd-java repository](https://github.com/rhodey/uhd-java) and
follow the instructions to install `org.anhonesteffort.uhd:uhd-java:x.x`
in your local Maven repo.

## Configure your USRP
Copy the file `example-usrp.properties` to another file named `usrp.properties`,
this file is read from your working directory at runtime and used to configure
your USRP:

## Build
```
$ mvn package
```

## Usage
Copy `target/dsp-usrp-source-x.jar` into the classpath of your dependant project.

## License

Copyright 2015 An Honest Effort LLC

Licensed under the GPLv3: http://www.gnu.org/licenses/gpl-3.0.html
