#!/bin/sh

set -- "mcr.microsoft.com/dotnet/sdk:6.0-alpine" \
  "mcr.microsoft.com/dotnet/runtime:6.0-alpine"

for image in "$@"; do
  docker image pull "$image"
done