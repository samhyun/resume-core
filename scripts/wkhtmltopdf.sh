#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE=${COMPOSE_FILE:-docker/docker-compose.yml}
SERVICE=${WKHTMLTOPDF_SERVICE:-wkhtmltopdf}
CONTAINER_DIR=${WKHTMLTOPDF_CONTAINER_DIR:-/tmp/resume-pdf}

if [ "$#" -lt 2 ]; then
  echo "Usage: ${0} [wkhtmltopdf options] <input-html> <output-pdf>" >&2
  exit 1
fi

# Capture all arguments so we can split options vs files
args=("$@")
arg_count=${#args[@]}
input_path=${args[$((arg_count-2))]}
output_path=${args[$((arg_count-1))]}
option_count=$((arg_count-2))
options=()
for ((i=0; i<option_count; i++)); do
  options+=("${args[$i]}")
done

abs_path() {
  local target="$1"
  if [ -d "$target" ]; then
    (cd "$target" && pwd)
  else
    local dir
    dir=$(cd "$(dirname "$target")" && pwd)
    echo "$dir/$(basename "$target")"
  fi
}

input_abs=$(abs_path "$input_path")
output_abs=$(abs_path "$output_path")
output_dir=$(dirname "$output_abs")

if [ ! -f "$input_abs" ]; then
  echo "Input HTML file not found: $input_abs" >&2
  exit 1
fi

mkdir -p "$output_dir"
rm -f "$output_abs"

compose() {
  docker compose -f "$COMPOSE_FILE" "$@"
}

container_id=$(compose ps -q "$SERVICE")
if [ -z "$container_id" ]; then
  echo "Service '$SERVICE' is not running. Start it with 'docker compose -f $COMPOSE_FILE up -d $SERVICE'" >&2
  exit 1
fi

unique=$(uuidgen 2>/dev/null || date +%s%N)
container_input="$CONTAINER_DIR/${unique}.html"
container_output="$CONTAINER_DIR/${unique}.pdf"

compose exec -T "$SERVICE" sh -c "mkdir -p '$CONTAINER_DIR'"
compose cp "$input_abs" "$SERVICE:$container_input"
compose exec -T "$SERVICE" wkhtmltopdf "${options[@]}" "$container_input" "$container_output"
compose cp "$SERVICE:$container_output" "$output_abs"
compose exec -T "$SERVICE" sh -c "rm -f '$container_input' '$container_output'"
