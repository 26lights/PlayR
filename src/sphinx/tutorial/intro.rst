========
Tutorial
========

This tutorial will try to analyze the Play'R tutorial application and explain the associated API and functions.

A good start is to open the ``playr-tutorial`` project in your favorite editor and navigate through the different controllers starting from the ``Application`` object.

At the same time, you can run the application with ``sbt run`` and use your favorite ReST tool to play with the available resource.

Available resources/url
=======================

The playr-tutorial application define a single entry point for the ReST API as : ``/api`` .

From there, you will find the following resource tree:
 
 +----------------------------------------+------------------------------------------+-------------------------+
 | uri                                    | description                              | available http methods  |
 +========================================+==========================================+=========================+
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
 | ``/api/color``                         | a list of colors                         | GET, POST               |
 +----------------------------------------+------------------------------------------+-------------------------+
 | ``/api/color/:id``                     | color details                            | GET, PUT                |
 +----------------------------------------+------------------------------------------+-------------------------+

Each resource introduces some Play'R concepts and is detailed in a separate part.


.. toctree::
   :maxdepth: 2

   color
   person
   company
   employee
