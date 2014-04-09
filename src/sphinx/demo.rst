====
Demo
====

A working version of this example is located in the ``samples/playr-demo`` project.

Just start it with ``sbt run`` and try it with ``curl`` or other HTTP tools.

Source code
===========

Let's start with a simple read only resource that manages a list of person.

.. code-block:: scala
 
  package controllers
  
  import play.api.mvc._
  import twentysix.playr._
  import twentysix.playr.simple._
  import play.api.libs.json.Json
  
  case class Person(name: String)
  
  object PersonController extends Controller
                             with Resource[Person]
                             with ResourceRead {
    def name = "person"
  
    def persons = Map(
      1 -> Person("john"),
      2 -> Person("jane")
    )
  
    implicit val personFormat = Json.format[Person]
  
    def fromId(sid: String): Option[Person] = toInt(sid).flatMap(persons.get(_))
  
    def read(person: Person) = Action { Ok(Json.toJson(person)) }
  
    def list() = Action { Ok(Json.toJson(persons.keys)) }
  }
  
  object PersonRouter extends RestResourceRouter(PersonController)


First, we define a case class that represents a person.

.. code-block:: scala

  case class Person(name: String)


Next, we define a Play controller that implements two Play'R traits.

.. code-block:: scala

  object PersonController extends Controller
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

  object PersonRouter extends RestResourceRouter(PersonController)


The only missing step is to reference this router in Play's routes file.

.. code:: nginx

  # Routes
  # This file defines all application routes (Higher priority routes first)
  # ~~~~

  ->      /person                     controllers.PersonRouter


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


