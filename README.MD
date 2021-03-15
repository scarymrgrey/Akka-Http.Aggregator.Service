Akka-http-server:
- Implementation:
    - Story 1 +
    - Story 2 +
    - Story 3 +
- Tests
    - Basic at the moment
    
Implementation details:
  - Most of the complexity goes from http-client-forwarder which need to aggregate multiple requests into one.
    Fortunately akka-http client based on streams have all this functionality from the box. 
    https://doc.akka.io/docs/akka/current/stream/operators/Source-or-Flow/groupedWithin.html
  - Config moved from explicit DI via a constructor into WithSettings trait
    
Run:
- git clone https://github.com/scarymrgrey/Akka-Http.Aggregator.Service.git
- cd Akka-Http.Aggregator.Service
- sbt run