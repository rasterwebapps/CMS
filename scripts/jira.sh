#!/usr/bin/env bash
# JIRA CLI helper for OneCMS / OC project
#
# Full workflow:
#   Open → In Progress → In Review → In QA → Resolved
#   ←—— Claude/dev ——→               ←—— QA/release team ——→
#
# Commands:
#   create  "Title" ["Desc"] ["label"]  — create Task (label defaults to: onecms)
#   start   OC-123                      — Open → In Progress
#   review  OC-123 ["summary comment"]  — In Progress → In Review
#   qa      OC-123                      — In Review → In QA  (you, after deploying to test)
#   reopen  OC-123 ["reason"]           — In QA → Reopened   (QA found bugs)
#   resolve OC-123                      — In QA → Resolved   (after live release)
#   comment OC-123 "text"               — add a comment at any stage
#   info    OC-123                      — show summary, status, labels

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/../.jira.env"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERROR: .jira.env not found at $ENV_FILE" >&2
  exit 1
fi

# shellcheck source=../.jira.env
source "$ENV_FILE"

AUTH="$JIRA_USER:$JIRA_PASS"
API="$JIRA_BASE_URL/rest/api/2"

# Transition IDs (mapped from the OC project workflow)
#   11  Open        → In Progress
#   21  In Progress → In Review
#   31  In Review   → In QA
#   41  In QA       → Resolved
#   91  In QA       → Reopened
_do_transition() {
  local key="$1" tid="$2"
  curl -s -u "$AUTH" -X POST \
    -H "Content-Type: application/json" \
    -d "{\"transition\":{\"id\":\"$tid\"}}" \
    "$API/issue/$key/transitions"
}

_status() {
  curl -s -u "$AUTH" "$API/issue/$1" \
    | python3 -c "import json,sys; print(json.load(sys.stdin)['fields']['status']['name'])"
}

_add_comment() {
  local key="$1" text="$2"
  payload=$(python3 -c "import json,sys; print(json.dumps({'body': sys.argv[1]}))" "$text")
  curl -s -u "$AUTH" -X POST \
    -H "Content-Type: application/json" \
    -d "$payload" \
    "$API/issue/$key/comment" > /dev/null
}

cmd="${1:-help}"

case "$cmd" in

  create)
    title="${2:?Usage: jira.sh create \"Title\" [\"Description\"] [\"label\"]}"
    desc="${3:-}"
    label="${4:-onecms}"
    payload=$(python3 -c "
import json, sys
print(json.dumps({
  'fields': {
    'project':     {'key': '$JIRA_PROJECT'},
    'summary':     sys.argv[1],
    'description': sys.argv[2] if sys.argv[2] else sys.argv[1],
    'issuetype':   {'name': 'Task'},
    'labels':      [sys.argv[3]]
  }
}))
" "$title" "$desc" "$label")
    result=$(curl -s -u "$AUTH" -X POST \
      -H "Content-Type: application/json" \
      -d "$payload" \
      "$API/issue")
    key=$(echo "$result" | python3 -c "
import json,sys
d=json.load(sys.stdin)
print(d.get('key', 'ERROR: ' + str(d)))
")
    echo "$key"
    echo "View: $JIRA_BASE_URL/browse/$key" >&2
    ;;

  start)
    key="${2:?Usage: jira.sh start OC-123}"
    status=$(_status "$key")
    if [[ "$status" == "In Progress" ]]; then
      echo "$key is already In Progress."
    else
      _do_transition "$key" 11 > /dev/null
      echo "$key → In Progress"
    fi
    ;;

  review)
    key="${2:?Usage: jira.sh review OC-123 [\"summary\"]}"
    summary="${3:-}"
    _do_transition "$key" 21 > /dev/null
    echo "$key → In Review"
    if [[ -n "$summary" ]]; then
      _add_comment "$key" "$summary"
      echo "Summary comment added."
    fi
    ;;

  qa)
    key="${2:?Usage: jira.sh qa OC-123}"
    _do_transition "$key" 31 > /dev/null
    echo "$key → In QA"
    ;;

  reopen)
    key="${2:?Usage: jira.sh reopen OC-123 [\"reason\"]}"
    reason="${3:-}"
    _do_transition "$key" 91 > /dev/null
    echo "$key → Reopened"
    if [[ -n "$reason" ]]; then
      _add_comment "$key" "Reopened: $reason"
      echo "Reason comment added."
    fi
    ;;

  resolve)
    key="${2:?Usage: jira.sh resolve OC-123}"
    _do_transition "$key" 41 > /dev/null
    echo "$key → Resolved"
    ;;

  comment)
    key="${2:?Usage: jira.sh comment OC-123 \"text\"}"
    text="${3:?Missing comment text}"
    _add_comment "$key" "$text"
    echo "Comment added to $key."
    ;;

  info)
    key="${2:?Usage: jira.sh info OC-123}"
    curl -s -u "$AUTH" "$API/issue/$key" | python3 -c "
import json, sys
d = json.load(sys.stdin)['fields']
print('Summary  :', d['summary'])
print('Status   :', d['status']['name'])
print('Type     :', d['issuetype']['name'])
print('Created  :', d['created'][:10])
print('Labels   :', ', '.join(d.get('labels', [])))
"
    ;;

  help|*)
    cat <<'HELP'
JIRA CLI — OneCMS / OC project

  jira.sh create  "Title" ["Desc"] ["label"]   create Task  (label: onecms)
  jira.sh start   OC-123                        Open → In Progress
  jira.sh review  OC-123 ["summary"]            In Progress → In Review
  jira.sh qa      OC-123                        In Review → In QA
  jira.sh reopen  OC-123 ["reason"]             In QA → Reopened
  jira.sh resolve OC-123                        In QA → Resolved

  jira.sh comment OC-123 "text"                 add a comment at any stage
  jira.sh info    OC-123                        show summary + status

Workflow:
  Open → In Progress → In Review → In QA → Resolved
  ←—— Claude/dev ——→               ←—— QA/release ——→

Commit prefix convention:
  OC-123: feat(module): what changed
HELP
    ;;
esac
