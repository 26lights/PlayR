==========================
Tutorial project structure
==========================

Before introducing different Play'R concepts, here are some notes about the tutorial project internal structure. 

Most interesting things are located in the controller package.

The rest is supporting code to allow the application to do something.

Model
=====

The model package contains a case class and a container object for each defined resource.

The container object uses play's cache as storage, and exposes a simple api used by the controllers.

It allows you to list, get, create, update and delete resources.


Available resources/url
=======================

The playr-tutorial application defines a single entry point for the ReST API as : ``/api``.

From there, you will find the following resource tree:
 
 +----------------------------------------+------------------------------------------+-------------------------+
 | uri                                    | description                              | available http methods  |
 +========================================+==========================================+=========================+
 | ``/api/color``                         | a list of colors                         | GET, POST               |
 +----------------------------------------+------------------------------------------+-------------------------+
 | ``/api/color/:id``                     | color details                            | GET, PUT                |
 +----------------------------------------+------------------------------------------+-------------------------+
 | ``/api/crm``                           | crm related api                          |                         |
 +----------------------------------------+------------------------------------------+-------------------------+
 | ``/api/crm/person``                    | people management                        | GET, POST               |
 +----------------------------------------+------------------------------------------+-------------------------+
 | ``/api/crm/person/:id``                | person details                           | GET, PUT, DELETE        |
 +----------------------------------------+------------------------------------------+-------------------------+
 | ``/api/crm/company``                   | companies management                     | GET, POST               |
 +----------------------------------------+------------------------------------------+-------------------------+
 | ``/api/crm/company/:id``               | company details                          | GET, PUT, DELETE        |
 +----------------------------------------+------------------------------------------+-------------------------+
 | ``/api/crm/company/:id/employee``      | list of persons working for that company | GET, POST               |
 +----------------------------------------+------------------------------------------+-------------------------+
 | ``/api/crm/company/:id/employee/:eid`` | employee details                         | GET, PATCH, DELETE      |
 +----------------------------------------+------------------------------------------+-------------------------+
 | ``/api/crm/company/:id/functions``     | list of employees functions              | GET                     |
 +----------------------------------------+------------------------------------------+-------------------------+

Each resource introduces some Play'R concepts and is detailed in a separate part.
