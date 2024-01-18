FROM ghcr.io/graalvm/native-image-community:21-muslib AS build
ADD src ./
RUN javac org/kpipe/StepWrapper.java
RUN native-image org.kpipe.StepWrapper --static --libc=musl -march=compatibility

# experimental: -H:TempDirectory=/tmp

FROM alpine:3.18.4
COPY --from=build /app/org.kpipe.StepWrapper /bin/step-wrapper
