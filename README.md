connector-workproductViewer
===========================

A GUI allows user to view the binary work products that have been assoicated with an XchangeCore incident.

Dependencies:
connector-base-util
connector-base-async

To Build:
1. Use maven and run "mvn clean install" to build the dependencies.
2. Run "mvn clean install" to build workproductViewer.

To Run:
1. Copy the workproductViewer/src/main/resources/contexts/async-context to the same directory of the WorkProductViewer.jar file.
2. Use an editor to open the async-context file.
3. Look for the webServiceTemplate bean, replace the "defaultUri" to the XchangeCore you are using to run this adapter to create the incidents.
   If not localhost, change http to https, example "https://test4.xchangecore.leidos.com/uicds/core/ws/services"
4. Change the "credentials" to a valid username and password that can access your XchangeCore.
5. More detail documentation of the connector can be found in /doc directory.