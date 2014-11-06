@import twentysix.playr.RestRouteActionType
@(api: Seq[JQueryApiItem], basePath: String)

var restApi = {};

@for(apiItem <- api) {
  /***************************************************************************
   * @(apiItem.name)
   ***************************************************************************/
  @("restApi."+ apiItem.name) =  function(path) {
    @views.js.jqueryObj("self", apiItem)
  }("@JavaScript(basePath)");
}

