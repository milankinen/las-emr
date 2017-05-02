#!/usr/bin/env bash
set -euo pipefail

./lein deploy-binaries ${1:-}

