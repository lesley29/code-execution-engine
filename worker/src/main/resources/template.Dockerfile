ARG TARGET_FRAMEWORK
FROM mcr.microsoft.com/dotnet/sdk:${TARGET_FRAMEWORK}-alpine as build-env

WORKDIR /sln

RUN dotnet new console -n Project -o ./

COPY ./packages.txt ./
RUN \
    while IFS='' read -r LINE || [ -n "${LINE}" ]; do \
        package=${LINE% *}; \
        version=${LINE#* }; \
        dotnet add package "$package" -v "$version"; \
    done < ./packages.txt

COPY ./Program.cs ./

RUN dotnet build ./Project.csproj -c Release --no-restore

RUN dotnet publish ./Project.csproj -o ./published -c Release --no-build

FROM mcr.microsoft.com/dotnet/runtime:${TARGET_FRAMEWORK}-alpine
WORKDIR /app

COPY --from=build-env ./sln/published ./

RUN addgroup -S worker && adduser -G worker -S worker -D -H
USER worker

ENTRYPOINT ["./Project"]