#!/usr/bin/env bash
# Deletes every compiled .class file under this folder (all levels)

cd "$(dirname "$0")" || exit 1
find . -name "*.class" -type f -not -path "./.git/*" -print -delete | sed 's/^/removed /'
