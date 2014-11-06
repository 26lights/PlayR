@import twentysix.playr.RestRouteActionType
@(selfName: String, apiItem: JQueryApiItem)
  var @(selfName) = function(id) {
    var idUrl = @(selfName).url+"/"+id;
    return { @for(action <- apiItem.actions) { @action match {
    case RestRouteActionType.Read => { 
      read: function() {
        return $.ajax(idUrl, {
          type: "GET"
        });
      },} 
    case RestRouteActionType.Write => { 
      write: function(data) {
        return $.ajax(idUrl, {
          type: "PUT", data: JSON.stringify(data), dataType: "json", contentType: "application/json"
        });
      },}
    case RestRouteActionType.Update => { 
      update: function(data) {
        return $.ajax(idUrl, {
          type: "PATCH", data: JSON.stringify(data), dataType: "json", contentType: "application/json"
        });
      },}
    case RestRouteActionType.Delete => { 
      remove: function() {
        return $.ajax(idUrl, {
          type: "DELETE"
        });
      },}
    case RestRouteActionType.Traverse => { @for(child <- apiItem.children) {
      /***************************************
       * Start @(child.name)*/
      @(child.name): function(path) {
        @views.js.jqueryObj(selfName+child.name, child)
      }(idUrl+"/"),
       /* End @(child.name)
        **************************************/
    }}
    case _ => {}
    }}
      id: id
    }
  };
  @if(apiItem.actions.contains(RestRouteActionType.List)) {
  @(selfName).list = function() {
    return $.ajax(@(selfName).url, {
      type: "GET"
    });
  }}
  @if(apiItem.actions.contains(RestRouteActionType.Create)) {
  @(selfName).create = function(data) {
    return $.ajax(@(selfName).url, {
      type: "POST", data: JSON.stringify(data), dataType: "json", contentType: "application/json"
    });
  }}
  @(selfName).url = path+"@JavaScript(apiItem.path)";
  return @(selfName);
