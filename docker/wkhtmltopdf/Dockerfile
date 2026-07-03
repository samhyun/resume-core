FROM debian:12-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        fontconfig \
        fonts-dejavu \
        fonts-freefont-ttf \
        fonts-noto-cjk \
        wkhtmltopdf \
    && rm -rf /var/lib/apt/lists/*

ENTRYPOINT ["/bin/sh", "-c", "sleep infinity"]
