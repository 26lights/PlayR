===============
Person resource
===============

This resource introduces two new features: the use of the HTTP delete method and a namespace.

To use the HTTP delete method, just implement the ``DeleteResource`` trait:

.. code-block:: scala

  object PersonController extends RestReadController[Color]
                             with ResourceCreate
                             with ResourceWrite
                             with ResourceDelete
  ...

As writing controllers with create, read, write and delete is really common, there is a shortcut trait: ``RestCrudController``.

So, the ``PersonController`` definition can be rewritten as: 

.. code-block:: scala

  object PersonController extends RestCrudController[Person] {

  ...

    def delete(person: Person) = Action {
      PersonContainer.delete(person)
      NoContent
    }
  ...

Note that the delete method should return HTTP code 201: ``NoContent``

This controller should now be added to our application api in ``controllers/Application.scala`` .

Instead of adding it directly to the api router as was the case with the ``ColorController``, we will group all crm controllers under the same namespace ``/api/crm`` .

This is done by creating a new ``RestApiRouter`` instance and adding this one to the api under the ``crm`` route:

.. code-block:: scala

  ...

  val crmApi = RestApiRouter()
    .add(PersonController)

  val api = RestApiRouter()
    .add(new RestResourceRouter(ColorController))
    .add("crm" -> crmApi)

More controllers will now be added in the ``crm`` namespace.
