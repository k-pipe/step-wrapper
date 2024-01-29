FROM ghcr.io/graalvm/native-image-community:21-muslib AS build
ADD src ./
RUN javac org/kpipe/*.java
RUN native-image org.kpipe.Main --static --libc=musl -march=compatibility
FROM alpine:latest
COPY --from=build /app/org.kpipe.main /bin/step-wrapper
ENTRYPOINT ["step-wrapper"]
