"""Development API smoke test; creates borrower and loan records. Python 3, standard library only."""
import concurrent.futures
import json
import sys
import time
import uuid
import urllib.error
import urllib.request

BASE = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080").rstrip("/")
PASSWORD = "Passw0rd1"


def call(method, path, token=None, body=None, key=None, expected=200):
    headers = {"Accept": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    if key:
        headers["Idempotency-Key"] = key
    data = None if body is None else json.dumps(body).encode()
    if data is not None:
        headers["Content-Type"] = "application/json"
    request = urllib.request.Request(BASE + "/api/v1" + path, data=data, headers=headers, method=method)
    for attempt in range(3):
        try:
            response = urllib.request.urlopen(request, timeout=40)
        except urllib.error.HTTPError as error:
            response = error
        raw = response.read()
        result = json.loads(raw) if raw else None
        status = response.code
        if status == 503 and (key or method == "GET" or path == "/auth/login") and attempt < 2:
            time.sleep(2)
            continue
        allowed = (expected,) if isinstance(expected, int) else expected
        assert status in allowed, f"{method} {path}: HTTP {status}, code={result.get('code') if isinstance(result, dict) else 'unknown'}"
        return result, status
    raise AssertionError("Request did not complete")


def login(email):
    return call("POST", "/auth/login", body={"email": email, "password": PASSWORD})[0]["accessToken"]


def detail(loan, token):
    return call("GET", "/applications/" + loan, token)[0]["application"]


def main():
    identity = uuid.uuid4().hex
    email = "smoke-" + identity + "@example.com"
    phone = "07" + str(int(identity[:12], 16) % 100000000).zfill(8)
    created, _ = call("POST", "/auth/register", body={"fullName": "Integration Test", "email": email, "phone": phone, "password": PASSWORD}, expected=201)
    assert created.get("devOtp"), "Use a development API with OTP echo enabled"
    call("POST", "/auth/login", body={"email": email, "password": PASSWORD}, expected=403)
    call("POST", "/auth/otp/verify", body={"email": email, "code": created["devOtp"]}, expected=204)
    borrower = login(email)
    print("PASS registration, phone verification and login", flush=True)
    profile = {"nationalId": "CM" + identity[:12].upper(), "dateOfBirth": "1995-04-12", "employmentInfo": {"employer": "MUST", "jobTitle": "Lecturer", "employmentType": "FORMAL"}, "incomeMonthly": 3000000, "address": {"district": "Mbarara", "city": "Mbarara", "street": "Plot 1 High Street"}}
    call("PUT", "/me/profile", borrower, profile)
    products, _ = call("GET", "/products", borrower)
    product = next(item for item in products if item["type"] == "PERSONAL")
    draft = {"productId": product["id"], "amountRequested": 1000000, "termMonths": 12, "purpose": "School fees for the coming term"}
    key = str(uuid.uuid4())
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
        futures = [executor.submit(call, "POST", "/applications", borrower, draft, key, (200, 201)) for _ in range(2)]
        replies = [future.result() for future in futures]
    assert sorted(status for _, status in replies) == [200, 201]
    loan = replies[0][0]["id"]
    assert all(reply["id"] == loan for reply, _ in replies)
    assert len(call("GET", "/applications", borrower)[0]) == 1
    call("POST", "/applications", borrower, dict(draft, amountRequested=2000000), key, expected=422)
    print("PASS concurrent duplicate submission, one loan and cached reply", flush=True)
    officer = login("grace.officer@bankloan.ug")
    call("POST", f"/applications/{loan}/status", officer, {"status": "CREDIT_CHECK_PENDING", "version": detail(loan, officer)["version"]})
    for _ in range(30):
        application = detail(loan, officer)
        if application["status"] != "CREDIT_CHECK_PENDING":
            break
        time.sleep(1)
    assert application["status"] == "UNDER_REVIEW", "Background credit check did not reach review"
    print("PASS asynchronous credit check", flush=True)
    manager = login("esther.manager@bankloan.ug")
    decision = {"decision": "APPROVED", "comments": "Income verified in integration test", "version": detail(loan, manager)["version"]}
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
        futures = [executor.submit(call, "POST", f"/applications/{loan}/decision", manager, decision, expected=(200, 409)) for _ in range(2)]
        results = [future.result() for future in futures]
    assert sorted(status for _, status in results) == [200, 409]
    conflict = next(body for body, status in results if status == 409)
    assert conflict["code"] == "VERSION_CONFLICT"
    print("PASS concurrent approvals: one success and one version conflict", flush=True)
    call("POST", f"/applications/{loan}/offer/accept", borrower, key=str(uuid.uuid4()), expected=201)
    disbursement = {"method": "MOBILE_MONEY", "version": detail(loan, manager)["version"]}
    disburse_key = str(uuid.uuid4())
    call("POST", f"/applications/{loan}/disbursement", manager, disbursement, disburse_key, expected=201)
    call("POST", f"/applications/{loan}/disbursement", manager, disbursement, disburse_key)
    assert detail(loan, manager)["status"] == "ACTIVE"
    schedule, _ = call("GET", f"/applications/{loan}/schedule", borrower)
    payment = {"amount": schedule[0]["amountDue"], "method": "MOBILE_MONEY"}
    payment_key = str(uuid.uuid4())
    receipt, _ = call("POST", f"/applications/{loan}/repayments", borrower, payment, payment_key, expected=201)
    retry, _ = call("POST", f"/applications/{loan}/repayments", borrower, payment, payment_key)
    assert receipt == retry
    print("PASS offer acceptance, disbursement and repayment replay", flush=True)
    call("POST", f"/applications/{loan}/decision", borrower, decision, expected=403)
    notices, _ = call("GET", "/me/notifications", borrower)
    assert notices, "Expected asynchronous borrower notifications"
    admin = login("admin@bankloan.ug")
    audits, _ = call("GET", "/audit-logs", admin)
    assert any(item.get("entityId") == loan for item in audits), "Expected asynchronous loan audit entries"
    print("PASS role separation, notifications and audit persistence", flush=True)
    print("All Neon API smoke checks passed. Development records remain in the database.", flush=True)


if __name__ == "__main__":
    main()
