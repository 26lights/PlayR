==============
Color resource
==============

If you started with the :doc:`../demo` project, you should be already familiar with the Play'R way of defining resource and routing.

Let's look at the controller definition in ``controllers/ColorController.scala``:

.. code-block:: scala 

  object ColorController extends RestReadController[Color]
                            with ResourceCreate
                            with ResourceWrite {
    val name = "color"
  
    implicit val colorFormat = Json.format[Color]
  
    def fromId(sid: String) = toInt(sid).flatMap(id => ColorContainer.get(id))
  
    def list = Action { Ok(Json.toJson(ColorContainer.list)) }
  
    def read(color: Color) = Action { Ok(Json.toJson(color)) }
  
    def write(color: Color) = Action(parse.json) { request =>
      val newColor = request.body.as[Color].copy(id=color.id)
      ColorContainer.update(newColor)
      Ok(Json.toJson(newColor))
    }
  
    def create = Action(parse.json){ request =>
      val newColor = request.body.as[Color]
      ColorContainer.add(newColor.name, newColor.rgb)
      Created(Json.toJson(newColor))
    }
  }

Compared to the demo ``PersonController``, this one uses a shortcut trait. ``RestReadController`` is equivalent to ``Resource with ReadResource``

This resource also implements two new traits: 

``ResourceCreate``:
  It handles POST requests on ``/color``

``ResourceWrite``.
  It handles PUT requests on ``/color/:id``

In this example implementation, all requests and response are serialized to and from JSON.


This resource is added to our application api in ``controllers/Application.scala``:

.. code-block:: scala

  ...
  val api = RestApiRouter()
    .add(new RestResourceRouter(ColorController))
  ...

Here we have defined an api entry point (more routes will be added in the next examples) and added the controller.
