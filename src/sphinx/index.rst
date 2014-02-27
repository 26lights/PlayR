======
Play'R
======

Play'R stands for: Playing with Play and ReST.

It's a simple, uniform, and introspectable way to declare ReST api in play.

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

Development and bug tracking is done on github:

`<https://github.com/26lights/PlayR>`_

========
Contents
========

.. toctree::
   :maxdepth: 2

   demo
   tutorial/intro

Indices and tables
==================

* :ref:`genindex`
* :ref:`modindex`
* :ref:`search`

