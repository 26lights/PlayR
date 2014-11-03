======
Play'R
======

Play'R stands for: Playing with Play and ReST.

It's a simple, uniform, and introspectable way to declare ReST APIs in play.

simple
  Just implements some traits and Play'R will do the routing for you.

uniform
  Play'R defines an uniform mapping from http methods to controller methods.

introspectable
  | The declared resources capabilities can be listed and used to generate documentation or ui connectors.
  | See the `PlayR-swagger project <https://github.com/26lights/PlayR-swagger>`_ as an example.


========
Features
========

- Uses standard Play controllers
- Map HTTP verbs to controller methods via traits implementation.
- Resource routing and dependencies defined in Scala
- Introspection API allowing you to generate content from your defined ReST API

===========
Development
===========

.. |26lights| image:: 26lights.png
    :width: 64px
    :target: http://www.26lights.com

Development is supported by |26lights|.

Source and bug tracking are hosted on github: `<https://github.com/26lights/PlayR>`_

.. warning::
  
  Play'R is a young project and the API is subject to change.

=====
Usage
=====

To use Play'R in your sbt based project, you should add a resolver for the 26lights public repository:

.. code-block:: scala
  
  resolvers += "26Lights releases" at "http://build.26source.org/nexus/content/repositories/public-releases"

and add Play'R to your library dependencies:

.. code-block:: scala
  
  libraryDependencies ++= Seq (
    "26lights"  %% "playr"  % "0.4.0"
  )



===============
Further reading
===============

If not already done, you should start with the :doc:`demo` example as a quick introduction.

After that, you can read about :doc:`Play'R basics <basics>` and the more complete :doc:`tutorial <tutorial/intro>`.

========
Contents
========

.. toctree::

    demo
    basics
    tutorial/intro
    API (scaladoc) <http://playr.26source.org/api>
    license
    → by 26Lights <http://www.26lights.com>
