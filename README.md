# Edge
a simple serverless runtime implementation, powered by GraalJS and Vertx. Currently working in progress.  

Code will be frequently refactored without warning until first release, and it's currently open-source for sharing idea and review. 

Discussion: [Telegram](https://t.me/kalculos_hub)

## Roadmap
- Basic Functionality
  - [x] Load script (per-domain, aka virtual host) and serve HTTP requests.
  - [x] Restricted IO Access (not tested yet)
  - [ ] More configurable runtime.
- Regular Abilities
  - Auto-scaling up by sharing deployments
    - [ ] Project modularize.
  - Plugins
    - [ ] Autoconfigured database table and JDBC connection for guest code.
    - [ ] Autoconfigured Redis connection for guest code.
- Security Features (but we are not intended for sandboxes.)
  - [ ] Isolation of trusted codes "libraries" and guest code.
    - Thus, Java world is only accessible to "libraries" from host.
  - [ ] Resource limitation
  - Plugins
    - [ ] Built-in rate limiter
- Release
  - [ ] Dockerfile
- A mature script runtime as a module.
  - Then you can benefit from its features like secured VFS, resource limitation, host-guest isolation, etc.