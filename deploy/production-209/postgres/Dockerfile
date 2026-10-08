# Postgres 17 (Alpine) with the pgvector extension built in — required by
# V561+ (document_embeddings / OC-277 RAG pipeline). Local dev uses the
# pgvector/pgvector:pg17 image, but 209 runs plain postgres:17-alpine, so the
# extension must be added here.
#
# Built from source against this image's own /usr/local Postgres build, not
# Alpine's apk `postgresql17` package — Alpine's `postgresql-pgvector` apk
# package tracks a different Postgres major version than this image ships
# and installs to the wrong extension directory.
#
# JIT bitcode build is skipped (with_llvm=no): it needs a clang version this
# image doesn't have, and it's only a query-JIT optimization, not required
# for the extension itself to load and work.
FROM postgres:17-alpine

RUN apk add --no-cache --virtual .pgvector-build-deps build-base git \
    && git clone --branch v0.8.1 https://github.com/pgvector/pgvector.git /tmp/pgvector \
    && cd /tmp/pgvector \
    && make OPTFLAGS='' \
    && make install with_llvm=no \
    && cd / \
    && rm -rf /tmp/pgvector \
    && apk del .pgvector-build-deps
