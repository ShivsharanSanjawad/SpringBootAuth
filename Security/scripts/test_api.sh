#!/usr/bin/env bash
set -e
set -u

BASE_URL="${BASE_URL:-http://localhost:8080}"

wait_for_server() {
  printf "\nWaiting for server on %s...\n" "$BASE_URL"
  for i in {1..30}; do
    STATUS=$(curl -sS -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/login" \
      -H "Content-Type: application/json" \
      -d "{\"username\":\"_\",\"password\":\"_\"}" || true)
    if [ "$STATUS" != "000" ]; then
      echo "Server is up (HTTP $STATUS)"
      return 0
    fi
    sleep 1
  done
  echo "Server not reachable after 30s. Start it with: .\\mvnw spring-boot:run"
  exit 1
}

read -r -p "Username: " USERNAME
read -r -p "Password: " PASSWORD
read -r -p "Email: " EMAIL

wait_for_server

printf "\n== Sign Up ==\n"
curl -sS -w "\n[HTTP %{http_code}]\n" -X POST "$BASE_URL/api/signUp" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\",\"email\":\"$EMAIL\"}" \
  | cat

printf "\n\n== Login (get tokens) ==\n"
LOGIN_RESPONSE=$(curl -sS -w "\n[HTTP %{http_code}]\n" -X POST "$BASE_URL/api/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")

echo "$LOGIN_RESPONSE"

printf "\nPaste accessToken from response: "
read -r ACCESS_TOKEN

printf "\nPaste refreshToken from response: "
read -r REFRESH_TOKEN

printf "\n== Get User Info ==\n"
curl -sS -w "\n[HTTP %{http_code}]\n" -X GET "$BASE_URL/api/user-info" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  | cat

printf "\n\n== Update Profile ==\n"
read -r -p "Description (optional): " DESCRIPTION
read -r -p "College (SPIT, ATHRAV COLLEGE, DJ, VJTI, KJ): " COLLEGE
read -r -p "Gender (MALE, FEMALE, OTHER): " GENDER
read -r -p "Image path (optional): " IMAGE_PATH

if [ -n "$IMAGE_PATH" ]; then
  curl -sS -w "\n[HTTP %{http_code}]\n" -X POST "$BASE_URL/api/profile" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -F "description=$DESCRIPTION" \
    -F "college=$COLLEGE" \
    -F "gender=$GENDER" \
    -F "image=@$IMAGE_PATH" \
    | cat
else
  curl -sS -w "\n[HTTP %{http_code}]\n" -X POST "$BASE_URL/api/profile" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -F "description=$DESCRIPTION" \
    -F "college=$COLLEGE" \
    -F "gender=$GENDER" \
    | cat
fi

printf "\n\n== Refresh Token ==\n"
curl -sS -w "\n[HTTP %{http_code}]\n" -X POST "$BASE_URL/api/refresh-token" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}" \
  | cat

printf "\n\nDone.\n"
