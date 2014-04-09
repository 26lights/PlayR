=================
Employee resource
=================

Last resource in this tutorial with two new features: PATCH support and sub-resources.

Let's start with PATCH support; if you have read the HTTP to scala method mapping in :doc:`../basics`, it's just a matter of implementing the ``ResourceUpdate`` trait and provide an implementation for the ``update`` method.

.. code-block:: scala
  
  object EmployeeController extends RestReadController[Employee]
                               with ResourceCreate
                               with ResourceDelete
                               with ResourceUpdate {
    ...
    def update(employee: Employee) = Action { ... }    
    ...
  }

To simplify the common case of a resource with read, create, delete and update traits there is a shortcut trait:

.. code-block:: scala
  
  object EmployeeController extends RestRwdController[Employee] {
    ...
  }


Next, an employee is just a link between a person and a company, and in itself, an employee resource does not exists without a company.

In controller terms, it means that our ``EmployeeController`` depends on a ``Company`` resource instance.

So, we will rewrite our controller as a ``case class`` that takes a ``Company`` as parameter.

All methods in our controller will then have access to the matching ``Company``.



.. code-block:: scala

  case class EmployeeController(company: Company) extends RestRwdController[Employee]{
    ...
    def fromId(sid: String) = toInt(sid).flatMap(id => EmployeeContainer.get(id))

    def list = Action { Ok(Json.toJson(EmployeeContainer.filterList(_.companyId==company.id))) }
    ...
  }

All methods are implemented like in the other controller, except they can use the given ``Company`` object to retrieve information, like the ``list`` method that filters employees based on the company's id.

The last step is to explain to the router how we will access those employees and how to create new ``EmployeeController`` instances.

In URL terms, we will have access the employee list for company 1 through: ``/crm/company/1/employee``; and the employee with id 2 through: ``/crm/company/1/employee/2``

To create new controller instances, it's just a method that for a ``Company`` instance gives an ``EmployeeController`` instance:

.. code-block:: scala

  ...
  val crmApi = RestApiRouter()
    .add(PersonController)
    .add(new RestResourceRouter(CompanyController)

      .add("employee", company => EmployeeController(company))

      .add("functions", GET, CompanyController.functions _)
    )
   ...





