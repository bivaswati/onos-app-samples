Description of Template Files - Client Side
---------------------------------------------
Note that the directory naming convention must be observed for the files to be placed
in the correct location when the archive is built.Since our view has a unique identifier
"qosui"(same as view id declared inside Handler class),its client source files should
be placed under the directory  ~/src/main/resources/app/view/qosui.

Rules for placing the client files
---------------------------------------------
client files                  ->  ~/src/main/resources
client files for UI views     ->  app/view/
client files for "qosui" view ->  app/view/qosui and there are three files

qosui.html
qosui.js
qosui.css


The vital things are the two "glue" files used to patch references to our client-side
source into index.html. These files are located in ~src/main/resources/qosui directory.
They must be named css.html and js.html respectively, so the framework can find them.

Rules for Constructing the client files
---------------------------------------------

qosui.html - The outer <div> element defines the contents of our custom "view". It should use a dash-delimited id
             of the internal identifier for the view, prefixed with "ov".
             So, in our application, "qosui" becomes "ov-qosui". ("ov" stands for "ONOS view").

qosui.js  - 1.request and response event strings follow the convention of using the view ID as a prefix.
              This guarantees that our event names will be distinct from those of any other view.

            2.The module name should start with "ov" (lowercase) followed by the identifier for our view, in continuing camel-case.
              (ovQosui)

            3.name of our controller is to start with "Ov" (uppercase 'O') followed by the identifier for our view (continuing camel-case), followed by "Ctrl"
              (OvQosuiCtrl)