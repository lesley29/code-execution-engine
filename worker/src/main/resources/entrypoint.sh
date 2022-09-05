#!/bin/sh

timeout -k 1 "${TIMEOUT:-5}" ./Project "$@"