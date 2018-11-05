FROM alpine as prep

RUN apk add --no-cache unzip
RUN mkdir /prep /tars
COPY maven/acdebugger-distribution-${project.version}.tar.gz /tars
RUN tar xzf /tars/acdebugger-distribution-${project.version}.tar.gz -C /prep
RUN mv /prep/acdebugger-distribution-${project.version} /prep/acdebugger

FROM ${docker.base.image.repo}:${docker.base.image.version}
LABEL maintainer=codice

ENV APP_HOME=/app

COPY --from=prep /prep/acdebugger $APP_HOME

ENTRYPOINT ["/app/bin/acdebugger"]
CMD ["--help"]
