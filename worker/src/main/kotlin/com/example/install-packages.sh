#!/bin/sh

file_with_packages_to_add=$1
while IFS='' read -r LINE || [ -n "${LINE}" ]; do
    package=${LINE% *}
    version=${LINE#* }
    dotnet add package "$package" -v "$version"
done < "$file_with_packages_to_add"
