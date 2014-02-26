=====
PlayR
=====

PlayR stands for: Playing with Play and ReST.

It's a simple, uniform, and introspectable way to declare ReST api in play.

========
Features
========

- Uses standard Play controllers
- Map HTTP verbs to controller methods via traits implementation.
- Resource routing and dependencies defined in Scala
- Introspection API allowing you to generate content from your defined ReST API

=============
Quick Example
=============

Source code
===========

Let's start with a simple read only resource that manages a list of person

.. code:: scala
 
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

.. code:: scala

  case class SimplePerson(name: String)


Next, we define a Play controller that implements two PlayR traits

.. code:: scala

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
  respond to a http GET method on the resource's path, in this case, it returns the list of available id

``read``
  respond to a http GET method on an identified resource, in this case, it returns the person object serialized as json.


Finally, we define a ``RestResourceRouter`` instance that will route requests coming for that resource

.. code:: scala

  object SimplePersonRouter extends RestResourceRouter(SimplePersonController)


The only missing step is to reference this router in the play's routes file

.. code:: scala

  # Routes
  # This file defines all application routes (Higher priority routes first)
  # ~~~~

  ->      /person                     controllers.SimplePersonRouter


Demo
====

To show how the router works, let's use ``curl`` with some url.

.. code:: console

  $ curl -f http://localhost:9000/person
  [1,2]

A simple http GET on the person resource returns a the list of available id as a json list.
It's the result of the controller's ``list`` method

.. code:: console

  $ curl -f http://localhost:9000/person/1
  {"name":"john"}

If we add a valid id to the URL, we get the json version of that resource.
It's the result of the controller's ``read`` method.


Let's try to find what methods our resource support:

.. code:: console

  $ curl -f -XOPTION http://localhost:9000/person
  {"name":"john"}


Let's try some erroneous requests.

First, a not supported method on the resource:

.. code:: console

  $ curl -f -XPOST http://localhost:9000/person
  curl: (22) The requested URL returned error: 405 Method Not Allowed
  $ curl -f -XPOST http://localhost:9000/person/1
  curl: (22) The requested URL returned error: 405 Method Not Allowed

Returns the expected "method not supported" code, both for the resource itself and the identified resource.

.. code:: console

  $ curl -f http://localhost:9000/person/5
  curl: (22) The requested URL returned error: 404 Not Found

There are only two existing person resource, id 5 is invalid, so returns "not found"

====
TODO
====

- Use objects for HTTP verbs instead strings
- Routing configuration DSL
- Reverse routing

