#!/usr/bin/env bash
# Seeds realistic like data into production Redis.
# Expects COMMUNITY_DATASOURCE_URL etc. to be set in the environment
# (source .env.prod before running, or let the workflow handle it).
set -eo pipefail

# Try to load .env.prod if not already sourced
if [ -z "${COMMUNITY_DATASOURCE_URL:-}" ]; then
  for candidate in /var/www/tech-community/.env.prod .env.prod; do
    if [ -f "$candidate" ]; then
      set -a
      source "$candidate"
      set +a
      break
    fi
  done
fi

if [ -z "${COMMUNITY_DATASOURCE_URL:-}" ]; then
  echo "ERROR: COMMUNITY_DATASOURCE_URL is not set. Source .env.prod first." >&2
  exit 1
fi

# Parse DB connection from JDBC URL
db_host=$(printf '%s' "$COMMUNITY_DATASOURCE_URL" | sed -E 's#jdbc:mysql://([^:/?]+)(:([0-9]+))?/([^?]+).*$#\1#')
db_port=$(printf '%s' "$COMMUNITY_DATASOURCE_URL" | sed -nE 's#jdbc:mysql://[^:/?]+:([0-9]+)/[^?]+.*#\1#p')
db_name=$(printf '%s' "$COMMUNITY_DATASOURCE_URL" | sed -E 's#jdbc:mysql://[^/]+/([^?]+).*$#\1#')
db_user="$COMMUNITY_DATASOURCE_USERNAME"
db_pass="$COMMUNITY_DATASOURCE_PASSWORD"

redis_host="${COMMUNITY_REDIS_HOST:-localhost}"
redis_port="${COMMUNITY_REDIS_PORT:-6379}"
redis_db="${COMMUNITY_REDIS_DATABASE:-0}"
redis_pass="${COMMUNITY_REDIS_PASSWORD:-}"

if [ -z "$db_port" ]; then
  db_port=3306
fi

mysql_query() {
  mysql -N -h"$db_host" -P"$db_port" -u"$db_user" -p"$db_pass" "$db_name" -e "$1"
}

redis_cmd() {
  if [ -n "$redis_pass" ]; then
    redis-cli -h "$redis_host" -p "$redis_port" -n "$redis_db" -a "$redis_pass" --no-auth-warning "$@"
  else
    redis-cli -h "$redis_host" -p "$redis_port" -n "$redis_db" "$@"
  fi
}

echo "Fetching active users..."
users=()
while IFS= read -r user_id; do
  [[ -n "$user_id" ]] || continue
  users+=("$user_id")
done < <(mysql_query "SELECT id FROM user WHERE id <> 1 AND status = 1 ORDER BY id;" | tr -d '\r')

user_count=${#users[@]}
echo "Found $user_count active users."

if ((user_count < 2)); then
  echo "Not enough active users to seed likes." >&2
  exit 1
fi

# Clear existing like data
echo "Clearing old like data..."
while IFS= read -r key; do
  [[ -n "$key" ]] || continue
  redis_cmd DEL "$key" >/dev/null
done < <(redis_cmd --scan --pattern 'like:*')

redis_batch_file="$(mktemp)"
trap 'rm -f "$redis_batch_file"' EXIT

seed_entity_likes() {
  local entity_type="$1"
  local entity_id="$2"
  local author_id="$3"
  local base_count="$4"
  local boost="$5"

  local like_count=$((base_count + boost))
  local max_likes=$((user_count - 1))

  if (( like_count > max_likes )); then
    like_count=$max_likes
  fi
  if (( like_count < 0 )); then
    like_count=0
  fi

  local index=$(( entity_id % user_count ))
  local added=0

  while (( added < like_count )); do
    local user_id="${users[$index]}"
    if [[ "$user_id" != "$author_id" ]]; then
      printf 'SADD like:entity:%s:%s %s\n' "$entity_type" "$entity_id" "$user_id" >>"$redis_batch_file"
      printf 'INCR like:user:%s\n' "$author_id" >>"$redis_batch_file"
      added=$((added + 1))
    fi
    index=$(( (index + 1) % user_count ))
  done
}

echo "Seeding post likes..."
post_count=0
while IFS=$'\t' read -r post_id author_id post_type post_status comment_count; do
  [[ -n "$post_id" ]] || continue

  # More generous like counts for a lively forum
  like_count=$(( (comment_count * 2) + 3 + (post_id % 7) ))

  # Boost featured/pinned posts
  if (( post_status == 1 )); then
    like_count=$((like_count + 12))
  fi
  if (( post_type == 1 )); then
    like_count=$((like_count + 15))
  fi
  # Less likes for deleted posts
  if (( post_status == 2 )); then
    like_count=$(( (comment_count / 3) + 1 ))
  fi

  seed_entity_likes 1 "$post_id" "$author_id" "$like_count" 0
  post_count=$((post_count + 1))
done < <(mysql_query "SELECT id, user_id, type, status, comment_count FROM discuss_post WHERE status != 2 ORDER BY id;" | tr -d '\r')

echo "Seeding comment likes..."
comment_count=0
while IFS=$'\t' read -r comment_id author_id entity_type entity_id target_id; do
  [[ -n "$comment_id" ]] || continue

  # More generous comment likes
  base_count=$(( 2 + (comment_id % 5) ))
  boost=0

  if (( entity_type == 1 )); then
    boost=$(( entity_id % 6 ))
  else
    boost=$(( 1 + target_id % 3 ))
  fi

  seed_entity_likes 2 "$comment_id" "$author_id" "$base_count" "$boost"
  comment_count=$((comment_count + 1))
done < <(mysql_query "SELECT id, user_id, entity_type, entity_id, target_id FROM comment WHERE status = 0 ORDER BY id;" | tr -d '\r')

echo "Writing ${post_count} post + ${comment_count} comment like entries to Redis..."
if [ -n "$redis_pass" ]; then
  redis-cli -h "$redis_host" -p "$redis_port" -n "$redis_db" -a "$redis_pass" --no-auth-warning <"$redis_batch_file" >/dev/null
else
  redis-cli -h "$redis_host" -p "$redis_port" -n "$redis_db" <"$redis_batch_file" >/dev/null
fi

echo "Done! Seeded likes for $post_count posts and $comment_count comments."
