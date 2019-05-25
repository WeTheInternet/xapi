This module contains the basic logger interface,  
and a few adapter implementations.
    
Note, to use an adapter, you will be responsible for providing dependencies;  
this module only uses provided scope, to avoid leaking dependencies for adapters.