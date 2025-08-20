# Edge
a simple serverless runtime implementation, powered by GraalJS and Vertx. Currently working in progress.

Code will be frequently refactored without warning until first release, and it's currently open-source for sharing idea and review. 

Discussion: [Telegram](https://t.me/kalculos_hub)

## Features
 - **High performance**: Heavy lifting is done to Vert.x and Netty, which provide solid foundations for I/O-bound applications.  
 - **Language-level integration**: For example, you can call `await` on _Vert.x Futures_ directly in JavaScript, without the need of wrappers.
 - **Automatic resource management**: Edge manages things like database connection initializations and pools to help you achieve zero configuration, without sacrificing your freedom to do by yourself.
 - **Isolated runtime**: Although it isn't a sandbox, edge isolates application codes and library codes and grant them with different privileges.
 - **Plugin support**: You can extend Edge's capability easily, check the built-in plugins.
 - **Easy interop**: We use GraalVM for our script execution engine, which makes your scripts run much faster and gives them access to Java's vast ecosystem.  
 - **Self-hostable**: Edge is easy to set up, and it uses little memory to host all of your scripts.
 - **Not vibe coded**: The code and this readme haven't been generated from LLMs.

## Build & Running
Before following the instructions, make sure you have Java 21 and Git installed.
```bash
java -version # check your java version. Use a graalvm variant or only the interpreter will work, and it will be much slower.
git clone https://github.com/iceBear67/egde
cd edge
./gradlew :launcher:run
```

Deploy a script for testing:
```bash
cat << EOF | curl -X POST -d@- http://localhost:8081/deploy
{
  "name" : "test",
  "version" : "0.0.1",
  "source" : {
    "name" : "test.mjs",
    "mime" : null,
    "file" : null,
    "language" : "js",
    "type" : "string",
    "data" : "export function handleRequest(request) {\n    request.response().end(\"hello\");\n}\n"
  }
}
EOF

# wait a second
curl http://test.localhost:8081/ -vv
```

## Why Vert.x instead of Virtual Threads?
Context is not thread-safe, yet running scripts requires Context.
To enhance concurrency through multithreading, you must manage these Contexts and script code via synchronisation or thread locals. 
For Virtual Threads, their primary feature is scalability; however, due to the aforementioned limitations, 
you are forced to either create a Context for each thread or degrade to serial execution due to lock contention (the worst-case scenario).
Therefore, in this scenario, Virtual Threads are not very effective, and we ultimately revert to thread reuse. 
Vert.x's event loop is an excellent choice for this purpose. The event loop not only resolves the management issues of
thread-unsafe objects like Context through thread encapsulation but also enables multiple workers to run "concurrently" (#9) , 
while staying in same threads—something Virtual Threads cannot achieve. This is 
because you cannot allocate the time slice of a thread to other workers while a
worker is waiting for an I/O operation; otherwise, you would still end up reverting to the event loop in some form.

## Roadmap
- Basic Functionality
  - [x] Load script (per-domain, aka virtual host) and serve HTTP requests.
  - [x] Restricted IO Access (not tested yet)
  - [ ] More configurable runtime.
- Regular Abilities
  - [x] Await support for Vert.x Futures.
  - [ ] Multiple language support 
  - Auto-scaling up by sharing deployments
    - [ ] Project modularize.
  - Plugins
    - [ ] Autoconfigured database table and JDBC connection for guest code.
    - [ ] Autoconfigured Redis connection for guest code.
- Security Features (but we are not intended for sandboxes.)
  - [ ] ~~From `SandboxPolicy.TRUSTED` to `SandboxPolicy.UNTRUSTED`~~ That will sacrifice a lot of features, not worth doing it.
  - [x] Isolation of trusted codes "libraries" and guest code.
    - Thus, Java world is only accessible to "libraries" from host.
  - [ ] Resource limitation
  - Plugins
    - [ ] Built-in rate limiter
- Release
  - [ ] Dockerfile
- A mature script runtime as a module.
  - Then you can benefit from its features like secured VFS, resource limitation, host-guest isolation, etc.