ARG TARGET_FRAMEWORK
FROM mcr.microsoft.com/dotnet/sdk:${TARGET_FRAMEWORK}-alpine as build-env
ARG PROGRAM_FILE
ARG PACKAGES_FILE

WORKDIR /sln

RUN dotnet new console -n Project -o ./ --no-restore

COPY --chmod=0755 ./install-packages.sh ./
COPY $PACKAGES_FILE ./packages.txt
RUN ./install-packages.sh ./packages.txt

COPY $PROGRAM_FILE ./Program.cs

RUN dotnet build ./Project.csproj -c Release --no-restore

RUN dotnet publish ./Project.csproj -o ./published -c Release --no-build

FROM mcr.microsoft.com/dotnet/runtime:${TARGET_FRAMEWORK}-alpine
WORKDIR /app

COPY --from=build-env ./sln/published ./

ENTRYPOINT ["./Project"]