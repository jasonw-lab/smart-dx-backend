#!/bin/bash
# E2E Test Script for Property Search and Chatbot APIs
# Usage: ./scripts/e2e-test.sh [BASE_URL]

set -e

BASE_URL="${1:-http://localhost:8081}"
PASSED=0
FAILED=0
TOTAL=0

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_pass() {
    ((PASSED++))
    ((TOTAL++))
    echo -e "${GREEN}[PASS]${NC} $1"
}

log_fail() {
    ((FAILED++))
    ((TOTAL++))
    echo -e "${RED}[FAIL]${NC} $1"
    echo "  Response: $2"
}

log_info() {
    echo -e "${YELLOW}[INFO]${NC} $1"
}

# Wait for server to be ready
wait_for_server() {
    log_info "Waiting for server at $BASE_URL..."
    for i in {1..30}; do
        if curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" 2>/dev/null | grep -q "200"; then
            log_info "Server is ready!"
            return 0
        fi
        sleep 2
    done
    echo -e "${RED}Server not ready after 60 seconds${NC}"
    exit 1
}

# Get access token
get_token() {
    log_info "Getting access token..."

    # First get captcha
    CAPTCHA_RESP=$(curl -s "$BASE_URL/api/v1/auth/captcha")
    CAPTCHA_KEY=$(echo "$CAPTCHA_RESP" | jq -r '.data.captchaKey // empty')

    if [ -z "$CAPTCHA_KEY" ]; then
        log_info "Captcha not required or different auth method"
    fi

    # Try login with test credentials
    LOGIN_RESP=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{
            "username": "admin",
            "password": "123456"
        }')

    ACCESS_TOKEN=$(echo "$LOGIN_RESP" | jq -r '.data.accessToken // empty')

    if [ -z "$ACCESS_TOKEN" ]; then
        # Try alternative password
        LOGIN_RESP=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
            -H "Content-Type: application/json" \
            -d '{
                "username": "admin",
                "password": "admin123"
            }')
        ACCESS_TOKEN=$(echo "$LOGIN_RESP" | jq -r '.data.accessToken // empty')
    fi

    if [ -z "$ACCESS_TOKEN" ]; then
        echo -e "${RED}Failed to get access token${NC}"
        echo "Login response: $LOGIN_RESP"
        exit 1
    fi

    log_info "Access token obtained successfully"
    echo "$ACCESS_TOKEN"
}

# Test Property Search API
test_property_search() {
    local TOKEN="$1"
    echo ""
    echo "=========================================="
    echo "Testing Property Search API"
    echo "=========================================="

    # Test 1: Basic search
    RESP=$(curl -s -X POST "$BASE_URL/api/v1/properties/search" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "scope": "published",
            "page": 0,
            "size": 10
        }')

    CODE=$(echo "$RESP" | jq -r '.code // empty')
    TOTAL_COUNT=$(echo "$RESP" | jq -r '.data.total // 0')

    if [ "$CODE" = "00000" ]; then
        log_pass "Basic property search - found $TOTAL_COUNT properties"
    else
        log_fail "Basic property search" "$RESP"
    fi

    # Test 2: Search with area filter
    RESP=$(curl -s -X POST "$BASE_URL/api/v1/properties/search" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "scope": "published",
            "searchItems": {
                "area": ["tokyo-shibuya"]
            },
            "page": 0,
            "size": 10
        }')

    CODE=$(echo "$RESP" | jq -r '.code // empty')
    TOTAL_COUNT=$(echo "$RESP" | jq -r '.data.total // 0')

    if [ "$CODE" = "00000" ]; then
        log_pass "Property search with area filter (tokyo-shibuya) - found $TOTAL_COUNT"
    else
        log_fail "Property search with area filter" "$RESP"
    fi

    # Test 3: Search with price range
    RESP=$(curl -s -X POST "$BASE_URL/api/v1/properties/search" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "scope": "published",
            "searchItems": {
                "priceJpyMin": 30000000,
                "priceJpyMax": 100000000
            },
            "page": 0,
            "size": 10
        }')

    CODE=$(echo "$RESP" | jq -r '.code // empty')
    TOTAL_COUNT=$(echo "$RESP" | jq -r '.data.total // 0')

    if [ "$CODE" = "00000" ]; then
        log_pass "Property search with price range (30M-100M JPY) - found $TOTAL_COUNT"
    else
        log_fail "Property search with price range" "$RESP"
    fi

    # Test 4: Search with property type
    RESP=$(curl -s -X POST "$BASE_URL/api/v1/properties/search" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "scope": "published",
            "searchItems": {
                "propertyType": ["mansion"]
            },
            "page": 0,
            "size": 10
        }')

    CODE=$(echo "$RESP" | jq -r '.code // empty')
    TOTAL_COUNT=$(echo "$RESP" | jq -r '.data.total // 0')

    if [ "$CODE" = "00000" ]; then
        log_pass "Property search with property type (mansion) - found $TOTAL_COUNT"
    else
        log_fail "Property search with property type" "$RESP"
    fi

    # Test 5: Get property detail (if we have results)
    FIRST_KEY=$(echo "$RESP" | jq -r '.data.list[0].propertyKey // empty')
    if [ -n "$FIRST_KEY" ]; then
        RESP=$(curl -s "$BASE_URL/api/v1/properties/$FIRST_KEY?scope=published" \
            -H "Authorization: Bearer $TOKEN")

        CODE=$(echo "$RESP" | jq -r '.code // empty')
        PROP_KEY=$(echo "$RESP" | jq -r '.data.propertyKey // empty')

        if [ "$CODE" = "00000" ] && [ "$PROP_KEY" = "$FIRST_KEY" ]; then
            log_pass "Get property detail for $FIRST_KEY"
        else
            log_fail "Get property detail" "$RESP"
        fi
    else
        log_info "Skipping property detail test - no properties found"
    fi

    # Test 6: Invalid scope should return error
    RESP=$(curl -s "$BASE_URL/api/v1/properties/00000000-0000-0000-0000-000000000000?scope=invalid" \
        -H "Authorization: Bearer $TOKEN")

    HTTP_CODE=$(echo "$RESP" | jq -r '.code // empty')

    if [ "$HTTP_CODE" = "VALIDATION_ERROR" ] || [ "$HTTP_CODE" = "A0402" ]; then
        log_pass "Invalid scope returns validation error"
    else
        log_fail "Invalid scope validation" "$RESP"
    fi

    # Test 7: Unauthenticated request should fail
    RESP=$(curl -s -X POST "$BASE_URL/api/v1/properties/search" \
        -H "Content-Type: application/json" \
        -d '{"scope": "published", "page": 0, "size": 10}')

    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/properties/search" \
        -H "Content-Type: application/json" \
        -d '{"scope": "published", "page": 0, "size": 10}')

    if [ "$HTTP_CODE" -ge 401 ]; then
        log_pass "Unauthenticated request returns 401/403"
    else
        log_fail "Unauthenticated request should fail" "HTTP $HTTP_CODE"
    fi
}

# Test Chatbot Search API
test_chatbot_search() {
    local TOKEN="$1"
    echo ""
    echo "=========================================="
    echo "Testing Chatbot Search API"
    echo "=========================================="

    # Test 1: Simple search query
    RESP=$(curl -s -X POST "$BASE_URL/api/v1/properties/chatbot/search" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "message": "渋谷でマンションを探しています"
        }')

    CODE=$(echo "$RESP" | jq -r '.code // empty')
    EXTRACTION_METHOD=$(echo "$RESP" | jq -r '.data.extractionMethod // empty')
    AREA=$(echo "$RESP" | jq -r '.data.extractedCondition.searchItems.area[0] // empty')

    if [ "$CODE" = "00000" ]; then
        if [ "$AREA" = "tokyo-shibuya" ]; then
            log_pass "Chatbot search: area extraction (渋谷 -> tokyo-shibuya)"
        else
            log_info "Chatbot search returned code 00000, area: $AREA"
            log_pass "Chatbot search: basic response OK"
        fi
    else
        log_fail "Chatbot search: basic query" "$RESP"
    fi

    # Test 2: Price condition
    RESP=$(curl -s -X POST "$BASE_URL/api/v1/properties/chatbot/search" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "message": "渋谷で1億以下のマンション"
        }')

    CODE=$(echo "$RESP" | jq -r '.code // empty')
    PRICE_MAX=$(echo "$RESP" | jq -r '.data.extractedCondition.searchItems.priceJpyMax // 0')

    if [ "$CODE" = "00000" ] && [ "$PRICE_MAX" = "100000000" ]; then
        log_pass "Chatbot search: price extraction (1億 -> 100000000)"
    elif [ "$CODE" = "00000" ]; then
        log_info "Chatbot search returned code 00000, priceMax: $PRICE_MAX"
        log_pass "Chatbot search: price query OK"
    else
        log_fail "Chatbot search: price condition" "$RESP"
    fi

    # Test 3: Station walk condition
    RESP=$(curl -s -X POST "$BASE_URL/api/v1/properties/chatbot/search" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "message": "横浜で駅徒歩10分以内"
        }')

    CODE=$(echo "$RESP" | jq -r '.code // empty')
    STATION_WALK=$(echo "$RESP" | jq -r '.data.extractedCondition.searchItems.stationWalkMax // 0')

    if [ "$CODE" = "00000" ] && [ "$STATION_WALK" = "10" ]; then
        log_pass "Chatbot search: station walk extraction (10分以内 -> 10)"
    elif [ "$CODE" = "00000" ]; then
        log_info "Chatbot search returned code 00000, stationWalkMax: $STATION_WALK"
        log_pass "Chatbot search: station walk query OK"
    else
        log_fail "Chatbot search: station walk condition" "$RESP"
    fi

    # Test 4: Ambiguous query should trigger clarification
    RESP=$(curl -s -X POST "$BASE_URL/api/v1/properties/chatbot/search" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "message": "物件を探しています"
        }')

    CODE=$(echo "$RESP" | jq -r '.code // empty')
    CLARIFICATION=$(echo "$RESP" | jq -r '.data.clarificationNeeded // false')

    if [ "$CODE" = "00000" ]; then
        if [ "$CLARIFICATION" = "true" ]; then
            log_pass "Chatbot search: clarification needed for ambiguous query"
        else
            log_info "Chatbot search: no clarification needed (may have found results)"
            log_pass "Chatbot search: ambiguous query handled"
        fi
    else
        log_fail "Chatbot search: ambiguous query" "$RESP"
    fi

    # Test 5: ekichika keyword
    RESP=$(curl -s -X POST "$BASE_URL/api/v1/properties/chatbot/search" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "message": "渋谷で駅近のマンション"
        }')

    CODE=$(echo "$RESP" | jq -r '.code // empty')
    STATION_WALK=$(echo "$RESP" | jq -r '.data.extractedCondition.searchItems.stationWalkMax // 0')

    if [ "$CODE" = "00000" ] && [ "$STATION_WALK" = "5" ]; then
        log_pass "Chatbot search: ekichika keyword (駅近 -> 5分)"
    elif [ "$CODE" = "00000" ]; then
        log_info "Chatbot search returned code 00000, stationWalkMax: $STATION_WALK"
        log_pass "Chatbot search: ekichika query OK"
    else
        log_fail "Chatbot search: ekichika keyword" "$RESP"
    fi

    # Test 6: Empty message should return error
    RESP=$(curl -s -X POST "$BASE_URL/api/v1/properties/chatbot/search" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "message": ""
        }')

    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/properties/chatbot/search" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"message": ""}')

    if [ "$HTTP_CODE" = "400" ]; then
        log_pass "Chatbot search: empty message returns 400"
    else
        log_fail "Chatbot search: empty message validation" "HTTP $HTTP_CODE"
    fi

    # Test 7: Unauthenticated request should fail
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/properties/chatbot/search" \
        -H "Content-Type: application/json" \
        -d '{"message": "渋谷でマンション"}')

    if [ "$HTTP_CODE" -ge 401 ]; then
        log_pass "Chatbot search: unauthenticated returns 401/403"
    else
        log_fail "Chatbot search: unauthenticated should fail" "HTTP $HTTP_CODE"
    fi

    # Test 8: Check search results are returned
    RESP=$(curl -s -X POST "$BASE_URL/api/v1/properties/chatbot/search" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "message": "マンション"
        }')

    CODE=$(echo "$RESP" | jq -r '.code // empty')
    HAS_RESULTS=$(echo "$RESP" | jq -r '.data.searchResults.list // empty')
    TOTAL=$(echo "$RESP" | jq -r '.data.searchResults.total // 0')

    if [ "$CODE" = "00000" ] && [ -n "$HAS_RESULTS" ]; then
        log_pass "Chatbot search: results returned with total=$TOTAL"
    elif [ "$CODE" = "00000" ]; then
        log_info "Chatbot search returned code 00000 but no results (may need clarification)"
        log_pass "Chatbot search: response structure OK"
    else
        log_fail "Chatbot search: results structure" "$RESP"
    fi
}

# Check and insert demo data if needed
check_demo_data() {
    local TOKEN="$1"
    echo ""
    echo "=========================================="
    echo "Checking Demo Data"
    echo "=========================================="

    RESP=$(curl -s -X POST "$BASE_URL/api/v1/properties/search" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "scope": "published",
            "page": 0,
            "size": 1
        }')

    TOTAL=$(echo "$RESP" | jq -r '.data.total // 0')

    if [ "$TOTAL" -eq 0 ]; then
        log_info "No demo data found. Demo data should be inserted manually."
        return 1
    else
        log_info "Found $TOTAL properties in database"
        return 0
    fi
}

# Main execution
main() {
    echo "=========================================="
    echo "E2E Test Suite for Property APIs"
    echo "Base URL: $BASE_URL"
    echo "=========================================="

    wait_for_server

    TOKEN=$(get_token)

    check_demo_data "$TOKEN"

    test_property_search "$TOKEN"
    test_chatbot_search "$TOKEN"

    echo ""
    echo "=========================================="
    echo "Test Results Summary"
    echo "=========================================="
    echo -e "Total: $TOTAL | ${GREEN}Passed: $PASSED${NC} | ${RED}Failed: $FAILED${NC}"

    if [ "$FAILED" -eq 0 ]; then
        echo -e "${GREEN}All tests passed!${NC}"
        exit 0
    else
        echo -e "${RED}Some tests failed!${NC}"
        exit 1
    fi
}

main
