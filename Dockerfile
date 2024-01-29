FROM ghcr.io/graalvm/native-image-community:21-muslib AS build
ADD src ./
RUN javac org/kpipe/StepWrapper.java
RUN native-image org.kpipe.StepWrapper --static --libc=musl -march=compatibility
FROM alpine:latest
COPY --from=build /app/org.kpipe.stepwrapper /bin/step-wrapper
ENTRYPOINT ["step-wrapper"]
