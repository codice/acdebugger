# AC Debugger Docker Image

## Usage

To run `acdebugger` as a docker container:

```bash
docker container run codice/acdebugger:<VERSION> <args>
```

The args passed into the container can be any option supported by the `acdebugger` command

For example: `docker container run codice/acdebugger:1.7-SNAPSHOT -c -H some.host` would run acdebugger continuously against the the system running at `some.host:5005`