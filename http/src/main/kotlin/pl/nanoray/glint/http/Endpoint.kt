package pl.nanoray.glint.http

data class Endpoint<PathParameters, QueryParameters>(
    val method: HttpRequest.Method,
    val url: EndpointUrl<PathParameters, QueryParameters>
)