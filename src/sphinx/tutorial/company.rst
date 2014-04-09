================
Company resource
================

The company resource is a simple crud resource like the person one, but it comes with a new feature: a custom function. 

This is a simple way to extends identified resources (and to overcome clients limitations) by handling url parts that follow the resource identifier.

For our company, we would like to retrieve a list employees functions for a given company.

First let's implement this in our controller:

.. code-block:: scala

  object CompanyController extends RestCrudController[Company]{
  ...
      def functions(company: Company) = Action {
        val functions = for {
          item <- EmployeeContainer.items.values if item.companyId==company.id
        } yield item.function

        Ok(Json.toJson(functions))
      }
  ...
  }

Next, we would like to have that action called when doing a HTTP GET on an url like: 
``/api/crm/company/1/functions``

To do so, we will add the company controller to the crm api:

.. code-block:: scala

  ...
  val crmApi = RestApiRouter()
    .add(PersonController)
    .add(CompanyController)
  ...

And to extend the company controller with a new function, we will explicitly create a ``RestResourceRouter`` and add the new function to it:

.. code-block:: scala

  ...
  val crmApi = RestApiRouter()
    .add(PersonController)
    .add(new RestResourceRouter(CompanyController)
      .add("functions", GET, CompanyController.functions _)
    )
  ...

