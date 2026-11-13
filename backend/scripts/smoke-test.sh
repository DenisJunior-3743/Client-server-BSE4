#!/usr/bin/env bash
# End-to-end smoke test of the loan workflow against a running backend (dev profile).
# Walks ONE loan from registration to the first repayment and checks the three guarantees:
#   at-most-once (Idempotency-Key), optimistic locking (409 on a stale version), state workflow.
#
# Usage:   ./scripts/smoke-test.sh [base-url]        default http://localhost:8080
# Needs:   curl, jq.  The server must run with the dev profile (OTP echo + demo staff accounts).
set -euo pipefail

BASE="${1:-http://localhost:8080}"
API="$BASE/api/v1"
PASS="Passw0rd1"
STAMP="$(date +%s)"
EMAIL="smoke$STAMP@example.com"
PHONE="07${STAMP: -8}"
NIN="CM$(printf '%012d' "$STAMP")"
uuid() { cat /proc/sys/kernel/random/uuid 2>/dev/null || uuidgen; }
say() { printf '\n== %s\n' "$*"; }
ok() { printf '   OK   %s\n' "$*"; }
fail() { printf '   FAIL %s\n' "$*"; exit 1; }

# json METHOD URL TOKEN [BODY] [IDEMPOTENCY_KEY]  -> prints "<body>\n<http status>"
json() {
  local method="$1" url="$2" token="$3" body="${4:-}" key="${5:-}"
  local args=(-sS -X "$method" "$url" -H 'Accept: application/json' -w '\n%{http_code}')
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$body" ] && args+=(-H 'Content-Type: application/json' -d "$body")
  [ -n "$key" ] && args+=(-H "Idempotency-Key: $key")
  curl "${args[@]}"
}
body() { sed '$d' <<<"$1"; }
code() { tail -n1 <<<"$1"; }
expect() { [ "$(code "$1")" = "$2" ] || { body "$1"; fail "$3 (expected HTTP $2, got $(code "$1"))"; }; }
login() { body "$(json POST "$API/auth/login" "" "{\"email\":\"$1\",\"password\":\"$PASS\"}")" | jq -r .accessToken; }
version() { body "$(json GET "$API/applications/$1" "$2")" | jq -r .application.version; }
status() { body "$(json GET "$API/applications/$1" "$2")" | jq -r .application.status; }

say "Health"
curl -sS "$BASE/actuator/health" | jq -c . || fail "server not reachable at $BASE"

say "1. Borrower registers, verifies the one-time code, signs in"
R="$(json POST "$API/auth/register" "" "{\"fullName\":\"Smoke Test\",\"email\":\"$EMAIL\",\"phone\":\"$PHONE\",\"password\":\"$PASS\"}")"
expect "$R" 201 "register"
OTP="$(body "$R" | jq -r .devOtp)"
[ "$OTP" != "null" ] || fail "no devOtp in the reply: is the server running with the dev profile?"
R="$(json POST "$API/auth/login" "" "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}")"
expect "$R" 403 "login before verification must be refused"; ok "unverified account cannot sign in"
R="$(json POST "$API/auth/otp/verify" "" "{\"email\":\"$EMAIL\",\"code\":\"$OTP\"}")"
expect "$R" 204 "verify code"
BORROWER="$(login "$EMAIL")"; [ -n "$BORROWER" ] && [ "$BORROWER" != "null" ] || fail "borrower login"
ok "registered, verified, signed in"

say "2. Borrower completes the profile and submits a loan (twice, same Idempotency-Key)"
R="$(json PUT "$API/me/profile" "$BORROWER" "{\"nationalId\":\"$NIN\",\"dateOfBirth\":\"1995-04-12\",\"employmentInfo\":{\"employer\":\"MUST\",\"jobTitle\":\"Lecturer\",\"employmentType\":\"FORMAL\"},\"incomeMonthly\":3000000,\"address\":{\"district\":\"Mbarara\",\"city\":\"Mbarara\",\"street\":\"Plot 1 High Street\"}}")"
expect "$R" 200 "save profile"
PRODUCT="$(body "$(json GET "$API/products" "$BORROWER")" | jq -r '[.[] | select(.type=="PERSONAL")][0].id')"
KEY="$(uuid)"
LOAN_BODY="{\"productId\":\"$PRODUCT\",\"amountRequested\":1000000,\"termMonths\":12,\"purpose\":\"School fees for the coming term\"}"
R1="$(json POST "$API/applications" "$BORROWER" "$LOAN_BODY" "$KEY")"; expect "$R1" 201 "first submit"
R2="$(json POST "$API/applications" "$BORROWER" "$LOAN_BODY" "$KEY")"; expect "$R2" 200 "retried submit"
LOAN="$(body "$R1" | jq -r .id)"
[ "$LOAN" = "$(body "$R2" | jq -r .id)" ] || fail "the retry created a different loan"
COUNT="$(body "$(json GET "$API/applications" "$BORROWER")" | jq length)"
[ "$COUNT" = "1" ] || fail "expected exactly 1 loan, found $COUNT"
ok "at-most-once: two requests, one loan ($(body "$R1" | jq -r .reference))"
R="$(json POST "$API/applications" "$BORROWER" "${LOAN_BODY/1000000/2000000}" "$KEY")"
expect "$R" 422 "same key with a different request must be refused"; ok "key reuse for a different request refused"

say "3. Officer forwards for credit check; the background worker scores it"
OFFICER="$(login grace.officer@bankloan.ug)"
R="$(json POST "$API/applications/$LOAN/status" "$OFFICER" "{\"status\":\"CREDIT_CHECK_PENDING\",\"version\":$(version "$LOAN" "$OFFICER")}")"
expect "$R" 200 "forward for credit check"
for _ in 1 2 3 4 5 6 7 8; do sleep 1; S="$(status "$LOAN" "$OFFICER")"; [ "$S" = "CREDIT_CHECK_PENDING" ] || break; done
[ "$S" = "UNDER_REVIEW" ] || fail "expected UNDER_REVIEW after the credit check, got $S"
ok "request returned at once; worker moved the loan to UNDER_REVIEW"

say "4. Two managers decide on the same version: one wins, one gets 409"
MANAGER="$(login esther.manager@bankloan.ug)"
V="$(version "$LOAN" "$MANAGER")"
DECISION="{\"decision\":\"APPROVED\",\"comments\":\"Income and documents verified\",\"version\":$V}"
R="$(json POST "$API/applications/$LOAN/decision" "$MANAGER" "$DECISION")"; expect "$R" 200 "approve"
R="$(json POST "$API/applications/$LOAN/decision" "$MANAGER" "${DECISION/APPROVED/REJECTED}")"
expect "$R" 409 "stale decision must conflict"
[ "$(body "$R" | jq -r .code)" = "VERSION_CONFLICT" ] || fail "expected VERSION_CONFLICT"
ok "optimistic lock: second decision on version $V refused (current is $(body "$R" | jq -r .currentVersion))"

say "5. Borrower accepts the offer before its deadline"
R="$(json POST "$API/applications/$LOAN/offer/accept" "$BORROWER" "" "$(uuid)")"; expect "$R" 201 "accept offer"
ok "offer accepted (expires $(body "$R" | jq -r .offerExpiresAt))"

say "6. Manager disburses (retry with the same key pays out once)"
DKEY="$(uuid)"
DBODY="{\"method\":\"MOBILE_MONEY\",\"version\":$(version "$LOAN" "$MANAGER")}"
R="$(json POST "$API/applications/$LOAN/disbursement" "$MANAGER" "$DBODY" "$DKEY")"; expect "$R" 201 "disburse"
R="$(json POST "$API/applications/$LOAN/disbursement" "$MANAGER" "$DBODY" "$DKEY")"; expect "$R" 200 "retried disburse"
[ "$(status "$LOAN" "$MANAGER")" = "ACTIVE" ] || fail "loan should be ACTIVE"
ok "funds released once; loan is ACTIVE"

say "7. Borrower sees the schedule and pays the first installment"
SCHEDULE="$(body "$(json GET "$API/applications/$LOAN/schedule" "$BORROWER")")"
DUE="$(jq -r '.[0].amountDue' <<<"$SCHEDULE")"
PKEY="$(uuid)"
PBODY="{\"amount\":$DUE,\"method\":\"MOBILE_MONEY\"}"
R="$(json POST "$API/applications/$LOAN/repayments" "$BORROWER" "$PBODY" "$PKEY")"; expect "$R" 201 "repay"
B1="$(body "$R" | jq -r .outstandingBalance)"
R="$(json POST "$API/applications/$LOAN/repayments" "$BORROWER" "$PBODY" "$PKEY")"; expect "$R" 200 "retried repay"
[ "$B1" = "$(body "$R" | jq -r .outstandingBalance)" ] || fail "the retry changed the balance"
ok "$(jq length <<<"$SCHEDULE") installments; paid $DUE once; balance $B1"

say "8. Authorization: a borrower token cannot take a staff action"
R="$(json POST "$API/applications/$LOAN/decision" "$BORROWER" "$DECISION")"; expect "$R" 403 "borrower must be forbidden"
ok "borrower refused on a staff endpoint"

printf '\nAll checks passed.\n'
