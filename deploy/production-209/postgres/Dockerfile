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
# This image's postgres binary was itself built --with-llvm (CLANG=clang-19,
# LLVM_CONFIG=llvm19), so PGXS always builds pgvector's JIT bitcode as part of
# `install` regardless of a `with_llvm=no` override on the make command line —
# that override does not actually suppress it (confirmed by trial). So instead
# of fighting it, just install the matching clang19/llvm19 packages the build
# already expects.
FROM postgres:17-alpine

RUN apk add --no-cache --virtual .pgvector-build-deps build-base git clang19 llvm19 \
    && git clone --branch v0.8.1 https://github.com/pgvector/pgvector.git /tmp/pgvector \
    && cd /tmp/pgvector \
    && make OPTFLAGS='' \
    && make install \
    && cd / \
    && rm -rf /tmp/pgvector \
    && apk del .pgvector-build-deps
