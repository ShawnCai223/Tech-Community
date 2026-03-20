#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-mysql-community}"
REDIS_CONTAINER="${REDIS_CONTAINER:-redis-community}"
REDIS_DB="${REDIS_DB:-11}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

mysql_query() {
  local sql="$1"
  docker exec "$MYSQL_CONTAINER" mysql -N -uroot -pMQKY7CJx community -e "$sql"
}

redis_cmd() {
  docker exec "$REDIS_CONTAINER" redis-cli -n "$REDIS_DB" "$@"
}

require_cmd docker

users=()
while IFS= read -r user_id; do
  [[ -n "$user_id" ]] || continue
  users+=("$user_id")
done < <(mysql_query "SELECT id FROM user WHERE id <> 1 AND status = 1 ORDER BY id;" | tr -d '\r')

if ((${#users[@]} < 2)); then
  echo "Not enough active users to seed likes." >&2
  exit 1
fi

while IFS= read -r key; do
  [[ -n "$key" ]] || continue
  :
done < <(redis_cmd --scan --pattern 'like:*')

user_count=${#users[@]}
redis_batch_file="$(mktemp)"
trap 'rm -f "$redis_batch_file"' EXIT

while IFS= read -r key; do
  [[ -n "$key" ]] || continue
  printf 'DEL %s\n' "$key" >>"$redis_batch_file"
done < <(redis_cmd --scan --pattern 'like:*')

seed_entity_likes() {
  local entity_type="$1"
  local entity_id="$2"
  local author_id="$3"
  local base_count="$4"
  local boost="$5"

  local like_count=$((base_count + boost))
  local max_likes=$((user_count - 1))
  local index added user_id

  if (( like_count > max_likes )); then
    like_count=$max_likes
  fi
  if (( like_count < 1 )); then
    like_count=1
  fi

  index=$(( entity_id % user_count ))
  added=0

  while (( added < like_count )); do
    user_id="${users[$index]}"
    if [[ "$user_id" != "$author_id" ]]; then
      printf 'SADD like:entity:%s:%s %s\n' "$entity_type" "$entity_id" "$user_id" >>"$redis_batch_file"
      printf 'INCR like:user:%s\n' "$author_id" >>"$redis_batch_file"
      added=$((added + 1))
    fi
    index=$(( (index + 1) % user_count ))
  done
}

while IFS=$'\t' read -r post_id author_id post_type post_status comment_count; do
  [[ -n "$post_id" ]] || continue

  like_count=$(( (comment_count / 2) + 1 + (post_id % 5) ))
  if (( post_status == 1 )); then
    like_count=$((like_count + 8))
  fi
  if (( post_type == 1 )); then
    like_count=$((like_count + 10))
  fi
  if (( post_status == 2 )); then
    like_count=$(( (comment_count / 3) + 1 ))
  fi

  seed_entity_likes 1 "$post_id" "$author_id" "$like_count" 0
done < <(mysql_query "SELECT id, user_id, type, status, comment_count FROM discuss_post ORDER BY id;" | tr -d '\r')

while IFS=$'\t' read -r comment_id author_id entity_type entity_id target_id; do
  [[ -n "$comment_id" ]] || continue

  base_count=$(( 1 + (comment_id % 3) ))
  boost=0

  if (( entity_type == 1 )); then
    boost=$(( entity_id % 4 ))
  else
    boost=$(( target_id % 2 ))
  fi

  seed_entity_likes 2 "$comment_id" "$author_id" "$base_count" "$boost"
done < <(mysql_query "SELECT id, user_id, entity_type, entity_id, target_id FROM comment ORDER BY id;" | tr -d '\r')

docker exec -i "$REDIS_CONTAINER" redis-cli -n "$REDIS_DB" <"$redis_batch_file" >/dev/null

echo "Seeded post and comment likes in Redis DB ${REDIS_DB}."
