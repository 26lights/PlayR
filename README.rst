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
  | the declared resources capabilities can be listed and used to generate documentation or ui connectors.
  | See the `PlayR-swagger project <https://github.com/26lights/PlayR-swagger>`_ as an example.


.. |26lights| image:: src/sphinx/26lights.png
    :width: 64px
    :align: middle
    :target: http://www.26lights.com

Development is supported by |26lights|.

.. warning::
  
  Play'R is a young project and the API is subject to change.



========
Features
========

- Uses standard Play controllers
- Map HTTP verbs to controller methods via traits implementation.
- Resource routing and dependencies defined in Scala
- Introspection API allowing you to generate content from your defined ReST API

=====
Usage
=====

To use Play'R in your sbt based project, you should add a resolver for the 26lights public repository:

.. code-block:: scala
  
  resolvers += "26Lights snapshots" at "http://build.26source.org/nexus/content/repositories/public-snapshots"

and add Play'R to your library dependencies:

.. code-block:: scala
  
  libraryDependencies ++= Seq (
    "26lights"  %% "playr"  % "0.2.0-SNAPSHOT"
  )


=============
Quick Example
=============

A working version of this example is located in the ``samples/playr-demo`` project.

Just start it with ``sbt run`` and try it with ``curl`` or other HTTP tools.

Source code
===========

Let's start with a simple read only resource that manages a list of person

.. code-block:: scala
 
  import play.api.mvc._
  import play.api.libs.json.Json
  import twentysix.playr._

  case class SimplePerson(name: String)

  object SimplePersonController extends Controller
                                   with Resource[Person]
                                   with ResourceRead {
    def name = "person"

    def persons = Map(
      1 -> SimplePerson("john"),
      2 -> SimplePerson("jane")
    )

    implicit val personFormat = Json.format[SimplePerson]

    def fromId(sid: String): Option[SimplePerson] = toInt(sid).flatMap(persons.get(_))

    def read(person: SimplePerson) = Action { Ok(Json.toJson(person)) }

    def list() = Action { Ok(Json.toJson(persons.keys)) }
  }

  object SimplePersonRouter extends RestResourceRouter(SimplePersonController)

First, we define a case class that represents a person.

.. code-block:: scala

  case class SimplePerson(name: String)


Next, we define a Play controller that implements two Play'R traits.

.. code-block:: scala

  object SimplePersonController extends Controller
                                   with Resource[Person]
                                   with ResourceRead


The ``Resource`` trait extends Controller, defines basic resource capabilities and it requires you to define:

``name``
  a name that can be used by the router to access your resource

``fromId``
  a method to retrieve your resource instance from an identifier (given in the URL)


The ``ResourceRead`` trait defines that there is a way to read that resource; it requires you to define two methods:

``list``
  respond to an http GET method on the resource's path, in this case, it returns the list of available id

``read``
  respond to an http GET method on an identified resource, in this case, it returns the person object serialized as JSON.


Finally, we define a ``RestResourceRouter`` instance that will route incoming requests for that resource.

.. code-block:: scala

  object SimplePersonRouter extends RestResourceRouter(SimplePersonController)


The only missing step is to reference this router in Play's routes file.

.. code-block:: nginx

  # Routes
  # This file defines all application routes (Higher priority routes first)
  # ~~~~

  ->      /person                     controllers.SimplePersonRouter


Demo
====

To show how the router works, let's use ``curl`` with some url.

.. code-block:: console

  $ curl -f http://localhost:9000/person
  [1,2]

A simple http GET on the person resource returns the list of available ids as a json list.
It's the result of the controller's ``list`` method.

.. code-block:: console

  $ curl -f http://localhost:9000/person/1
  {"name":"john"}

If we add a valid id to the URL, we get the JSON version of that resource.
It's the result of the controller's ``read`` method.


Let's try to find what methods our resource support:

.. code-block:: console

  $ curl -f -XOPTIONS -i http://localhost:9000/person
  HTTP/1.1 200 OK
  Allow: GET
  Content-Length: 0


Let's try some erroneous requests.

First, a not supported method on the resource:

.. code-block:: console

  $ curl -f -XPOST http://localhost:9000/person
  curl: (22) The requested URL returned error: 405 Method Not Allowed
  $ curl -f -XPOST http://localhost:9000/person/1
  curl: (22) The requested URL returned error: 405 Method Not Allowed

Returns the expected «method not supported» code, both for the resource itself and the identified resource.

.. code-block:: console

  $ curl -f http://localhost:9000/person/5
  curl: (22) The requested URL returned error: 404 Not Found

There are only two existing person resource, id 5 is invalid, so it returns «not found»


==================
More documentation
==================

A more complete documentation, showing all supported HTTP methods, and more complex routing with sub-resources is available in the `Play'R documentation <http://playr.26source.org>`_

The associated code is in the ``samples/playr-tutorial`` project.

====
TODO
====

Play'R can already be used to develop ReST API, but it's only a starting point and a lot more is left to do, like:

- Use objects for HTTP verbs instead of strings
- Multiple HTTP method per action
- Routing configuration DSL
- Reverse routing
- Resource type introspection
- Transactional router
