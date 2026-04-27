#!/bin/bash
# =============================================================================
#  SKS College of Nursing — User Guide PDF Generator
#  Usage:  ./generate-pdfs.sh
#  Requires: pandoc, google-chrome (or chromium) installed
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo ""
echo "╔══════════════════════════════════════════════════════╗"
echo "║   SKS College of Nursing — PDF Guide Generator      ║"
echo "╚══════════════════════════════════════════════════════╝"
echo ""

# Detect browser
CHROME=""
for browser in google-chrome chromium-browser chromium; do
  if command -v "$browser" &>/dev/null; then
    CHROME="$browser"
    break
  fi
done

if [ -z "$CHROME" ]; then
  echo "❌ ERROR: Chrome/Chromium not found. Install with:"
  echo "   sudo apt-get install google-chrome-stable"
  echo "   or: sudo apt-get install chromium-browser"
  exit 1
fi

if ! command -v pandoc &>/dev/null; then
  echo "❌ ERROR: pandoc not found. Install with:"
  echo "   sudo apt-get install pandoc"
  exit 1
fi

echo "📌 Using browser: $CHROME"
echo "📌 Using pandoc:  $(pandoc --version | head -1)"
echo ""

mkdir -p pdf

GUIDES=(
  "COLLEGE_ADMIN_USER_GUIDE"
  "FRONT_OFFICE_USER_GUIDE"
  "CASHIER_USER_GUIDE"
  "USER_GUIDE_INDEX"
)

TITLES=(
  "College Admin Guide"
  "Front Office User Guide"
  "Cashier User Guide"
  "User Guide Index"
)

# Step 1: Generate HTML from Markdown
echo "Step 1: Generating HTML files from Markdown..."
echo "──────────────────────────────────────────────"
for guide in "${GUIDES[@]}"; do
  if [ ! -f "${guide}.md" ]; then
    echo "  ⚠️  Skipping ${guide}.md — file not found"
    continue
  fi
  pandoc "${guide}.md" \
    -o "${guide}.html" \
    --standalone \
    --css="assets/guide-style.css" \
    --metadata title="SKS College of Nursing" \
    --toc \
    --toc-depth=3 \
    --highlight-style=tango \
    2>/dev/null
  echo "  ✅ ${guide}.html"
done

echo ""

# Step 2: Convert HTML to PDF using Chrome
echo "Step 2: Converting HTML to PDF..."
echo "──────────────────────────────────────────────"
for i in "${!GUIDES[@]}"; do
  guide="${GUIDES[$i]}"
  if [ ! -f "${guide}.html" ]; then
    echo "  ⚠️  Skipping ${guide} — HTML not found"
    continue
  fi
  "$CHROME" \
    --headless \
    --disable-gpu \
    --no-sandbox \
    --disable-dev-shm-usage \
    --print-to-pdf="pdf/${guide}.pdf" \
    --print-to-pdf-no-header \
    "file://$(pwd)/${guide}.html" \
    2>/dev/null

  if [ -f "pdf/${guide}.pdf" ]; then
    SIZE=$(du -h "pdf/${guide}.pdf" | cut -f1)
    echo "  ✅ pdf/${guide}.pdf (${SIZE})"
  else
    echo "  ❌ Failed to generate pdf/${guide}.pdf"
  fi
done

echo ""
echo "╔══════════════════════════════════════════════════════╗"
echo "║  ✅ PDF Generation Complete!                         ║"
echo "╚══════════════════════════════════════════════════════╝"
echo ""
echo "Output files:"
ls -lh pdf/*.pdf 2>/dev/null || echo "No PDFs generated"
echo ""
echo "To open the PDFs:"
echo "  xdg-open pdf/COLLEGE_ADMIN_USER_GUIDE.pdf"
echo "  xdg-open pdf/FRONT_OFFICE_USER_GUIDE.pdf"
echo "  xdg-open pdf/CASHIER_USER_GUIDE.pdf"
echo "  xdg-open pdf/USER_GUIDE_INDEX.pdf"
echo ""

