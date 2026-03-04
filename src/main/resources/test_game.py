import requests
import json
import os
from pathlib import Path

# --- CONFIGURATION ---
BASE_URL = "http://localhost:8080/api"

# find the project root
CURRENT_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = CURRENT_DIR
while not (PROJECT_ROOT / "src").exists() and PROJECT_ROOT.parent != PROJECT_ROOT:
    PROJECT_ROOT = PROJECT_ROOT.parent

BOOKS_DIR = PROJECT_ROOT / "src" / "main" / "resources" / "books"
TEST_BOOK_FILE = BOOKS_DIR / "crystal-caverns.json"

def get_valid_moves(book_json, current_section_id):
    """list choices available in the JSON."""
    sections = book_json.get('sections', [])
    current = next((s for s in sections if s['id'] == current_section_id), None)
    if not current or 'options' not in current:
        return []
    return [opt['gotoId'] for opt in current['options']]

def test_adventure_engine():
    print(f"🚀 Starting Crystal Caverns E2E Test | Root: {PROJECT_ROOT.name}")
    print("=" * 60)

    # 1. LOAD AND UPLOAD
    if not TEST_BOOK_FILE.exists():
        print(f"❌ ERROR: Cannot find {TEST_BOOK_FILE}")
        return

    with open(TEST_BOOK_FILE, 'r', encoding='utf-8') as f:
        book_data = json.load(f)

    print(f"\n[STEP 1] Uploading: {book_data['title']}")
    try:
        with open(TEST_BOOK_FILE, 'rb') as f:
            files = {'file': (TEST_BOOK_FILE.name, f, 'application/json')}
            response = requests.post(f"{BASE_URL}/books/upload", files=files)

        response.raise_for_status()
        book_uuid = response.json()['id']
        print(f"✅ Uploaded! ID: {book_uuid}")
    except Exception as e:
        print(f"💥 Upload Error: {e}")
        return

    # 2. START SESSION
    print("\n[STEP 2] Starting Session...")
    state = requests.post(f"{BASE_URL}/play/{book_uuid}/start").json()
    session_id = state['sessionId']
    curr_id = state['currentSectionId']
    print(f"✅ Session: {session_id} | HP: {state['hp']} | Section: {curr_id}")

    # 3. TEST LOSE_HEALTH (The Path: 1 -> 20 -> 200)
    print("\n[STEP 3] Testing LOSE_HEALTH (-4 Damage)...")

    # Check if 20 is a valid move from 1
    if 20 in get_valid_moves(book_data, curr_id):
        requests.post(f"{BASE_URL}/play/session/{session_id}/choose", json=20)

        # Check if 200 is valid from 20
        if 200 in get_valid_moves(book_data, 20):
            resp = requests.post(f"{BASE_URL}/play/session/{session_id}/choose", json=200)
            state = resp.json()

            if 'hp' in state:
                print(f"✅ Moved to 200. HP is now: {state['hp']}")
                if state['hp'] == 6:
                    print("📊 HP Correct: 10 - 4 = 6")
                else:
                    print(f"⚠️ HP Unexpected: Got {state['hp']}, Expected 6")
            else:
                print(f"❌ Server Error at Section 200: {state}")
    else:
        print("❌ Choice 20 not found in Section 1 options!")

    # 4. TEST GAIN_HEALTH (New Session: 1 -> 100 -> 300 -> 700)
    print("\n[STEP 4] Testing GAIN_HEALTH (+3 Healing)...")

    # Start fresh to ensure we are back at Section 1
    new_state = requests.post(f"{BASE_URL}/play/{book_uuid}/start").json()
    sid = new_state['sessionId']

    path = [100, 300, 700]
    for target in path:
        resp = requests.post(f"{BASE_URL}/play/session/{sid}/choose", json=target)
        state = resp.json()

        if resp.status_code != 200:
            print(f"❌ Movement Failed to {target}: {state}")
            break

        if target == 700:
            print(f"✅ HP after resting: {state['hp']}")
            if state['hp'] > 6: # Should be 10 if capped, or 13 if uncapped
                print(f"📊 Healing Success! Log: {state['log'][-1]}")

    # 5. TEST PERSISTENCE
    print("\n[STEP 5] Testing Session Persistence...")
    persist_resp = requests.get(f"{BASE_URL}/play/session/{sid}")
    if persist_resp.status_code == 200:
        p_state = persist_resp.json()
        if p_state['currentSectionId'] == 700:
            print("✅ Persistence Success: Resumed at Section 700.")
    else:
        print("❌ Persistence Check Failed.")

    print("\n" + "=" * 60)
    print("🏁 TEST SUITE COMPLETE")

if __name__ == "__main__":
    test_adventure_engine()