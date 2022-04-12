# Wiremock CORS Extension
A Wiremock Extension project for getting around issues faced with CORS (particularly, in my case, when using the "Try It Now" feature in Swagger for stubs hosted in Wiremock)

## Description
This is an extension for Wiremock which will work around the CORS protection (which is pretty strict in Chrome!) in order to not block the "Test it out!" functionality which Swagger provides when hitting stubs.

It does this by ensuring the following:
- An "Access-Control-Allow-Origin" response header is sent with a value of "*".
- If an "Access-Control-Request-Headers" header is present in the request, then that too is added in the list of response headers.
- Any user-defined request headers (such as Requesting-System, Accept, etc...) are appended to the "Access-Control-Allow-Headers" header.


## Usage
Within Wiremock Standalone you can invoke an extension(s) in the wiremock instance by doing the following:
```
$ java -cp "<wiremock_standalone_jar>:<wiremock_extension_jar_1>:...:<wiremock_extension_jar_n>" <path_to_wiremock_server_runner_main_class> --extensions <comma_delimited_path_to_extension_main_classes> <other_params>
```
So, for example:
```
$ java -cp "wiremock-standalone-2.11.0.jar:wiremockCorsExtension-0.0.1.jar" com.github.tomakehurst.wiremock.standalone.WireMockServerRunner --extensions com.aviva.uk.integration.wiremock.CORSResponseHeaderTransformer --port 9000
```
In addition, the cors_fix.json found in src/main/resources will need to be placed into the mappings/ directory in your Wiremock directory.

Thanks,
Rich
