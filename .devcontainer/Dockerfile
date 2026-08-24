FROM debian:bookworm-slim@sha256:362e64223cc0da95422b3b13c045186fc0a81250e765d31c025fbddf257f6143

ARG JDK17_URL=https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.20.1%2B1/OpenJDK17U-jdk_x64_linux_hotspot_17.0.20.1_1.tar.gz
ARG JDK17_SHA256=3808d1d15e3ec6bd5b84057fb5d84c33d8a1536a258146bcea2e603fc726e08e
ARG JDK21_URL=https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk_x64_linux_hotspot_21.0.12.1_1.tar.gz
ARG JDK21_SHA256=ce79869e1307ed8ee1e2baa86a412b1eb5b75d10a01006d788a6f968bcfaee94
ARG NODE_URL=https://nodejs.org/dist/v24.19.0/node-v24.19.0-linux-x64.tar.xz
ARG NODE_SHA256=14b342e71204f811bde6153be8e04b62aef63c236fef92b55f9c83154b409647
ARG ANDROID_TOOLS_URL=https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip
ARG ANDROID_TOOLS_SHA256=0877a1d048fe4a24efe2eff536ca4223f7adeb58648bb81909d33c446918cfa8
ARG COMPOSE_HOT_RELOAD_IDEA_URL=https://repo.maven.apache.org/maven2/org/jetbrains/compose/hot-reload/hot-reload-gradle-idea/1.0.0/hot-reload-gradle-idea-1.0.0.jar
ARG COMPOSE_HOT_RELOAD_IDEA_SHA256=85f87c4f0144b548a6cb2d27d63a6952e7c396ed3cd1bde3ca7a193652f31531
ARG HOST_USER=

SHELL ["/bin/bash", "-o", "pipefail", "-c"]
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    ca-certificates curl file fontconfig git git-lfs libasound2 libfreetype6 libgtk-3-0 \
    libncurses6 libnss3 libstdc++6 libx11-6 libxext6 libxi6 libxkbcommon0 \
    libxrender1 libxtst6 openssh-client openssl procps unzip xz-utils zlib1g \
    && rm -rf /var/lib/apt/lists/*
RUN install -d -m 0755 /etc/ssh \
    && printf '%s\n' 'github.com ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIOMqqnkVzrm0SdG6UOoqKLsabgH5C9okWi0dh2l9GKJl' > /etc/ssh/ssh_known_hosts \
    && chmod 0644 /etc/ssh/ssh_known_hosts

RUN curl -fsSL "$JDK17_URL" -o /tmp/jdk17.tgz \
    && echo "$JDK17_SHA256  /tmp/jdk17.tgz" | sha256sum -c - \
    && mkdir -p /opt/java/temurin-17 \
    && tar -xzf /tmp/jdk17.tgz --strip-components=1 -C /opt/java/temurin-17 \
    && curl -fsSL "$JDK21_URL" -o /tmp/jdk21.tgz \
    && echo "$JDK21_SHA256  /tmp/jdk21.tgz" | sha256sum -c - \
    && mkdir -p /opt/java/temurin-21 \
    && tar -xzf /tmp/jdk21.tgz --strip-components=1 -C /opt/java/temurin-21 \
    && curl -fsSL "$NODE_URL" -o /tmp/node.tar.xz \
    && echo "$NODE_SHA256  /tmp/node.tar.xz" | sha256sum -c - \
    && tar -xJf /tmp/node.tar.xz --strip-components=1 -C /usr/local \
    && rm /tmp/jdk17.tgz /tmp/jdk21.tgz /tmp/node.tar.xz

RUN ln -s /opt/java/temurin-17/bin/* /usr/local/bin/
RUN mkdir -p /opt/jetbrains-tooling \
    && curl -fsSL "$COMPOSE_HOT_RELOAD_IDEA_URL" -o /opt/jetbrains-tooling/hot-reload-gradle-idea.jar \
    && echo "$COMPOSE_HOT_RELOAD_IDEA_SHA256  /opt/jetbrains-tooling/hot-reload-gradle-idea.jar" | sha256sum -c -
ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk
RUN curl -fsSL "$ANDROID_TOOLS_URL" -o /tmp/android-tools.zip \
    && echo "$ANDROID_TOOLS_SHA256  /tmp/android-tools.zip" | sha256sum -c - \
    && mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools" \
    && unzip -q /tmp/android-tools.zip -d /tmp/android-tools \
    && mv /tmp/android-tools/cmdline-tools "$ANDROID_SDK_ROOT/cmdline-tools/latest" \
    && (yes | "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null || [[ $? -eq 141 ]])
RUN "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" \
      "platform-tools" "platforms;android-34" "platforms;android-36" \
      "build-tools;34.0.0" "build-tools;36.0.0" \
    && rm -rf /tmp/android-tools /tmp/android-tools.zip /root/.android

RUN groupadd --gid 1000 dev \
    && useradd --uid 1000 --gid dev --create-home --shell /bin/bash dev \
    && mkdir -p /home/dev/.gradle /home/dev/.konan /home/dev/.npm /home/dev/composites \
    && ln -s /opt/android-sdk /home/dev/.androidsdk \
    && if [[ -n "$HOST_USER" && "$HOST_USER" != dev ]]; then \
         [[ "$HOST_USER" =~ ^[A-Za-z0-9._-]+$ ]] \
         || { printf 'Invalid HOST_USER build argument: %s\n' "$HOST_USER" >&2; exit 1; }; \
         mkdir -p "/home/$HOST_USER"; \
         ln -s /opt/android-sdk "/home/$HOST_USER/.androidsdk"; \
       fi \
    && chown -R dev:dev /home/dev /opt/android-sdk \
    && chmod -R a+rwX /opt/android-sdk

COPY .devcontainer/scripts/setup-workspace.sh /usr/local/bin/wallet-setup-workspace
COPY .devcontainer/scripts/wallet-import-debug-keystore /usr/local/bin/wallet-import-debug-keystore
RUN chmod 0755 /usr/local/bin/wallet-setup-workspace /usr/local/bin/wallet-import-debug-keystore

# Container-only environment may use the real path; nothing outside the container reads it.
# sdk.dir in local.properties is a different matter and must NOT use this path: that file is
# read by container-side Gradle *and* validated by the host-side IntelliJ, which rewrites it
# when the directory does not exist on the host. See the /home/$HOST_USER/.androidsdk symlink
# above and prepare_properties_file in setup-workspace.sh.
ENV ANDROID_HOME=/opt/android-sdk ANDROID_SDK_ROOT=/opt/android-sdk
ENV JAVA_HOME=/opt/java/temurin-17 \
    JDK_17=/opt/java/temurin-17 \
    JDK_21=/opt/java/temurin-21 \
    GRADLE_USER_HOME=/home/dev/.gradle \
    KONAN_DATA_DIR=/home/dev/.konan \
    PATH=/opt/java/temurin-17/bin:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:/usr/local/bin:/usr/bin:/bin
ENV GRADLE_OPTS=-Dorg.gradle.daemon=false
# The Compose Hot Reload IDEA model stays available under /opt/jetbrains-tooling, but it must
# never be placed on the bootstrap class path. It depends on kotlin-stdlib and
# kotlinx-serialization, which the bootstrap loader cannot see, so -Xbootclasspath/a: both
# breaks linkage and makes the JVM silently drop the class's @kotlin.Metadata annotation.
# kotlin-reflect then reports properties as unresolved during Gradle sync. If IntelliJ's
# Gradle target proxy really is missing this model, add it to the Gradle class path with an
# init script instead.
RUN printf '%s\n' \
    "export ANDROID_HOME=/opt/android-sdk" \
    "export ANDROID_SDK_ROOT=/opt/android-sdk" \
    "export PATH=\$JAVA_HOME/bin:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:\$PATH" \
    > /etc/profile.d/wallet-toolchain.sh
USER dev
